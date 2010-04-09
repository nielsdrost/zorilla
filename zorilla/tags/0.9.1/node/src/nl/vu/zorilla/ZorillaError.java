package nl.vu.zorilla;

/**
 * Like java.lang.Error, but with a cause.
 */
public class ZorillaError extends Error {

    private static final long serialVersionUID = 1L;

    /** A nested throwable. */
    Throwable cause = null;

    /**
     * Constructsn a <code>IbisError</code> with <code>null</code> as its
     * error detail message.
     */
    public ZorillaError() {
        super();
    }

    /**
     * Constructs a <code>IbisError</code> with the specified detail message.
     * 
     * @param s
     *            the detail message
     */
    public ZorillaError(String s) {
        super(s);
    }

    /**
     * Constructs a <code>IbisError</code> with the specified detail message
     * and cause.
     * 
     * @param s
     *            the detail message
     * @param cause
     *            the cause
     */
    public ZorillaError(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }

    /**
     * Constructs a <code>IbisError</code> with the specified cause.
     * 
     * @param cause
     *            the cause
     */
    public ZorillaError(Throwable cause) {
        super();
        initCause(cause);
    }

    /**
     * Initializes and returns the cause of this error.
     * 
     * @return the cause.
     */
    public Throwable initCause(Throwable t) {
        return cause = t;
    }

    /**
     * Returns the cause of this error.
     * 
     * @return the cause.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the error detail message of this error, including the one of
     * <code>cause</code>, if there is one.
     * 
     * @return the detail message.
     */
    public String getMessage() {
        String res = super.getMessage();
        if (cause != null) {
            res += ": " + cause.getMessage();
        }

        return res;
    }

    /**
     * Prints stack trace of both <code>cause</code> and <code>this</code>.
     */
    public void printStackTrace() {
        if (cause != null) {
            cause.printStackTrace();
        }

        super.printStackTrace();
    }
}