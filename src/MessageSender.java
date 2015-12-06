import java.util.*;
import com.twilio.sdk.*;
import com.twilio.sdk.resource.factory.*;
import com.twilio.sdk.resource.instance.*;
import com.twilio.sdk.resource.list.*;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
/**
 * Created by Parker on 12/1/2015.
 */
public class MessageSender {
    public String phoneNumber = "3603256564";
    public final String sendNumber = "+18187228329";
    TwilioRestClient client;
    List<NameValuePair> params;
    MessageFactory messageFactory;

    // Construct a MessageSender object for the current session
    public MessageSender(TwilioRestClient client) {
        this.client = client;
        this.params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("To", phoneNumber));
        params.add(new BasicNameValuePair("From", sendNumber));
        params.add(new BasicNameValuePair("Body", "WELCOME TO CATFAX!!!!"));
        this.messageFactory = client.getAccount().getMessageFactory();
    }

    // Return the phone number
    public String getPhoneNumber() {
        return phoneNumber;
    }

    // Set the phone number that will be dealt with
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        params.set(0, new BasicNameValuePair("To", phoneNumber));
    }

    // Send a message containing messageText
    public String sendMessage(String messageText) throws TwilioRestException{
        params.set(2, new BasicNameValuePair("Body", messageText));
        Message message = messageFactory.create(params);
        return message.getSid();
    }
}
/*
        // Build the parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("To", "3603256564"));
        params.add(new BasicNameValuePair("From", "+18187228329"));
        params.add(new BasicNameValuePair("Body", "WELCOME TO CATFAX!!!!"));
        MessageFactory messageFactory = client.getAccount().getMessageFactory();
        Message message = messageFactory.create(params);
        params.set(2, new BasicNameValuePair("Body", "Sedond Message"));
        Message message2 = messageFactory.create(params);

        System.out.println(message.getSid());
 */