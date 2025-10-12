package za.co.tms.exception;

import java.io.Serial;

public class TenantNotFoundException extends RuntimeException {
    /**
	 * 
	 */
	@Serial
    private static final long serialVersionUID = 1L;

	public TenantNotFoundException(String message) {
        super(message);
    }
}
