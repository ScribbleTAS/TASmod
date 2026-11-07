package com.minecrafttas.tasmod.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.minecrafttas.mctcommon.test.Test;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;

public class TestMotion extends TASmodTestBase {

	@Override
	public void beforeAll() {
		launchWorld("Void");
	}

	@Override
	public void beforeEach() {
		teleportToStart();
	}

	@Test("Tests joining the world and not moving")
	public void testNoMove() {
		Vec3d expected = new Vec3d(7.065021719547429, 4.0, 7.939614959472749);
		Minecraft mc = Minecraft.getMinecraft();
		Vec3d actual = mc.player.getPositionVector();
		assertEquals(expected, actual);
	}

	@Test("Used for testing basic keyboard movement")
	public void testMovement() {
		runInputs("testMovement1");
		Vec3d expected = new Vec3d(6.427050596119601, 4.0, 8.584996000161354);
		Minecraft mc = Minecraft.getMinecraft();
		Vec3d actual = mc.player.getPositionVector();
		assertEquals(expected, actual);
	}

	@Override
	public void afterAll() {
		quitWorld();
	}
}
