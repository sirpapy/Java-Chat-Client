package fr.upem.matou.client.ui;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import fr.upem.matou.client.network.ClientEvent;
import fr.upem.matou.client.network.Message;
import fr.upem.matou.shared.network.ErrorType;
import fr.upem.matou.shared.network.Username;

/**
 * This interface provides methods to display information to the user and retrieve information entered by the user. All
 * implementations of this interface should be transparent and thread-safe.
 */
public interface UserInterface extends Closeable {

	/**
	 * Retrieves the username given by the user.
	 * 
	 * @return The username.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public String getUsername() throws IOException;

	/**
	 * Retrieves an event requested by the user.
	 * 
	 * @return An optional describing the requested event or an empty optional if the user requested to leave the chat.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public Optional<ClientEvent> getEvent() throws IOException;

	/**
	 * Displays an error message to the user.
	 * 
	 * @param type
	 *            The error.
	 */
	public void warnError(ErrorType type);

	/**
	 * Displays a message to the user.
	 * 
	 * @param message
	 *            The message to display
	 */
	public void displayNewMessage(Message message);

	/**
	 * Displays a new connection event to the user.
	 * 
	 * @param username
	 *            The connected username.
	 */
	public void displayNewConnection(Username username);

	/**
	 * Displays a new disconnection event to the user.
	 * 
	 * @param username
	 *            The disconnected username
	 */
	public void displayNewDisconnection(Username username);

	/**
	 * Displays a new private connection request event to the user.
	 * 
	 * @param username
	 *            The requesting username.
	 */
	public void displayNewPrivateRequest(Username username);

	/**
	 * Displays a new private connection acceptation event to the user.
	 * 
	 * @param username
	 *            The accepting username.
	 */
	public void displayNewPrivateAcception(Username username);

	/**
	 * Displays a new private disconnection event (message channel) to the user.
	 * 
	 * @param username
	 *            The disconnected username
	 */
	public void displayNewPrivateMessageDisconnection(Username username);

	/**
	 * Displays a new private disconnection event (file channel) to the user.
	 * 
	 * @param username
	 *            The disconnected username
	 */
	public void displayNewPrivateFileDisconnection(Username username);

	/**
	 * Displays a new private file reception event to the user.
	 * 
	 * @param username
	 *            The sender username.
	 * @param path
	 *            The path to the saved file.
	 */
	public void displayNewFileReception(String username, Path path);

	/**
	 * Warns the user that this username is not valid.
	 * 
	 * @param username
	 *            The username.
	 */
	public void warnInvalidUsername(String username);

	/**
	 * Warns the user that this event is not valid.
	 * 
	 * @param event
	 *            The event.
	 */
	public void warnInvalidMessageEvent(ClientEvent event);

	/**
	 * Warns the user that this username is not currently available.
	 * 
	 * @param username
	 *            The username.
	 */
	public void warnUnavailableUsername(String username);

}
