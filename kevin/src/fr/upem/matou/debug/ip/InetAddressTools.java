package fr.upem.matou.debug.ip;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class InetAddressTools {
	private InetAddressTools() {
	}

	public static void printAddress(InetAddress address) {
		byte[] bytes = address.getAddress();

		switch (bytes.length) {
		case 4:
			System.out.println("IPv4");
			break;
		case 16:
			System.out.println("IPv6");
			break;
		default:
			throw new AssertionError("Invalid IP address size");
		}

		for(byte b : bytes) {
			System.out.println("=> " + b);
		}

	}
	
	public static void main(String[] args) {
		InetSocketAddress isa = new InetSocketAddress("localhost",7777);
		InetAddress ia = isa.getAddress();
		printAddress(isa.getAddress());
		System.out.println(isa);
		System.out.println(ia);
	}
}
