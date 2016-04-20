package fr.upem.matou.shared.network;

import java.util.Optional;

public enum ErrorType {
	UNK("UNKNOWN ERROR"),
	USRNOTCO("USER NOT CONNECTED"),
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
	
	public static Optional<ErrorType> getError(int ordinal) {
		ErrorType[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return Optional.empty();
		}
		ErrorType type = values[ordinal];
		return Optional.of(type);
	}
}
