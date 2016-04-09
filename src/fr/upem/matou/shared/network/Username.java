package fr.upem.matou.shared.network;

public class Username {
	private final String name;

	public Username(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Username)) {
			return false;
		}
		Username username = (Username) obj;
		return name.equalsIgnoreCase(username.name);
	}

	@Override
	public int hashCode() {
		return name.toUpperCase().toLowerCase().hashCode();
	}

	@Override
	public String toString() {
		return name;
	}

}
