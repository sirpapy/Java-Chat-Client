package fr.upem.matou.client.ui;

import java.nio.file.Path;
import java.util.Optional;

import fr.upem.matou.client.network.ClientEvent;
import fr.upem.matou.client.network.Message;
import fr.upem.matou.shared.network.Username;

/**
 * This interface provides methods to display information to the user and retrieve information entered by the user.
 * All implementations of this interface should be transparent.
 */
public interface UserInterface {

	/**
	 * Retrieves the username given by the user.
	 * 
	 * @return The username.
	 */
	public Optional<String> getUsername();

	/**
	 * Retrieves an event requested by the user.
	 * 
	 * @return An event.
	 */
	public Optional<ClientEvent> getEvent();

	/**
	 * Displays a message to the user.
	 * 
	 * @param message
	 *            The message to display
	 */
	public void displayMessage(Message message);

	/**
	 * Displays a new connection event to the user.
	 * 
	 * @param username
	 *            The new connected
	 */
	public void displayNewConnectionEvent(String username);

	/**
	 * Display a new disconnection event to the user.
	 * 
	 * @param username
	 *            The new disconnected
	 */
	public void displayNewDisconnectionEvent(String username);

	public void displayNewPrivateRequestEvent(String username);

	public void displayNewPrivateAcceptionEvent(String username);

	public void displayNewPrivateMessageDisconnection(Username username);
	
	public void displayNewPrivateFileDisconnection(Username username);

	public void displayNewFileReception(String username, Path path);

	/**
	 * Warns the user that this username is not valid.
	 * 
	 * @param username
	 *            The username
	 */
	public void warnInvalidUsername(String username);

	/**
	 * Warns the user that this event is not valid.
	 * 
	 * @param event
	 *            The event
	 */
	public void warnInvalidMessage(ClientEvent event);

}
