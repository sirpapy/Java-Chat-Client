package fr.upem.matou.tcp;

import static org.junit.Assert.*;

import org.junit.Test;

import fr.upem.matou.main.ClientMatou;
import fr.upem.matou.main.ServerMatou;

@SuppressWarnings("static-method")
public class NetworkTest {

	private static final Runnable serverMain = () -> {
		try {
			String[] args = new String[1];
			args[0] = "7777";
			ServerMatou.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	};

	private static final Runnable clientMain = () -> {
		try {
			String[] args = new String[2];
			args[0] = "localhost";
			args[1] = "7777";
			ClientMatou.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	};
	
	@Test
	public void testConnection() throws InterruptedException {
		Thread server = new Thread(serverMain);
		Thread client = new Thread(clientMain);
		server.start();
		client.start();
		
		client.join();
		server.interrupt();
	}

}
