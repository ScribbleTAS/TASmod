package com.minecrafttas.tasmod.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.minecrafttas.mctcommon.test.Test;

import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class TestSavestates extends TASmodTestBase {

	@Override
	public void beforeAll() {
		launchWorld("Void");
	}

	@Override
	public void beforeEach() {
		teleportToStart();
	}

	@Test("Tests saving and loading a savestate")
	public void testSavestates() {
		runInputs("testSavestates");
//		copyPlayerPosAndAngleToClipBoard();
		Vec3d expectedPos = new Vec3d(-0.4192540547104504, 4.0, 7.9512846945622755);
		Vec3d actualPos = getPlayerPosition();
		assertEquals(expectedPos, actualPos, "The position is different");
		Vec2f expectedAngle = new Vec2f(23.249935f, -270.30014f);
		Vec2f actualAngle = getPlayerAngle();
		assertEquals(expectedAngle, actualAngle, "The camera angle is different");
	}

	@Override
	public void afterAll() {
		quitWorld();
	}
}
