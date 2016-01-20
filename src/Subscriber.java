import java.io.Serializable;

/**************************************************
 * Subscriber.java
 * <p/>
 * It's what you think it is
 **************************************************/

public class Subscriber implements Serializable {
    private String  phoneNumber;
    private int     displacement;
    private boolean needsFact;

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
        result = 31 * result + (needsFact ? 1 : 0);
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
     * @return If the user needs a fact
     */
    public boolean needsFact() {
        return needsFact;
    }

    /**
     * Set whether the user needs a fact or not.
     *
     * @param needsFact Whether the user needs a fact
     */
    public void needsFact(boolean needsFact) {
        this.needsFact = needsFact;
    }
}
