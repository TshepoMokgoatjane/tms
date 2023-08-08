package za.co.tms.exception;

public class TenantNotFoundException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TenantNotFoundException(String message) {
        super(message);
    }
}
