package fr.upem.matou.nonblocking.test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;

public enum NetworkProtocol {
	/* COREQ */ CLIENT_PUBLIC_CONNECTION_REQUEST("COREQ"),
	/* CORES */ SERVER_PUBLIC_CONNECTION_RESPONSE("CORES"),
	/* CODISP */ SERVER_PUBLIC_CONNECTION_NOTIFICATION("CODISP"),
	/* MSG */ CLIENT_PUBLIC_MESSAGE("MSG"),
	/* MSGBC */ SERVER_PUBLIC_MESSAGE_BROADCAST("MSGBC"),
	// /* PVCOREQ */ CLIENT_PRIVATE_CONNECTION_REQUEST("PVCOREQ"),
	// /* PVCOTR */ SERVER_PRIVATE_CONNECTION_TRANSFER("PVCOTR"),
	// /* PVCOACC */ CLIENT_PRIVATE_CONNECTION_CONFIRM("PVCOACC"),
	// /* PVCORES */ SERVER_PRIVATE_CONNECTION_RESPONSE("PVCORES"),
	// /* PVCOETA */ SERVER_PRIVATE_CONNECTION_ETABLISHMENT("PVCOETA"),
	// /* PVMSG */ CLIENT_PRIVATE_MESSAGE("PVMSG"),
	// /* PVFILE */ CLIENT_PRIVATE_FILE("PVFILE"),
	// /* PVDISCO */ CLIENT_PRIVATE_DISCONNECTION("PVDISCO"),
	/* DISCO */ CLIENT_PUBLIC_DISCONNECTION("DISCO"),
	;

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private final String code;

	private NetworkProtocol(String code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return "[" + ordinal() + " - " + code + "]";
	}

	public static Optional<NetworkProtocol> getProtocol(int ordinal) {
		NetworkProtocol[] values = values();
		if (ordinal < 0 || ordinal >= values.length) {
			return Optional.empty();
		}
		NetworkProtocol protocol = values[ordinal];
		return Optional.of(protocol);
	}


}
