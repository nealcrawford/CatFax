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
    public MessageReader(TwilioRestClient client) {
        this.client = client;
    }
    public ArrayList<String> checkMessages() throws IOException{
        MessageList messages = client.getAccount().getMessages(/*params*/);
        ArrayList<String> newSIDs = new ArrayList<>();
        Scanner fileReader = new Scanner(new File("handledMessages.txt"));
        ArrayList<String> handledSIDs = new ArrayList<>();
        while (fileReader.hasNextLine()) {
            handledSIDs.add(fileReader.nextLine());
        }
        fileReader.close();
        for (Message message : messages) {

            String sid = message.getSid();
            boolean found = false;
            for(int i = 0; i < handledSIDs.size(); i++) {
                if (handledSIDs.get(i).equals(sid)) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                newSIDs.add(sid);
            }
        }

        if(newSIDs.size() > 0){
            FileOutputStream fileOutStream = new FileOutputStream("handledMessages.txt", true);
            PrintStream filePrinter = new PrintStream(fileOutStream);
            for(int i = 0; i < newSIDs.size(); i++) {
                filePrinter.println(newSIDs.get(i));
            }
            fileOutStream.close();
        }
        return newSIDs;
    }

}
