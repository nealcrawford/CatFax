// You may want to be more specific in your imports
import java.util.*;
import com.twilio.sdk.*;
import com.twilio.sdk.resource.factory.*;
import com.twilio.sdk.resource.instance.*;
import com.twilio.sdk.resource.list.*;
import org.apache.http.NameValuePair;
import java.io.*;


public class MessageReader {
    // Check for any unhandled messages
    TwilioRestClient client;
    Map<String, String> filters;

    // Accepts a Twillio Client and a Map with message filters
    // Filters: https://www.twilio.com/docs/api/rest/message#list-get-filters
    public MessageReader(TwilioRestClient client, Map<String, String> filters) {
        this.client = client;
        this.filters = filters;
    }

    // Returns an ArrayList of new messages following criteria passed in constructor
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

    // Read in handled messages to an ArrayList
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
