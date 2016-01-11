import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.twilio.sdk.*;
import com.twilio.sdk.resource.instance.Message;

public class TwilioTest {
    public static final String ACCOUNT_SID = "AC3fd9f1b394e4fbcff3966c17c131ef97";
    public static final String AUTH_TOKEN = "5f187afdea5b5b94aaa64d421fb486f7";
    public static ArrayList<String> displacements;
    public static boolean messagesSent;
    public static boolean reconnect;
    public static int index;
    public static final String FACT_TIME = "15:15"; // 3:15 PM
    public static final String KILLSWITCH_CONFIRM = "Killswitch Activated";
    public static TwilioRestClient client;
    public static MessageSender sender;
    public static Calendar calendar;

    public static void main(String[] args) throws FileNotFoundException {
        client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
        sender = new MessageSender(client);

        getDisplacements();
        getIndex();
        final ArrayList<String> catFacts = getFacts();

        final ScheduledExecutorService executorService
                = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    eventLoop(catFacts);
                } catch (TwilioRestException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void getIndex() throws FileNotFoundException {
        Scanner indexFile = new Scanner(new File("currentIndex.txt"));
        index = indexFile.nextInt();
    }

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
        ArrayList<String> needsFact = parseInbox(getInbox());
        sendInstantFacts(needsFact, catFacts);

        if (time.equals(FACT_TIME) && !messagesSent) {
            sendGroupFact(sender, catFacts);
            messagesSent = true;
        }
        if (!time.equals(FACT_TIME)) {
            messagesSent = false;
        }
    }

    public static String getTime() {
        calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return "" + hour + ":" + minute;
    }

    public static void reconnect(String time) {
        int minute = Integer.parseInt(time.substring(time.indexOf(":") + 1));
        if ((minute == 0) || (minute ==30 ) && !reconnect) {
            client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
            sender = new MessageSender(client);
            reconnect = true;
        }
        if (minute != 0 && minute != 30) {
            reconnect = false;
        }
    }

    public static ArrayList<String> getInbox() {
        try {
            MessageReader reader = new MessageReader(client);
            return reader.checkMessages();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static ArrayList<String> parseInbox(ArrayList<String> inbox) throws TwilioRestException, IOException {
        ArrayList<String> needsFact = new ArrayList<>();
        for(String newMessage : inbox) {
            Message message = client.getAccount().getMessage(newMessage);
            String from = message.getFrom();
            if (!from.equals("+18187228329")) {
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
        }
        return needsFact;
    }

    // Return true if the specified numbers text "KILL ALL" to CatFax
    public static boolean isKillswitch(String number, String message) {
        if ((number.equals("+13603931867") || number.equals("+13603256564")) && message.equals("KILLALL")) {
            return true;
        } else {
            return false;
        }
    }

    // Check whether the number is currently a subscriber or not
    public static boolean isSubscriber(String number) {
        for (String phoneNumber : displacements) {
            if (number.equals(phoneNumber)) {
                return true;
            }
        }
        return false;
    }

    // Add new numbers as subscribers to CatFax
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
        System.out.println("Subscriber Added");
    }

    public static void sendInstantFacts(ArrayList<String> needsFact, ArrayList<String> catFacts)
            throws TwilioRestException, FileNotFoundException {
        for (String phoneNumber : needsFact) {
            sender.setPhoneNumber(phoneNumber);
            int displacement = 0; //need to instantiate
            for(int i = 0; i < displacements.size(); i += 2){
                if (displacements.get(i).equals(phoneNumber)){
                    displacement = Integer.parseInt(displacements.get(i+1)); // Get the subscriber's displacement
                    displacements.set((i + 1), "" + (displacement - 1)); // update their displacement in ArrayList to be written in file
                    break;
                }
            }

            // Rewrite printstream
            PrintStream fileOverwriter = new PrintStream(new File("numbersDisplacement.txt"));
            for(String info : displacements){
                fileOverwriter.println(info);
            }
            fileOverwriter.close();
            // Retrieve catFact
            String fact = catFacts.get((index - displacement) % catFacts.size());
            String sid = sender.sendMessage(fact);
            System.out.println(sid);
        }
    }

    public static ArrayList<String> getFacts() throws FileNotFoundException {
        Scanner catFaxFile = new Scanner(new File("catFax.txt"));
        ArrayList<String> catFacts = new ArrayList<>();
        while (catFaxFile.hasNextLine()) {
            catFacts.add(catFaxFile.nextLine());
        }
        return catFacts;
    }

    public static void sendGroupFact(MessageSender sender, ArrayList<String> catFacts) throws FileNotFoundException, TwilioRestException {
        // Determine which cat fact to send based on the subscriber's displacement from current fact
        for (int i = 0; i < displacements.size(); i++) {
            String number = displacements.get(i++);
            int displacement = Integer.parseInt(displacements.get(i));
            // Retrieve catFact
            int thisIndex = index - displacement;
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