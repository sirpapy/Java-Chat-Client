package fr.upem.matou.client.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import fr.upem.matou.client.ui.ShellInterface;
import fr.upem.matou.client.ui.UserInterface;
import fr.upem.matou.shared.logger.Logger;

/**
 * This class is the core of the chat client.
 */
public class ClientCore implements Closeable {

	private final InetSocketAddress address;
	private final UserInterface ui = new ShellInterface();

	/**
	 * Constructs a new client core.
	 * 
	 * @param hostname
	 *            The server hostname.
	 * @param port
	 *            The serveur pore.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public ClientCore(String hostname, int port) throws IOException {
		address = new InetSocketAddress(hostname, port);
	}

	/**
	 * Starts a chat with a predefined username. If this username is not available or not valid, the chat session will
	 * end immediately.
	 * 
	 * @param username
	 *            The choosen username.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws InterruptedException
	 *             If an interruption occurs.
	 */
	public void startChat(String username) throws IOException, InterruptedException {
		try (ClientInstance chat = new ClientInstance(address, ui)) {
			chat.start(username);
		}
	}

	/**
	 * Starts a chat without a predefined username. User will have to choose a username manually.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws InterruptedException
	 *             If an interruption occurs.
	 */
	public void startChat() throws IOException, InterruptedException {
		try (ClientInstance chat = new ClientInstance(address, ui)) {
			chat.start();
		}
	}

	@Override
	public void close() throws IOException {
		Logger.debug("CLIENT CORE CLOSING");
		ui.close();
	}

}
