package com.minecrafttas.tasmod.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ClipboardUtil {
	public static void copyToClipboard(String text) {
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection data = new StringSelection(text);
		cb.setContents(data, null);
	}

	public static String copyFromClipboard() throws UnsupportedFlavorException, IOException {
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transferable = cb.getContents(null);

		if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
			return data;
		}
		System.out.println("Couldn't get data from the clipboard");
		return null;
	}
}
