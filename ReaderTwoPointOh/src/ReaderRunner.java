import com.twilio.sdk.TwilioRestClient;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Parker on 1/11/2016.
 */
public class ReaderRunner {
    public static final String ACCOUNT_SID = "AC3fd9f1b394e4fbcff3966c17c131ef97";
    public static final String AUTH_TOKEN = "5f187afdea5b5b94aaa64d421fb486f7";
    public static TwilioRestClient client;
    public static void main(String[] args) {
        client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
        System.out.println(getDate());
        Map<String, String> filters = new HashMap<String, String>();
        filters.put("DateSent", getDate());
        filters.put("To", "+18187228329");
        System.out.println(filters);
        ReaderVTwo reader = new ReaderVTwo(client, filters);
        reader.checkMessages();

    }
    public static String getDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = 1 + calendar.get(Calendar.MONTH);
        String monthStr = "";
        String dayStr = "";

        if(month < 10){
            monthStr += "0" +month;
        } else{
            monthStr += month;
        }

        if(day < 10){
            dayStr += "0" + day;
        } else{
            dayStr += day;
        }

        return "" + year + "-" + monthStr + "-" + dayStr;
    }
}
