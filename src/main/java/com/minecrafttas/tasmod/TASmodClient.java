package com.minecrafttas.tasmod;

import static com.minecrafttas.tasmod.TASmod.LOGGER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.logging.log4j.Level;

import com.minecrafttas.mctcommon.Configuration;
import com.minecrafttas.mctcommon.ConfigurationRegistry;
import com.minecrafttas.mctcommon.KeybindManager;
import com.minecrafttas.mctcommon.LanguageManager;
import com.minecrafttas.mctcommon.events.EventClient.EventClientInit;
import com.minecrafttas.mctcommon.events.EventClient.EventOpenGui;
import com.minecrafttas.mctcommon.events.EventClient.EventPlayerJoinedClientSide;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.file.AbstractDataFile;
import com.minecrafttas.mctcommon.networking.Client;
import com.minecrafttas.mctcommon.networking.PacketHandlerRegistry;
import com.minecrafttas.mctcommon.networking.Server;
import com.minecrafttas.tasmod.gui.InfoHud;
import com.minecrafttas.tasmod.handlers.LoadingScreenHandler;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient;
import com.minecrafttas.tasmod.playback.PlaybackControllerClient.TASstate;
import com.minecrafttas.tasmod.playback.filecommands.integrated.DesyncMonitorFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.integrated.LabelFileCommandExtension;
import com.minecrafttas.tasmod.playback.filecommands.integrated.OptionsFileCommandExtension;
import com.minecrafttas.tasmod.playback.metadata.integrated.CreditsMetadataExtension;
import com.minecrafttas.tasmod.playback.metadata.integrated.StartpositionMetadataExtension;
import com.minecrafttas.tasmod.playback.tasfile.flavor.integrated.Beta1Flavor;
import com.minecrafttas.tasmod.registries.TASmodAPIRegistry;
import com.minecrafttas.tasmod.registries.TASmodConfig;
import com.minecrafttas.tasmod.registries.TASmodKeybinds;
import com.minecrafttas.tasmod.registries.TASmodPackets;
import com.minecrafttas.tasmod.savestates.SavestateHandlerClient;
import com.minecrafttas.tasmod.savestates.handlers.SavestatePlayerHandler;
import com.minecrafttas.tasmod.tickratechanger.TickrateChangerClient;
import com.minecrafttas.tasmod.ticksync.TickSyncClient;
import com.minecrafttas.tasmod.util.LoggerMarkers;
import com.minecrafttas.tasmod.util.Scheduler;
import com.minecrafttas.tasmod.util.ShieldDownloader;
import com.minecrafttas.tasmod.virtual.VirtualInput;
import com.minecrafttas.tasmod.virtual.VirtualKeybindings;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;

public class TASmodClient implements ClientModInitializer, EventClientInit, EventPlayerJoinedClientSide, EventOpenGui {

	public static VirtualInput virtual;

	public static TickSyncClient ticksyncClient;

	public final static Path tasfiledirectory = Minecraft.getMinecraft().mcDataDir.toPath().resolve("saves").resolve("tasfiles");

	public final static Path savestatedirectory = Minecraft.getMinecraft().mcDataDir.toPath().resolve("saves").resolve("savestates");

	public static InfoHud hud;

	public static ShieldDownloader shieldDownloader;

	public static TickrateChangerClient tickratechanger = new TickrateChangerClient();

	public static Scheduler gameLoopSchedulerClient = new Scheduler();

	public static Scheduler tickSchedulerClient = new Scheduler();

	public static Scheduler openMainMenuScheduler = new Scheduler();

	public static Configuration config;

	public static LoadingScreenHandler loadingScreenHandler;

	public static KeybindManager keybindManager;

	public static SavestateHandlerClient savestateHandlerClient = new SavestateHandlerClient();

	public static Client client;

	public static CreditsMetadataExtension creditsMetadataExtension = new CreditsMetadataExtension();

	public static StartpositionMetadataExtension startpositionMetadataExtension = new StartpositionMetadataExtension();
	/**
	 * The container where all inputs get stored during recording or stored and
	 * ready to be played back
	 */
	public static PlaybackControllerClient controller = new PlaybackControllerClient();

	public static void createTASfileDir() {
		try {
			AbstractDataFile.createDirectory(tasfiledirectory);
		} catch (IOException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	public static void createSavestatesDir() {
		try {
			AbstractDataFile.createDirectory(savestatedirectory);
		} catch (IOException e) {
			TASmod.LOGGER.catching(e);
		}
	}

	@Override
	public void onInitializeClient() {

		LanguageManager.registerMod("tasmod");

		createFolders();

		registerConfigValues();

		loadConfig(Minecraft.getMinecraft());

		virtual = new VirtualInput(LOGGER);

		// Initialize InfoHud
		hud = new InfoHud();
		// Initialize shield downloader
		shieldDownloader = new ShieldDownloader();
		// Initialize loading screen handler
		loadingScreenHandler = new LoadingScreenHandler();
		// Initialize Ticksync
		ticksyncClient = new TickSyncClient();
		// Initialize keybind manager
		keybindManager = new KeybindManager(VirtualKeybindings::isKeyDownExceptTextfield);

		// Create them here so they are created after the folders have been created, since they depend on the tasfiles folder
		desyncMonitorFileCommandExtension = new DesyncMonitorFileCommandExtension();
		optionsFileCommandExtension = new OptionsFileCommandExtension();
		labelFileCommandExtension = new LabelFileCommandExtension();

		registerEventListeners();

		registerNetworkPacketHandlers();

		// Starting local server instance
		try {
			TASmod.server = new Server(TASmod.networkingport - 1, TASmodPackets.values());
		} catch (Exception e) {
			LOGGER.error("Unable to launch TASmod server: {}", e.getMessage());
		}

	}

	private void createFolders() {
		createTASfileDir();
		createSavestatesDir();
	}

	private void registerNetworkPacketHandlers() {
		// Register packet handlers
		LOGGER.info(LoggerMarkers.Networking, "Registering network handlers on client");
		PacketHandlerRegistry.register(controller);
		PacketHandlerRegistry.register(ticksyncClient);
		PacketHandlerRegistry.register(tickratechanger);
		PacketHandlerRegistry.register(savestateHandlerClient);
		PacketHandlerRegistry.register(new SavestatePlayerHandler(null));
	}

	private void registerEventListeners() {
		EventListenerRegistry.register(this);
		EventListenerRegistry.register(hud);
		EventListenerRegistry.register(shieldDownloader);
		EventListenerRegistry.register(loadingScreenHandler);
		EventListenerRegistry.register(ticksyncClient);
		EventListenerRegistry.register(keybindManager);
		EventListenerRegistry.register((EventOpenGui) (gui -> {
			if (gui instanceof GuiMainMenu) {
				openMainMenuScheduler.runAllTasks();
			}
			return gui;
		}));
		EventListenerRegistry.register(controller);
		EventListenerRegistry.register(creditsMetadataExtension);
		EventListenerRegistry.register(startpositionMetadataExtension);

		EventListenerRegistry.register(desyncMonitorFileCommandExtension);

		EventListenerRegistry.register(TASmodAPIRegistry.PLAYBACK_METADATA);
		EventListenerRegistry.register(TASmodAPIRegistry.PLAYBACK_FILE_COMMAND);
		EventListenerRegistry.register(new LoggerMarkers());
		EventListenerRegistry.register(savestateHandlerClient);
	}

	@Override
	public void onClientInit(Minecraft mc) {
		registerKeybindings(mc);
		registerPlaybackMetadata(mc);
		registerSerialiserFlavors(mc);
		registerFileCommands();
	}

	boolean waszero;

	boolean isLoading;

	@Override
	public void onPlayerJoinedClientSide(EntityPlayerSP player) {
		Minecraft mc = Minecraft.getMinecraft();
		ServerData data = mc.getCurrentServerData();
		MinecraftServer server = TASmod.getServerInstance();

		String ip = null;
		int port;
		boolean local;
		if (server != null) {
			ip = "localhost";
			port = TASmod.networkingport - 1;
			local = true;
		} else {
			ip = data.serverIP.split(":")[0];
			port = TASmod.networkingport;
			local = false;
		}

		String connectedIP = null;
		try {
			connectedIP = client.getRemote();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!(ip + ":" + port).equals(connectedIP)) { // TODO Clean this up. Make TASmodNetworkHandler out of this... Maybe with Permission system?
			try {
				LOGGER.info("Closing client connection: {}", client.getRemote());
				client.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
			final String IP = ip;
			final int PORT = port;
			gameLoopSchedulerClient.add(() -> {
				try {
					// connect to server and authenticate
					client = new Client(IP, PORT, TASmodPackets.values(), mc.getSession().getUsername(), local);
				} catch (Exception e) {
					LOGGER.error("Unable to connect TASmod client: {}", e.getMessage());
					e.printStackTrace();
				}
				ticksyncClient.setEnabled(true);
			});
		}
	}

	@Override
	public GuiScreen onOpenGui(GuiScreen gui) {
		if (gui instanceof GuiMainMenu) {
			initializeCustomPacketHandler();
		} else if (gui instanceof GuiControls) {
			TASmodClient.controller.setTASState(TASstate.NONE); // Set the TASState to nothing to avoid collisions
			if (TASmodClient.tickratechanger.ticksPerSecond == 0) {
				TASmodClient.tickratechanger.pauseClientGame(false); // Unpause the game
				waszero = true;
			}
		} else if (!(gui instanceof GuiControls)) {
			if (waszero) {
				waszero = false;
				TASmodClient.tickratechanger.pauseClientGame(true);
			}
		}
		return gui;
	}

	private void initializeCustomPacketHandler() {
		if (client == null) {
			Minecraft mc = Minecraft.getMinecraft();

			String IP = "localhost";
			int PORT = TASmod.networkingport - 1;

			// Get the connection on startup from config
			String configAddress = config.get(TASmodConfig.ServerConnection);
			if (configAddress != null && !configAddress.isEmpty()) {
				String[] ipSplit = configAddress.split(":");
				IP = ipSplit[0];
				try {
					PORT = Integer.parseInt(ipSplit[1]);
				} catch (Exception e) {
					LOGGER.catching(Level.ERROR, e);
					IP = "localhost";
					PORT = TASmod.networkingport - 1;
				}
			}

			try {
				// connect to server and authenticate
				client = new Client(IP, PORT, TASmodPackets.values(), mc.getSession().getUsername(), true);
			} catch (Exception e) {
				LOGGER.error("Unable to connect TASmod client: {}", e);
			}
			ticksyncClient.setEnabled(true);
		}
	}

	private void registerKeybindings(Minecraft mc) {
		Arrays.stream(TASmodKeybinds.valuesKeybind()).forEach(keybindManager::registerKeybind);
		Arrays.stream(TASmodKeybinds.valuesVanillaKeybind()).forEach(VirtualKeybindings::registerBlockedKeyBinding);
	}

	private void registerPlaybackMetadata(Minecraft mc) {
		TASmodAPIRegistry.PLAYBACK_METADATA.register(creditsMetadataExtension);
		TASmodAPIRegistry.PLAYBACK_METADATA.register(startpositionMetadataExtension);
	}

	public static Beta1Flavor betaFlavor = new Beta1Flavor();

	private void registerSerialiserFlavors(Minecraft mc) {
		TASmodAPIRegistry.SERIALISER_FLAVOR.register(betaFlavor);
	}

	public static DesyncMonitorFileCommandExtension desyncMonitorFileCommandExtension;
	public static OptionsFileCommandExtension optionsFileCommandExtension;
	public static LabelFileCommandExtension labelFileCommandExtension;

	private void registerFileCommands() {
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.register(desyncMonitorFileCommandExtension);
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.register(optionsFileCommandExtension);
		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.register(labelFileCommandExtension);

		TASmodAPIRegistry.PLAYBACK_FILE_COMMAND.setConfig(config);
	}

	private static final ConfigurationRegistry CONFIG_REGISTRY = new ConfigurationRegistry();

	private void registerConfigValues() {
		CONFIG_REGISTRY.register(TASmodConfig.values());
	}

	private void loadConfig(Minecraft mc) {
		Path configDir = mc.mcDataDir.toPath().resolve("config");
		if (!Files.exists(configDir)) {
			try {
				Files.createDirectory(configDir);
			} catch (IOException e) {
				LOGGER.catching(e);
			}
		}
		config = new Configuration("TASmod configuration", configDir.resolve("tasmod.cfg"), CONFIG_REGISTRY);
		config.loadFromXML();
		config.saveToXML();
	}
}
