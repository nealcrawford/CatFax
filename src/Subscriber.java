import java.io.Serializable;

/**************************************************
 * Subscriber.java
 * <p/>
 * It's what you think it is
 **************************************************/

public class Subscriber implements Serializable {
    private String  phoneNumber;
    private int     displacement;

    /**
     * Constructs a new subscriber with default values.
     */
    public Subscriber() {
        this(null, 0);
    }

    /**
     * Constructs a new subscriber with given values.
     *
     * @param phoneNumber The subscriber's phone number.
     */
    public Subscriber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Constructs a new subscriber with given values.
     *
     * @param phoneNumber  The subscriber's phone number.
     * @param displacement The subscriber's displacement.
     */
    public Subscriber(String phoneNumber, int displacement) {
        this.phoneNumber = phoneNumber;
        this.displacement = displacement;
    }

    /**
     * Determine if two subscribers are equal by comparing their phone numbers only.
     *
     * @param o Object to be compared to
     * @return Whether they are equal
     */
    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(o instanceof String) {
            String that = (String) o;

            return phoneNumber != null && phoneNumber.equals(that);
        } else if(o instanceof Subscriber) {
            Subscriber that = (Subscriber) o;

            return phoneNumber != null && phoneNumber.equals(that.phoneNumber);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = phoneNumber != null ? phoneNumber.hashCode() : 0;
        result = 31 * result + displacement;
        return result;
    }

    /**
     * @return Subscriber's phone number
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * @param phoneNumber Subscriber's phone number
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * @return Subscriber's displacement.
     */
    public int getDisplacement() {
        return displacement;
    }

    /**
     * @param displacement Subscriber's displacement.
     */
    public void setDisplacement(int displacement) {
        this.displacement = displacement;
    }

    /**
     * @return String representation of the class
     */
    public String toString() {
        return "[phone: " + phoneNumber + " dis: " + displacement + "]";
    }
}
