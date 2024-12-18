package com.minecrafttas.tasmod.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread for writing files to disc
 *
 * @author Pancake
 */
public class FileThread extends Thread {

	private final PrintWriter stream;
	private boolean end = false;

	private final List<String> output = new ArrayList<>();

	public FileThread(Path fileLocation, boolean append) throws IOException {
		OutputStream outStream = Files.newOutputStream(fileLocation, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		stream = new PrintWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
	}

	public void addLine(String line) {
		synchronized (output) {
			output.add(line + "\n");
		}
	}

	@Override
	public void run() {
		while (!end) {
			synchronized (output) {
				ArrayList<String> newList = new ArrayList<String>(output);
				output.clear();
				for (String line : newList) {
					stream.print(line);
				}
			}
		}
		stream.flush();
		stream.close();
	}

	public void close() {
		end = true;
	}

	public void flush() {
		stream.flush();
	}
}
