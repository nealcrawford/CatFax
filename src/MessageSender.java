/**
 * MessageSender.java
 *
 * Sends a message using the Twilio API
 */

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.*;

public class MessageSender {

    private String phoneNumber;
    private final String sendNumber;

    TwilioRestClient client;
    List<NameValuePair> params;
    MessageFactory messageFactory;

    /**
     * Construct a MessageSender object for the current session.
     *
     * @param client      The TwilioRestClient to use.
     * @param catFaxPhone The phone number to send from
     */
    public MessageSender(TwilioRestClient client, String catFaxPhone) {
        this.client = client;
        this.params = new ArrayList<>();
        this.sendNumber = catFaxPhone;

        params.add(new BasicNameValuePair("To", phoneNumber));
        params.add(new BasicNameValuePair("From", sendNumber));

        params.add(new BasicNameValuePair("Body", "WELCOME TO CATFAX!!!!"));

        this.messageFactory = client.getAccount().getMessageFactory();
    }

    /**
     * Set the phone number that will be dealt with.
     *
     * @param phoneNumber The number to send to.
     * @throws IllegalArgumentException if sendNumber and phoneNumber are equal.
     */
    //
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;

        if(phoneNumber.equals(sendNumber)){
            throw new IllegalArgumentException("sent message to self, this is bad and you should feel bad");
        }

        params.set(0, new BasicNameValuePair("To", phoneNumber));
    }

    /**
     * Send a text message.
     *
     * @param messageText The text to send.
     * @return The Sid of the message.
     * @throws TwilioRestException if something happens, idfk.
     */
    public String sendMessage(String messageText) throws TwilioRestException{
        params.set(2, new BasicNameValuePair("Body", messageText));

        Message message = messageFactory.create(params);

        return message.getSid();
    }
}