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

	public Optional<String> getUsername();

	public Optional<ClientEvent> getEvent();

	/**
	 * Displays a message to the user.
	 * 
	 * @param message
	 *            The message to display
	 */
	public void displayMessage(Message message);

	public void displayNewConnectionEvent(String username);

	public void displayNewDisconnectionEvent(String username);

	public void displayNewPrivateRequestEvent(String username);

	public void displayNewPrivateAcceptionEvent(String username);

	public void displayNewPrivateDisconnection(Username username);

	public void displayNewFileReception(String username, Path path);

	public void warnInvalidUsername(String username);

	public void warnInvalidMessage(ClientEvent event);


}
