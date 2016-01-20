import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.instance.Message;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class CatFaxMain {

    private static final String ACCOUNT_SID             = "AC3fd9f1b394e4fbcff3966c17c131ef97";
    private static final String AUTH_TOKEN              = "5f187afdea5b5b94aaa64d421fb486f7";
    private static final String CATFAX_PHONE            = "+18187228329";
    private static final String FACT_TIME               = "15:15"; // 3:15 PM
    private static final String KILLSWITCH_CONFIRM      = "Killswitch Activated";
    private static final String KILLSWITCH_MSG          = "KILLALL";
    private static final String KILLSWITCH_NUM1         = "+13603931867";
    private static final String KILLSWITCH_NUM2         = "+13603256564";
    private static final String SUBSCRIBERS_SERIAL      = "subscribers.serial";
    private static final String SUBSCRIBERS_TEST_SERIAL = "subscribers_test.serial";

    private static List<Subscriber> subscribers;
    private static List<String>     catFacts;

    private static boolean messagesSent;
    private static boolean reconnect;
    private static int     index;
    private static boolean testing;

    private static Logger log = Logger.getLogger("CatFax");

    private static TwilioRestClient    client;
    private static MessageSender       sender;
    private static Map<String, String> filters;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        testing = args[0].equals("testing");

        // Setup logger
        FileHandler fh = new FileHandler("catfax.log");
        // Enable logging to sout if in testing mode
        if(testing) {
            log.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
        }
        fh.setFormatter(new SimpleFormatter());
        log.addHandler(fh);

        client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
        sender = new MessageSender(client, CATFAX_PHONE);

        filters = new HashMap<>();

        // Will throw exception if file not found
        catFacts = getFacts();
        subscribers = getSubscribers();
        index = getIndex();

        filters.put("To", CATFAX_PHONE);

        final ScheduledExecutorService executorService
                = Executors.newSingleThreadScheduledExecutor();

        Future<?> future = executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    eventLoop();
                } catch(TwilioRestException e) {
                    e.printStackTrace();
                    log.log(Level.WARNING, "There was an error with Twilio. Not exiting.", e);
                } catch(IOException e) {
                    // Cause the program to exit on an IOException
                    log.log(Level.SEVERE, e.getMessage(), e);
                    throw new RuntimeException();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        if(testing) {
            log.info("In testing mode. Commands: send, index, subs, exit");
            Scanner scan = new Scanner(System.in);
            boolean exit = false;
            while(!exit) {
                if(scan.hasNext()) {
                    String input = scan.next();
                    try {
                        switch(input) {
                            case "send":
                                sendInstantFacts(subscribers);
                                System.out.println("Texts sent");
                                break;
                            case "index":
                                System.out.println("Current index: " + index);
                                break;
                            case "subs":
                                System.out.println(subscribers);
                                break;
                            case "exit":
                                exit = true;
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // This will cause any RuntimeExceptions propagated through the stack to cause the
        // program to exit. This may be desirable for some exceptions. To enable this, add the
        // exception type to the try-catch block above.
        try {
            future.get();
        } catch(Exception e) {
            executorService.shutdownNow();
            System.exit(0);
        }
    }

    /**
     * Get the index stored in currentIndex.txt.
     *
     * @return The current index.
     * @throws FileNotFoundException if "currentIndex.txt" not found.
     */
    private static int getIndex() throws FileNotFoundException {
        try {
            int index;

            try(Scanner indexFile = new Scanner(new File("currentIndex.txt"))) {
                index = indexFile.nextInt();
            }

            return index;
        } catch(FileNotFoundException e) {
            log.severe("Error reading currentIndex.txt. Exiting.");
            throw e;
        }
    }

    /**
     * Gets a list of subscribers from the file.
     *
     * @return An ArrayList of subscribers
     * @throws ClassNotFoundException if the class Subscriber is not found.
     * @throws FileNotFoundException if "subscribers.serial" not found.
     */
    @SuppressWarnings("unchecked")
    private static List<Subscriber> getSubscribers() throws IOException, ClassNotFoundException {
        List<Subscriber> subs;

        try(FileInputStream file = new FileInputStream(new File(getSubscriberFile()));
            ObjectInputStream ois = new ObjectInputStream(file)) {

            subs = (ArrayList<Subscriber>) ois.readObject();

        } catch(IOException e) {
            log.severe("Error reading subscribers. Exiting. " + e);
            throw e;
        }

        return subs;
    }

    /**
     * The main loop of the program.
     *
     * @throws TwilioRestException Something bad happens w/ Twilio.
     * @throws IOException         Error writing to files.
     */
    private static void eventLoop() throws TwilioRestException, IOException {
        String time = getTime();
        reconnect(time);

        List<Subscriber> needsFact = parseInbox(getInbox());
        sendInstantFacts(needsFact);

        if (time.equals(FACT_TIME) && !messagesSent) {
            sendGroupFact(sender);
            messagesSent = true;
        } else if(!time.equals(FACT_TIME)) {
            messagesSent = false;
        }
    }

    /**
     * @return 24-hour time in the form of 17:32.
     */
    private static String getTime() {
        return new SimpleDateFormat("HHmm").format(new Date());
    }

    /**
     * Reconnect to the Twilio API every 30 minutes. Idk why.
     *
     * @param time The current time.
     */
    private static void reconnect(String time) {
        int minute = Integer.parseInt(time.substring(time.indexOf(":") + 1));

        if(minute % 30 == 0 && !reconnect) {
            client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
            sender = new MessageSender(client, CATFAX_PHONE);
            reconnect = true;
        } else if(minute % 30 != 0) {
            reconnect = false;
        }
    }

    /**
     * Gets message Sids sent to CatFax.
     *
     * @return A List of new message Sids sent to CatFax today.
     * @throws IOException if handledMessages.txt unwritable.
     */
    private static List<String> getInbox() throws IOException {
        try {
            filters.put("DateSent", getDate());

            MessageReader reader = new MessageReader(client, filters);

            return reader.checkMessages();
        } catch(IOException e) {
            log.severe("Error reading handlesMessages.txt. Exiting.\n" + e);
            throw e;
        }
    }

    /**
     * Get the current date, one day in the future for some reason?
     *
     * @return Date + 1 day.
     */
    private static String getDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);

        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    /**
     * Parse the inbox. Kill the program if it receives the killswitch, subscribe people who
     * text it, and add people who text it to the needsFact list.
     *
     * @param inbox List of Sids to parse
     * @return A list of phone numbers that need cat facts sent to.
     * @throws TwilioRestException if something goes wrong.
     * @throws IOException if log file not writable.
     */
    private static List<Subscriber> parseInbox(List<String> inbox) throws IOException,
                                                                          TwilioRestException {
        List<Subscriber> needsFact = new ArrayList<>();

        for(String newMessageSid : inbox) {
            Message message = client.getAccount().getMessage(newMessageSid);

            Subscriber from = new Subscriber(message.getFrom());

            if(isKillswitch(from.getPhoneNumber(),
                            message.getBody())) { // Check if message is a killswitch
                System.out.println(KILLSWITCH_CONFIRM);

                // Confirm that killswitch did work
                sender.setPhoneNumber(from.getPhoneNumber());
                sender.sendMessage(KILLSWITCH_CONFIRM);

                System.exit(0);
            } else {
                boolean newSubs = false;

                if(!subscribers.contains(from)) {   // Check if number is new
                    newSubs = true;
                    from.setDisplacement(index);
                    subscribers.add(from);
                    log.info("Added subscriber: " + from.getPhoneNumber());
                }

                needsFact.add(subscribers.get(subscribers.indexOf(from)));

                if(newSubs) {
                    saveSubscribers();
                }
            }
        }

        return needsFact;
    }

    /**
     * Determines if a text was the killswitch.
     *
     * @param number    The number the text was sent from.
     * @param message   The content of the text.
     * @return whether of not it was a valid killswitch.
     */
    private static boolean isKillswitch(String number, String message) {
        return ((number.equals(KILLSWITCH_NUM1) || number.equals(KILLSWITCH_NUM2))
                && message.equals(KILLSWITCH_MSG));
    }

    /**
     * Saves the list of subscribers to subscribers.serial.
     *
     * @throws IOException if the log file is not writable.
     */
    private static void saveSubscribers() throws IOException {
        try(FileOutputStream file = new FileOutputStream(new File(getSubscriberFile()));
            ObjectOutputStream oos = new ObjectOutputStream(file)) {

            oos.writeObject(subscribers);
        } catch(IOException e) {
            // Don't want this error to end the program
            log.severe("Error saving subscribers.\n" + e);
            log.severe("I cannot save subscribers so I will output the file here just in case");
            log.severe("--------BEGIN '" + getSubscriberFile() + "' DUMP--------\n");
            try {
                FileOutputStream file = new FileOutputStream(new File("catfax.log"), true);
                ObjectOutputStream ois = new ObjectOutputStream(file);
            } catch(IOException ex) {
                log.severe("Error writing to log file, so we're fucked anyway.\n" + e);
                throw ex;
            }
            log.severe("--------END '\" + getSubscriberFile() + \"' DUMP--------\n");
        }
    }

    /**
     * Send facts!
     *
     * @param needsFact The list of subscribers who need facts
     * @throws TwilioRestException if something happens
     * @throws IOException if log file not writable.
     */
    private static void sendInstantFacts(List<Subscriber> needsFact) throws
                                                                     IOException,
                                                                     TwilioRestException {
        for(Subscriber sub : needsFact) {
            String phoneNumber = sub.getPhoneNumber();

            sender.setPhoneNumber(phoneNumber);
            int displacement = sub.getDisplacement();
            // Update displacement
            sub.setDisplacement(displacement + 1);

            // Retrieve catFact
            String fact = catFacts.get((index - displacement) % catFacts.size());
            String sid = sender.sendMessage(fact);
            log.info("Instant fact sent to: " + sub.getPhoneNumber() + "\t " + sid);
        }

        saveSubscribers();
    }

    /**
     * Retrieve cat facts.
     *
     * @return ArrayList of facts.
     * @throws FileNotFoundException catFax.txt unreadable.
     */
    private static ArrayList<String> getFacts() throws FileNotFoundException {
        try {
            Scanner catFaxFile = new Scanner(new File("catFax.txt"));

            ArrayList<String> catFacts = new ArrayList<>();

            while(catFaxFile.hasNextLine()) {
                catFacts.add(catFaxFile.nextLine());
            }

            return catFacts;
        } catch(FileNotFoundException e) {
            log.severe("Error accessing catFax.txt. Exiting.\n" + e);
            throw e;
        }
    }

    /**
     * Send facts!
     *
     * @param sender    A MessageSender to send texts from.
     * @throws TwilioRestException if something goes wrong.
     */
    private static void sendGroupFact(MessageSender sender) throws TwilioRestException {
        // Determine which cat fact to send based on the subscriber's displacement from current
        // fact
        for(Subscriber sub : subscribers) {
            String number = sub.getPhoneNumber();
            int displacement = sub.getDisplacement();

            // Retrieve catFact
            int thisIndex = index - displacement;
            String fact = catFacts.get(thisIndex);

            sender.setPhoneNumber(number);
            sender.sendMessage(fact);
        }

        log.info("Group fact sent to " + subscribers.size() + " people! New index: " + index);

        index++;

        try {
            PrintStream indexWriter = new PrintStream(new File("currentIndex.txt"));
            indexWriter.print(index);
            indexWriter.close();
        } catch(FileNotFoundException e) {
            log.severe("Error writing to currentIndex.txt. Will not exit. New index: " +
                       index + "\n" + e);
        }
    }

    private static String getSubscriberFile() {
        return testing ? SUBSCRIBERS_SERIAL : SUBSCRIBERS_TEST_SERIAL;
    }
}
