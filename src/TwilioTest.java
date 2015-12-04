// You may want to be more specific in your imports
import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.twilio.sdk.*;

import com.twilio.sdk.resource.instance.*;


public class TwilioTest {
    // Find your Account Sid and Token at twilio.com/user/account
    public static final String ACCOUNT_SID = "AC3fd9f1b394e4fbcff3966c17c131ef97";
    public static final String AUTH_TOKEN = "5f187afdea5b5b94aaa64d421fb486f7";
    public static int count = 0;
    public static int minutes = 960; //1 day assuming each program loop takes 1.5 seconds
    public static final int SEC_IN_MIN = 60;
    public static int index;

    public static void main(String[]args) throws TwilioRestException, IOException {
        TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
        MessageSender sender = new MessageSender(client);
        Scanner indexFile = new Scanner(new File("currentIndex.txt"));
        index = indexFile.nextInt();
        indexFile.close();

        // Runs the programLoop once a second
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    programLoop(client, sender);
                } catch (TwilioRestException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    public static void programLoop(TwilioRestClient client, MessageSender sender)throws TwilioRestException, IOException{

        //Checks if a text needs to be replied to
        MessageReader reader = new MessageReader(client);
        ArrayList<String> newMessages = reader.checkMessages();
        boolean sendMessage = false;
        ArrayList<String> needsMessage = new ArrayList<>();
        ArrayList<String> msgTxt = new ArrayList<>();
        for(int i = 0; i < newMessages.size(); i++) {
            Message message = client.getAccount().getMessage(newMessages.get(i));
            String from = message.getFrom();
            if (!from.equals("+18187228329")) {
                if (subscriber(from)) {
                    needsMessage.add(from);
                    msgTxt.add(message.getBody());
                } else {
                    addAsSubscriber(from);
                }
            }
        }

        // Send an instant Cat Fact to numbers that request one
        for(int i = 0; i < needsMessage.size(); i++) {
            sender.setPhoneNumber(needsMessage.get(i));
            Scanner indexFile = new Scanner(new File("currentIndex.txt"));
            int index = indexFile.nextInt();
            indexFile.close();
            String sid = sender.sendMessage(getInstantCatFact((""+sender.getPhoneNumber()), index));
            System.out.println(sid);
        }

        // Send any necessary catFAX
        count++;
        if(count > (minutes * SEC_IN_MIN)){
            sendCatFacts(sender);
            count = 0;
        }
    }

    public static void addAsSubscriber(String number) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("numbersDisplacement.txt", true));
        bw.write(number);
        bw.newLine();
        bw.write(index);
        bw.newLine();
        bw.close();
    }

    // Send the timed subscriber cat fact, update the current fact
    public static void sendCatFacts(MessageSender sender)throws FileNotFoundException, TwilioRestException{
        PrintStream indexWriter = new PrintStream(new File("currentIndex.txt"));
        indexWriter.print(index + 1);
        indexWriter.close();
        getGroupCatFact(index, sender);
        System.out.println("sentMessages");
    }

    // Send the timed cat fact to subscribers
    public static void getGroupCatFact(int index, MessageSender sender) throws FileNotFoundException, TwilioRestException {
        Scanner numDispFile = new Scanner(new File("numbersDisplacement.txt"));
        Scanner catFaxFile = new Scanner(new File("catFax.txt"));
        ArrayList<String> catFax = new ArrayList<>();
        while (catFaxFile.hasNextLine()){
            catFax.add(catFaxFile.nextLine());
        }

        // Determine which cat fact to send based on the subscriber's displacement from current fact
        while(numDispFile.hasNextLine()) {
            String number = numDispFile.nextLine();
            int displacement = Integer.parseInt(numDispFile.nextLine());
            // Retrieve catFact
            int thisIndex = index - displacement;
            String catFact = catFax.get(thisIndex);
            sender.setPhoneNumber(number);
            sender.sendMessage(catFact);
        }
        numDispFile.close();
    }

    // Return a new and instant cat fact, update the subscriber's displacement
    public static String getInstantCatFact(String number, int index) throws FileNotFoundException {
        Scanner numDispFile = new Scanner(new File("numbersDisplacement.txt"));
        // Put file into arraylist so we can close the file
        ArrayList<String> numDisp = new ArrayList<>();
        while(numDispFile.hasNextLine()) {
            numDisp.add(numDispFile.nextLine());
        }
        numDispFile.close();
        int displacement = 0; //need to instantiate
        for(int i = 0; i < numDisp.size(); i += 2){
            if (numDisp.get(i).equals(number)){
                displacement = Integer.parseInt(numDisp.get(i+1));
                numDisp.set((i + 1), "" + (displacement - 1)); //update displacement in ArrayList to be written in file
                break;
            }
        }

        // Rewrite printstream
        PrintStream fileOverwriter = new PrintStream(new File("numbersDisplacement.txt"));
        for(int i = 0; i < numDisp.size(); i++){
            fileOverwriter.println(numDisp.get(i));
        }
        fileOverwriter.close();
        // Retrieve catFact
        index -= displacement;
        Scanner catFaxFile = new Scanner(new File("catFax.txt"));
        ArrayList<String> catFax = new ArrayList<>();
        while(catFaxFile.hasNextLine()) {
            catFax.add(catFaxFile.nextLine());
        }
        String catFact = catFax.get(index % catFax.size());
        return catFact;
    }

    public static boolean subscriber(String number) throws FileNotFoundException {
        Scanner numDispFile = new Scanner(new File("numbersDisplacement.txt"));
        while (numDispFile.hasNextLine()) {
            if (number.equals(numDispFile.nextLine())) {
                return true;
            }
        }
        return false;
    }
}

