package fr.upem.matou.shared.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings("javadoc")
public class FileUtils {
	public static String getFileExtension(Path path) {
		System.out.println("PATH = " + path);
		String filename = path.getFileName().toString();
		System.out.println("\tFILENAME = " + filename);

		String extension;
		int index = filename.indexOf('.');
		if (index == -1) {
			extension = "";
		} else {
			extension = filename.substring(index);
		}

		System.out.println("\tEXTENSION = " + extension);
		return extension;
	}

	public static void main(String[] args) {
		getFileExtension(Paths.get("./foo"));
		getFileExtension(Paths.get("./dico.txt"));
		getFileExtension(Paths.get("./archive.tar.gz"));
		getFileExtension(Paths.get("./.classpath"));
		getFileExtension(Paths.get("./lol/../foo"));
		getFileExtension(Paths.get("./lol/../dico.txt"));
		getFileExtension(Paths.get("./lol/../archive.tar.gz"));
		getFileExtension(Paths.get("./lol/../.classpath"));
		getFileExtension(Paths.get("../lol/foo"));
		getFileExtension(Paths.get("../lol/dico.txt"));
		getFileExtension(Paths.get("../lol/archive.tar.gz"));
		getFileExtension(Paths.get("../lol/.classpath"));
	}
}
