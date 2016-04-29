package fr.upem.matou.shared.network;

import java.util.Optional;

/**
 * This class provides all error types that the server can notify to the client.
 */
public enum ErrorType {

	/**
	 * Unknown error.
	 */
	UNK("UNKNOWN ERROR"),

	/**
	 * The requested user is not connected.
	 */
	USRNOTCO("USER NOT CONNECTED"),

	/**
	 * The requested user did not ask for private connection.
	 */
	USRNOTPVREQ("USER NOT PRIVATE REQUESTING"),

	;

	private final String description;

	private ErrorType(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "{" + name() + " : " + description + "}";
	}

	/**
	 * Returns the error type associated with this ordinal number.
	 *
	 * @param ordinal
	 *            The ordinal number of the error.
	 * @return The error type if the ordinal number is valid.
	 */
	public static Optional<ErrorType> getError(int ordinal) {
		ErrorType[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return Optional.empty();
		}
		ErrorType type = values[ordinal];
		return Optional.of(type);
	}
}
