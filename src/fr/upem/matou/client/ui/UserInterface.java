package fr.upem.matou.client.ui;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Optional;

import fr.upem.matou.client.network.ClientEvent;
import fr.upem.matou.client.network.Message;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.Username;

/**
 * This interface provides methods to display information to the user and retrieve information entered by the user.
 * All implementations of this interface should be transparent.
 */
public interface UserInterface extends Closeable {

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
	public void displayNewConnectionEvent(Username username);

	/**
	 * Display a new disconnection event to the user.
	 * 
	 * @param username
	 *            The new disconnected
	 */
	public void displayNewDisconnectionEvent(Username username);

	public void displayNewPrivateRequestEvent(Username username);

	public void displayNewPrivateAcceptionEvent(Username username);

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

	public void warnUnavailableUsername(String username);

	public void displayError(ErrorType type);

}
