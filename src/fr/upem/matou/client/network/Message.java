package fr.upem.matou.client.network;

import static java.util.Objects.requireNonNull;

import fr.upem.matou.shared.network.Username;

/**
 * This class represents a chat message.
 */
public class Message {
	private final Username username;
	private final String content;
	private final boolean isPrivate;

	/**
	 * Creates a new public message.
	 * 
	 * @param username
	 *            The username of the speaker.
	 * @param content
	 *            The content of the message.
	 */
	public Message(Username username, String content) {
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
	public Message(Username username, String content, boolean isPrivate) {
		this.username = requireNonNull(username);
		this.content = requireNonNull(content);
		this.isPrivate = isPrivate;
	}

	/**
	 * Returns the username of the speaker.
	 * 
	 * @return The username of the speaker.
	 */
	public Username getUsername() {
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
	 * Returns true if the message is private or false otherwise.
	 * 
	 * @return true if the message is private or false otherwise.
	 */
	public boolean isPrivate() {
		return isPrivate;
	}

	@Override
	public String toString() {
		String string;

		if (isPrivate) {
			string = "{" + username + "}";
		} else {
			string = "<" + username + ">";
		}

		return string + " " + content;
	}

}
