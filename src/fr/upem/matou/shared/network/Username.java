package fr.upem.matou.shared.network;

import static java.util.Objects.requireNonNull;

/**
 * This object represents a Username. A username is a string where case is ignored.
 */
public class Username {
	private final String name;

	/**
	 * Returns a new username from a string.
	 * 
	 * @param name
	 *            The username
	 */
	public Username(String name) {
		this.name = requireNonNull(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Username)) {
			return false;
		}
		Username username = (Username) obj;
		return name.toLowerCase().equals(username.name.toLowerCase());
	}

	@Override
	public int hashCode() {
		return name.toLowerCase().hashCode();
	}

	@Override
	public String toString() {
		return name;
	}

}
