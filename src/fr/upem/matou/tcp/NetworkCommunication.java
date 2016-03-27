package fr.upem.matou.tcp;

import java.nio.charset.Charset;

/*
 * This class gathers the common factors between ClientCommunication and ServerCommunication.
 */
class NetworkCommunication {
	static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");
	
	private NetworkCommunication() {
	}
}
