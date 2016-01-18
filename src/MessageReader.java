/********************************************
 * MessageReader.java
 * <p/>
 * Checks for any unhandled messages
 ********************************************/

// You may want to be more specific in your imports

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.instance.Message;
import com.twilio.sdk.resource.list.MessageList;

import java.io.*;
import java.util.*;


public class MessageReader {
    TwilioRestClient client;
    Map<String, String> filters;

    /**
     * Creates a new MessageReader
     *
     * @param client    Twillio Client
     * @param filters   https://www.twilio.com/docs/api/rest/message#list-get-filters
     */
    public MessageReader(TwilioRestClient client, Map<String, String> filters) {
        this.client = client;
        this.filters = filters;
    }

    /**
     * Returns an ArrayList of new messages following criteria passed in constructor.
     *
     * @return An ArrayList of new filtered messages from handledMessages.txt
     * @throws IOException  if handledMessages.txt is not found.
     */
    public List<String> checkMessages() throws IOException {
        MessageList messages = client.getAccount().getMessages(filters);

        List<String> newSIDs = new ArrayList<>();
        List<String> handledSIDs = readInHandled();

        for (Message message : messages) {
            String sid = message.getSid();
            boolean found = false;
            // Check messages against previously handled messages list
            for(String handled : handledSIDs) {
                if (handled.equals(sid)) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                newSIDs.add(sid);
                System.out.println("newSID added");
            }
        }

        // Add handled messages to handledMessages.txt
        if(newSIDs.size() > 0){
            FileOutputStream fileOutStream = new FileOutputStream("handledMessages.txt", true);
            PrintStream filePrinter = new PrintStream(fileOutStream);
            for(String newSid : newSIDs) {
                filePrinter.println(newSid);
            }
            fileOutStream.close();
        }
        return newSIDs;
    }

    /**
     * Retrieve handled messages from handledMessages.txt.
     *
     * @return ArrayList of handled messages.
     * @throws FileNotFoundException if handledMessages.txt does not exist.
     */
    public static List<String> readInHandled() throws FileNotFoundException {
        Scanner fileReader = new Scanner(new File("handledMessages.txt"));

        List<String> handledSIDs = new ArrayList<>();

        while (fileReader.hasNextLine()) {
            handledSIDs.add(fileReader.nextLine());
        }

        fileReader.close();

        return handledSIDs;
    }
}
