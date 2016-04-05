package fr.upem.matou.debug.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class FileTransfer {

	private static final int CHUNK_SIZE = 4096; // in bytes

	private static void copyFileChunked(Path inputPath, Path outputPath)
			throws IOException {
		long totalSize = Files.size(inputPath);
		try (InputStream is = Files.newInputStream(inputPath, StandardOpenOption.READ)) {
			try (OutputStream os = Files.newOutputStream(outputPath, StandardOpenOption.WRITE,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

				byte[] chunk = new byte[CHUNK_SIZE];
				int read = 0;
				long totalRead = 0;
				while ((read = is.read(chunk)) != -1) {
					totalRead += read;
					long percent = totalRead * 100 / totalSize;
					System.out.println("READ LENGTH : " + read + "\n\tTotal : " + totalRead + "/" + totalSize + " [" + percent + "%]");
					os.write(chunk, 0, read);
				}
			}
		}
	}

	private static boolean checkFileEquals(Path inputPath, Path outputPath) throws IOException {
		byte[] inputBytes = Files.readAllBytes(inputPath);
		byte[] outputBytes = Files.readAllBytes(outputPath);

		return Arrays.equals(inputBytes, outputBytes);
	}
	
	// TODO : Network Sender/Receiver

	public static void main(String[] args) throws IOException {
		Path inputPath = Paths.get(args[0]);
		Path outputPath = Paths.get(args[1]);
		copyFileChunked(inputPath, outputPath);
		System.out.println("EQUALS : " + checkFileEquals(inputPath, outputPath));
	}
}
