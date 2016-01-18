import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.instance.Message;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class TwilioTest {

    public static final String ACCOUNT_SID = "AC3fd9f1b394e4fbcff3966c17c131ef97";
    public static final String AUTH_TOKEN = "5f187afdea5b5b94aaa64d421fb486f7";
    public static final String CATFAX_PHONE = "+18187228329";

    public static List<String> displacements;
    public static boolean messagesSent;
    public static boolean reconnect;
    public static int index;
    public static final String FACT_TIME = "15:15"; // 3:15 PM
    public static final String KILLSWITCH_CONFIRM = "Killswitch Activated";

    public static TwilioRestClient client;
    public static MessageSender sender;
    public static Map<String, String> filters;

    public static void main(String[] args) throws FileNotFoundException {
        //test
        client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
        sender = new MessageSender(client, CATFAX_PHONE);
        displacements = new ArrayList<>();
        filters = new HashMap<>();

        final ArrayList<String> catFacts = getFacts();

        getDisplacements();
        getIndex();
        filters.put("To", CATFAX_PHONE);

        final ScheduledExecutorService executorService
                = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    eventLoop(catFacts);
                } catch (TwilioRestException|IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Get the index stored in currentIndex.txt.
     *
     * @throws FileNotFoundException if file not found.
     */
    public static void getIndex() throws FileNotFoundException {
        Scanner indexFile = new Scanner(new File("currentIndex.txt"));

        index = indexFile.nextInt();

        indexFile.close();
    }

    /**
     * Get displacement values from numbersDisplacement.txt and adds them to the displacements
     * field.
     *
     * @throws FileNotFoundException if file not found.
     */
    public static void getDisplacements() throws FileNotFoundException {
        Scanner numDispFile = new Scanner(new File("numbersDisplacement.txt"));

        while (numDispFile.hasNextLine()) {
            displacements.add(numDispFile.nextLine());
        }

        numDispFile.close();
    }

    public static void eventLoop(ArrayList<String> catFacts) throws TwilioRestException, IOException {
        String time = getTime();
        reconnect(time);

        List<String> needsFact = parseInbox(getInbox());
        sendInstantFacts(needsFact, catFacts);

        if (time.equals(FACT_TIME) && !messagesSent) {
            sendGroupFact(sender, catFacts);
            messagesSent = true;
        } else if(!time.equals(FACT_TIME)) {
            messagesSent = false;
        }
    }

    /**
     * @return 24-hour time in the form of 17:32.
     */
    public static String getTime() {
        return new SimpleDateFormat("HHmm").format(new Date());
    }

    /**
     * Reconnect to the Twilio API every 30 minutes. Idk why.
     *
     * @param time The current time.
     */
    public static void reconnect(String time) {
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
    public static List<String> getInbox() throws IOException {
        filters.put("DateSent", getDate());

        MessageReader reader = new MessageReader(client, filters);

        return reader.checkMessages();
    }

    /**
     * Get the current date, one day in the future for some reason?
     *
     * @return Date + 1 day.
     */
    public static String getDate() {
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
     * @throws IOException         if handledMessages.txt unwritable.
     */
    public static List<String> parseInbox(List<String> inbox) throws TwilioRestException, IOException {
        List<String> needsFact = new ArrayList<>();

        for(String newMessageSid : inbox) {
            Message message = client.getAccount().getMessage(newMessageSid);

            String from = message.getFrom();

            if (isKillswitch(from, message.getBody())) { // Check if message is a killswitch
                System.out.println(KILLSWITCH_CONFIRM);

                // Confirm that killswitch did work
                sender.setPhoneNumber(from);
                sender.sendMessage(KILLSWITCH_CONFIRM);

                System.exit(0);
            } else {
                if (!isSubscriber(from)) {   // Check if number is new
                    addSubscriber(from); // Add to subscriber list
                }

                needsFact.add(from);
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
    public static boolean isKillswitch(String number, String message) {
        return ((number.equals("+13603931867") || number.equals("+13603256564"))
                && message.equals("KILLALL"));
    }

    /**
     * Check if someone is subscribed.
     *
     * @param number The number to check.
     * @return Whether the person is subscribed or not
     */
    //
    public static boolean isSubscriber(String number) {
        return displacements.contains(number);
    }

    /**
     * Add a new subscriber to CatFax.
     *
     * @param number    The number to add
     * @throws IOException if numbersDisplacement.txt us unwritable.
     */
    public static void addSubscriber(String number) throws IOException {
        // Append numbersDisplacement.txt with the new subscriber's displacement
        FileOutputStream fileOutStream = new FileOutputStream("numbersDisplacement.txt", true);
        PrintStream filePrinter = new PrintStream(fileOutStream);
        filePrinter.println(number);
        filePrinter.println(index); // Current index is new subscriber's displacement
        fileOutStream.close();

        // Update the ArrayList
        displacements.add(number);
        displacements.add("" + index);
        System.out.println("Subscriber Added: " + number);
    }

    /**
     * Send facts!
     *
     * @param needsFact The list of numbers who need facts
     * @param catFacts  A list of cat facts
     * @throws TwilioRestException if something happens
     * @throws FileNotFoundException if handledMessages.txt unwritable.
     */
    public static void sendInstantFacts(List<String> needsFact, ArrayList<String> catFacts)
            throws TwilioRestException, FileNotFoundException {
        for (String phoneNumber : needsFact) {
            sender.setPhoneNumber(phoneNumber);

            int displacement = 0; //need to instantiate
            for(int i = 0; i < displacements.size(); i += 2){
                if(displacements.get(i).equals(phoneNumber)) {
                    // Get the subscriber's displacement
                    displacement = Integer.parseInt(displacements.get(i + 1));
                    // update their displacement in ArrayList to be written in file
                    displacements.set((i + 1), "" + (displacement - 1));
                    break;
                }
            }

            // Retrieve catFact
            String fact = catFacts.get((index - displacement) % catFacts.size());
            String sid = sender.sendMessage(fact);
            System.out.println(sid);
        }

        // Rewrite numbersDisplacements after all texts have been sent.
        PrintStream fileOverwriter = new PrintStream(new File("numbersDisplacement.txt"));

        for(String info : displacements) {
            fileOverwriter.println(info);
        }

        fileOverwriter.close();
    }

    /**
     * Retrieve cat facts.
     *
     * @return ArrayList of facts.
     * @throws FileNotFoundException if file not found.
     */
    public static ArrayList<String> getFacts() throws FileNotFoundException {
        Scanner catFaxFile = new Scanner(new File("catFax.txt"));

        ArrayList<String> catFacts = new ArrayList<>();

        while (catFaxFile.hasNextLine()) {
            catFacts.add(catFaxFile.nextLine());
        }

        return catFacts;
    }

    /**
     * Send facts!
     *
     * @param sender    A MessageSender to send texts from.
     * @param catFacts  Array of various cat facts.
     * @throws FileNotFoundException if currentIndex.txt unwritable.
     * @throws TwilioRestException if something goes wrong.
     */
    public static void sendGroupFact(MessageSender sender, ArrayList<String> catFacts) throws FileNotFoundException, TwilioRestException {
        // Determine which cat fact to send based on the subscriber's displacement from current fact
        for (int i = 0; i < displacements.size(); i++) {
            String number = displacements.get(i);
            int displacement = Integer.parseInt(displacements.get(++i));

            // Retrieve catFact
            int thisIndex = index - displacement; //how come here its this but in the instant
            // method its using mod?
            String fact = catFacts.get(thisIndex);

            sender.setPhoneNumber(number);
            sender.sendMessage(fact);
        }

        PrintStream indexWriter = new PrintStream(new File("currentIndex.txt"));
        indexWriter.print(index + 1);
        indexWriter.close();

        index++;

        System.out.println("Group fact sent");
    }
}
