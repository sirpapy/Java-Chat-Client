package fr.upem.matou.client.ui;

/**
 * This class represents a chat message.
 */
public class Message {
	private final String pseudo;
	private final String content;
	private final boolean isPrivate;

	/**
	 * Creates a new message.
	 * 
	 * @param pseudo
	 *            The pseudo of the speaker.
	 * @param content
	 *            The content of the message.
	 */
	public Message(String pseudo, String content) {
		this(pseudo, content, false);
	}

	/**
	 * Creates a new message.
	 * 
	 * @param pseudo
	 *            The pseudo of the speaker.
	 * @param content
	 *            The content of the message.
	 * @param isPrivate
	 *            true if the message is private, false otherwise.
	 */
	public Message(String pseudo, String content, boolean isPrivate) {
		this.pseudo = pseudo;
		this.content = content;
		this.isPrivate = isPrivate;
	}

	/**
	 * Returns the pseudo of the speaker.
	 * 
	 * @return The pseudo of the speaker.
	 */
	public String getPseudo() {
		return pseudo;
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
			string = "#" + pseudo + "#";

		} else {
			string = "<" + pseudo + ">";
		}

		return string + " " + content;
	}

}
