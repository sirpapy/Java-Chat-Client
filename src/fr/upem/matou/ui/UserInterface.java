package fr.upem.matou.ui;

import java.util.Optional;

/**
 * This interface provides methods to display information to the user and retrieve information entered by the user.
 * All implementations of this interface should be transparent.
 */
public interface UserInterface {

	/**
	 * Reads a username entered by the user.
	 * 
	 * @return The pseudo
	 */
	public Optional<String> readPseudo();

	/**
	 * Reads a message entered by the user.
	 * 
	 * @return The message
	 */
	public Optional<String> readMessage();

	/**
	 * Displays a message to the user.
	 * 
	 * @param message
	 *            The message to display
	 */
	public void displayMessage(Message message);

	public void displayNewConnectionEvent(String pseudo);

	public void displayNewDisconnectionEvent(String pseudo);

}
