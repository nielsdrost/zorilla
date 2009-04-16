package ibis.zorilla.io;

/**
 * Like java.lang.Exception, but with a cause.
 */
public class ZorillaFileException extends Exception {

    private static final long serialVersionUID = 1L;

    /** A nested throwable. */
    Throwable cause = null;

    /**
     * Constructs an <code>ZorillaFileException</code> with <code>null</code> as
     * its error detail message.
     */
    public ZorillaFileException() {
        super();
    }

    /**
     * Constructs an <code>ZorillaFileException</code> with the specified detail
     * message.
     * 
     * @param s
     *            the detail message
     */
    public ZorillaFileException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>ZorillaFileException</code> with the specified detail
     * message and cause.
     * 
     * @param s
     *            the detail message
     * @param cause
     *            the cause
     */
    public ZorillaFileException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }

    /**
     * Constructs an <code>ZorillaFileException</code> with the specified cause.
     * 
     * @param cause
     *            the cause
     */
    public ZorillaFileException(Throwable cause) {
        super();
        initCause(cause);
    }

    /**
     * Initializes and returns the cause of this exception.
     * 
     * @return the cause.
     */
    public Throwable initCause(Throwable t) {
        return cause = t;
    }

    /**
     * Returns the cause of this exception.
     * 
     * @return the cause.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the error detail message of this exception, including the one of
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