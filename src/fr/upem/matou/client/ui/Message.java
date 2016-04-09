package fr.upem.matou.client.ui;

/**
 * This class represents a chat message.
 */
public class Message {
	private final String username;
	private final String content;
	private final boolean isPrivate;

	/**
	 * Creates a new message.
	 * 
	 * @param username
	 *            The username of the speaker.
	 * @param content
	 *            The content of the message.
	 */
	public Message(String username, String content) {
		this(username, content, false);
	}

	/**
	 * Creates a new message.
	 * 
	 * @param username
	 *            The username of the speaker.
	 * @param content
	 *            The content of the message.
	 * @param isPrivate
	 *            true if the message is private, false otherwise.
	 */
	public Message(String username, String content, boolean isPrivate) {
		this.username = username;
		this.content = content;
		this.isPrivate = isPrivate;
	}

	/**
	 * Returns the username of the speaker.
	 * 
	 * @return The username of the speaker.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the content of the message.
	 * 
	 * @return The content of the message.
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Returns true if the message is private, false otherwise.
	 * 
	 * @return true if the message is private, false otherwise.
	 */
	public boolean isPrivate() {
		return isPrivate;
	}

	@Override
	public String toString() {
		String string;

		if (isPrivate) {
			string = "#" + username + "#";

		} else {
			string = "<" + username + ">";
		}

		return string + " " + content;
	}

}
