package com.minecrafttas.tasmod.playback.filecommands.integrated;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import com.dselent.bigarraylist.BigArrayList;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.TASmodClient;
import com.minecrafttas.tasmod.events.EventPlaybackClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TickContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandContainer;
import com.minecrafttas.tasmod.playback.filecommands.PlaybackFileCommand.PlaybackFileCommandExtension;
import com.minecrafttas.tasmod.playback.tasfile.exception.PlaybackLoadException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextFormatting;

/**
 * Stores the players position during recording and compares it with the
 * position during playback
 * 
 * @author Scribble
 */
public class DesyncMonitorFileCommandExtension extends PlaybackFileCommandExtension implements EventPlaybackClient.EventControllerStateChange {

	private File tempDir = new File(Minecraft.getMinecraft().mcDataDir.getAbsolutePath() + File.separator + "saves" + File.separator + "tasfiles" + File.separator + "temp" + File.separator + "monitoring");

	private BigArrayList<MonitorContainer> monitorContainer = new BigArrayList<MonitorContainer>(tempDir.toString());

	private MonitorContainer currentValues;

	public DesyncMonitorFileCommandExtension() {
		enabled = true;
	}

	@Override
	public String name() {
		return "tasmod_desyncMonitoring@v1";
	}

	@Override
	public String[] getFileCommandNames() {
		return new String[] { "desyncMonitoring" };
	}

	@Override
	public void onControllerStateChange(TASstate newstate, TASstate oldstate) {
		if(newstate==TASstate.RECORDING && monitorContainer.isEmpty()) {
			recordNull(0);
		}
	}

	@Override
	public void onRecord(long tick, TickContainer tickContainer) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		MonitorContainer values = null;
		if (player != null) {
			values = new MonitorContainer(tick, player.posX, player.posY, player.posZ, player.motionX, player.motionY, player.motionZ);
		} else {
			values = new MonitorContainer(tick);
		}

		if (monitorContainer.size() <= tick) {
			monitorContainer.add(values);
		} else {
			monitorContainer.set(tick, values);
		}
	}

	@Override
	public void onDisable() {
		this.onClear();
	}

	@Override
	public PlaybackFileCommandContainer onSerialiseEndlineComment(long currentTick, TickContainer tickContainer) {
		PlaybackFileCommandContainer out = new PlaybackFileCommandContainer();
		MonitorContainer monitoredValues = monitorContainer.get(currentTick);
		PlaybackFileCommand command = new PlaybackFileCommand("desyncMonitor", monitoredValues.toStringArray());

		out.add("desyncMonitor", command);

		return out;
	}

	@Override
	public void onDeserialiseEndlineComment(long tick, TickContainer container, PlaybackFileCommandContainer fileCommandContainer) {
		List<PlaybackFileCommand> commandsEndline = fileCommandContainer.get("desyncMonitor");
		if (commandsEndline == null || commandsEndline.isEmpty()) {
			recordNull(tick);
			return;
		}

		PlaybackFileCommand command = commandsEndline.get(0);
		this.monitorContainer.add(loadFromFile(tick, command.getArgs()));
	}

	public void recordNull(long tick) {
		if (monitorContainer.size() <= tick) {
			monitorContainer.add(new MonitorContainer(tick));
		} else {
			monitorContainer.set(tick, new MonitorContainer(tick));
		}
	}

	@Override
	public void onPlayback(long tick, TickContainer tickContainer) {
		currentValues = get(tick - 1);
	}

	private MonitorContainer loadFromFile(long tick, String[] args) throws PlaybackLoadException {

		if (args.length != 6)
			throw new PlaybackLoadException("Tick %s: desyncMonitorArgsLength ");

		double x = 0;
		double y = 0;
		double z = 0;
		double mx = 0;
		double my = 0;
		double mz = 0;
		try {
			x = parseDouble(args[0]);
			y = parseDouble(args[1]);
			z = parseDouble(args[2]);
			mx = parseDouble(args[3]);
			my = parseDouble(args[4]);
			mz = parseDouble(args[5]);
		} catch (ParseException e) {
			throw new PlaybackLoadException(e);
		}

		return new MonitorContainer(tick, x, y, z, mx, my, mz);
	}

	public MonitorContainer get(long l) {
		try {
			return monitorContainer.get(l);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	private String lastStatus = TextFormatting.GRAY + "Empty";

	public String getStatus(EntityPlayerSP player) {
		if (!TASmodClient.controller.isNothingPlaying()) {
			if (currentValues != null) {
				double[] playervalues = new double[6];
				playervalues[0] = player.posX;
				playervalues[1] = player.posY;
				playervalues[2] = player.posZ;
				playervalues[3] = player.motionX;
				playervalues[4] = player.motionY;
				playervalues[5] = player.motionZ;
				DesyncStatus status = currentValues.getSeverity(TASmodClient.controller.index(), playervalues, TASmod.ktrngHandler.getGlobalSeedClient());
				lastStatus = status.getFormat() + status.getText();
			} else {
				lastStatus = TextFormatting.GRAY + "Empty";
			}
		}
		return lastStatus;
	}

	private String lastPos = "";

	public String getPos() {
		if (currentValues != null && !TASmodClient.controller.isNothingPlaying()) {
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			String[] values = new String[3];
			values[0] = getFormattedString(player.posX - currentValues.values[0]);
			values[1] = getFormattedString(player.posY - currentValues.values[1]);
			values[2] = getFormattedString(player.posZ - currentValues.values[2]);

			String out = "";
			for (String val : values) {
				if (val != null) {
					out += val + " ";
				}
			}
			lastPos = out;
		}
		return lastPos;
	}

	private String lastMotion = "";

	public String getMotion() {
		if (currentValues != null && !TASmodClient.controller.isNothingPlaying()) {
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			String[] values = new String[3];
			values[0] = getFormattedString(player.motionX - currentValues.values[3]);
			values[1] = getFormattedString(player.motionY - currentValues.values[4]);
			values[2] = getFormattedString(player.motionZ - currentValues.values[5]);

			String out = "";
			for (String val : values) {
				if (val != null) {
					out += val + " ";
				}
			}
			lastMotion = out;
		}
		return lastMotion;
	}

	private String getFormattedString(double delta) {
		String out = "";
		if (delta != 0D) {
			DesyncStatus status = DesyncStatus.fromDelta(delta);
			if (status == DesyncStatus.EQUAL) {
				return "";
			}
			out = status.getFormat() + Double.toString(delta);
		}
		return out;
	}

	public class MonitorContainer implements Serializable {
		private static final long serialVersionUID = -3138791930493647885L;

		long index;

		double[] values = new double[6];

		public MonitorContainer(long index, double posx, double posy, double posz, double velx, double vely, double velz) {
			this.index = index;
			this.values[0] = posx;
			this.values[1] = posy;
			this.values[2] = posz;
			this.values[3] = velx;
			this.values[4] = vely;
			this.values[5] = velz;
		}

		public MonitorContainer(long index) {
			this(index, 0, 0, 0, 0, 0, 0);
		}

		public String[] toStringArray() {
			String[] out = new String[values.length];
			for (int i = 0; i < values.length; i++) {
				out[i] = String.format(Locale.ENGLISH, "%.5f", values[i]);
			}
			return out;
		}

		@Override
		public String toString() {
			return String.format(Locale.US, "%d, %d, %d, %d, %d, %d", values[0], values[1], values[2], values[3], values[4], values[5]);
		}

		public DesyncStatus getSeverity(long index, double[] playerValues, long seed) {

			DesyncStatus out = null;

			for (int i = 0; i < values.length; i++) {
				double delta = 0;
				try {
					delta = playerValues[i] - values[i];
				} catch (Exception e) {
					return DesyncStatus.ERROR;
				}
				DesyncStatus status = DesyncStatus.fromDelta(delta);
				if (out == null || status.getSeverity() > out.getSeverity()) {
					out = status;
				}
			}

			return out;
		}
	}

	public enum DesyncStatus {
		EQUAL(0, TextFormatting.GREEN, "In sync", 0D),
		WARNING(1, TextFormatting.YELLOW, "Slight desync", 0.00001D),
		MODERATE(2, TextFormatting.RED, "Moderate desync", 0.01D),
		TOTAL(3, TextFormatting.DARK_RED, "Total desync"),
		ERROR(3, TextFormatting.DARK_PURPLE, "ERROR");

		private Double tolerance;
		private int severity;
		private String text;
		private TextFormatting format;

		private DesyncStatus(int severity, TextFormatting color, String text) {
			this.severity = severity;
			this.format = color;
			this.text = text;
			tolerance = null;
		}

		private DesyncStatus(int severity, TextFormatting color, String text, double tolerance) {
			this(severity, color, text);
			this.tolerance = tolerance;
		}

		public static DesyncStatus fromDelta(double delta) {
			DesyncStatus out = TOTAL;
			for (DesyncStatus status : values()) {
				if (status.tolerance == null) {
					return status;
				}
				if (Math.abs(delta) < status.tolerance) {
					break;
				}
				if (Math.abs(delta) >= status.tolerance) {
					out = status;
				}
			}
			return out;
		}

		public TextFormatting getFormat() {
			return format;
		}

		public int getSeverity() {
			return severity;
		}

		public String getText() {
			return text;
		}
	}

	private double parseDouble(String doublestring) throws ParseException {
		NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
		Number number = format.parse(doublestring);
		return number.doubleValue();
	}

	@Override
	public void onClear() {
		currentValues = null;
		try {
			monitorContainer.clearMemory();
		} catch (IOException e) {
			e.printStackTrace();
		}
		monitorContainer = new BigArrayList<MonitorContainer>(tempDir.toString());
		lastStatus = TextFormatting.GRAY + "Empty";
		lastPos = "";
		lastMotion = "";
	}
}
