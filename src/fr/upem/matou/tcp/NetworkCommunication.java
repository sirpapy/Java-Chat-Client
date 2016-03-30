package fr.upem.matou.tcp;

import java.nio.charset.Charset;

/*
 * This class gathers the common factors between ClientCommunication and ServerCommunication.
 */
class NetworkCommunication {
	static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");
	private static final int PSEUDO_MAX_LENGTH = 32;
	private static final int MESSAGE_MAX_LENGTH = 512;

	private NetworkCommunication() {
	}

	private static boolean isValidPseudoCharacter(int codePoint) {
		return Character.isLetterOrDigit(codePoint);
	}

	private static boolean isValidMessageCharacter(int codePoint) {
		return !Character.isISOControl(codePoint);
	}

	// TODO : Double check
	static boolean checkPseudoValidity(String pseudo) {
		return pseudo.length() <= PSEUDO_MAX_LENGTH
				&& pseudo.chars().allMatch(NetworkCommunication::isValidPseudoCharacter);
	}

	// TODO : Double check
	static boolean checkMessageValidity(String message) {
		return message.length() <= MESSAGE_MAX_LENGTH
				&& message.chars().allMatch(NetworkCommunication::isValidMessageCharacter);
	}
}
