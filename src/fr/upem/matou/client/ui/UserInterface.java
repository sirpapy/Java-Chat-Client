package fr.upem.matou.client.ui;

import java.util.Optional;

import fr.upem.matou.client.network.ClientEvent;

/**
 * This interface provides methods to display information to the user and retrieve information entered by the user.
 * All implementations of this interface should be transparent.
 */
public interface UserInterface {

	public Optional<String> getPseudo();

	public Optional<ClientEvent> getEvent();

	/**
	 * Displays a message to the user.
	 * 
	 * @param message
	 *            The message to display
	 */
	public void displayMessage(Message message);

	public void displayNewConnectionEvent(String pseudo);

	public void displayNewDisconnectionEvent(String pseudo);
	
	public void displayNewPrivateRequestEvent(String pseudo);

	public void warnInvalidPseudo(String pseudo);

	public void warnInvalidMessage(ClientEvent event);


}
