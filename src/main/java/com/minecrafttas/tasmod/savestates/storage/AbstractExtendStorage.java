package com.minecrafttas.tasmod.savestates.storage;

import org.apache.logging.log4j.Logger;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventSavestate.EventServerLoadstate;
import com.minecrafttas.tasmod.events.EventSavestate.EventServerSavestate;

public abstract class AbstractExtendStorage implements EventServerSavestate, EventServerLoadstate {
	protected Logger logger = TASmod.LOGGER;
}
