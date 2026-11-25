package com.minecrafttas.tasmod.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.minecrafttas.mctcommon.test.Test;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
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
		Vec3d actual = getPlayerPosition();
		assertEquals(expected, actual, "Position is not the start position");
	}

	@Test("Tests basic keyboard movement")
	public void testKeyboardMovement() {
		runInputs("testKeyboardMovement");
//		copyPlayerPosToClipboard();
		Vec3d expected = new Vec3d(8.357112068821067, 4.0, 7.724468414351504);
		Vec3d actual = getPlayerPosition();
		assertEquals(expected, actual, "Position of the player differs");
	}

	@Test("Tests basic mouse functions, like pickblocking and placing blocks")
	public void testMouseButtons() {
		runInputs("testMouseButtons1");
		IBlockState blockstate = getBlockAt(5, 4, 7);
		Block expected = Blocks.OBSIDIAN;
		Block actual = blockstate.getBlock();
		assertEquals(expected, actual, "Placed blocks differs");

		runInputs("testMouseButtons2");
		IBlockState blockstate2 = getBlockAt(5, 4, 7);
		Block expected2 = Blocks.AIR;
		Block actual2 = blockstate2.getBlock();
		assertEquals(expected2, actual2, "Block was not removed");
	}

	@Override
	public void afterAll() {
		quitWorld();
	}
}
