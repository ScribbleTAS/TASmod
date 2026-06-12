package com.minecrafttas.tasmod.ktrng;

import java.util.Random;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.registry.Registerable;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.events.EventKillTheRNGServer;

import kaptainwutax.seedutils.lcg.LCG;
import kaptainwutax.seedutils.rand.JRand;

/**
 * Base RNG class extending the {@link Random} class, but designed to be easily modifyable and monitorable.
 * 
 * @author Scribble
 */
public abstract class RandomBase extends Random implements Registerable {

	protected RNGSide side;
	protected long initialSeed;
	protected JRand jrand;

	public RandomBase() {
		super(TASmod.globalRandomness.getCurrentSeed());
		initialSeed = TASmod.globalRandomness.getCurrentSeed();
		jrand = new JRand(initialSeed, false);
	}

	public RandomBase(long seed) {
		super(seed);
		this.initialSeed = seed;
		jrand = new JRand(seed, false);
	}

	@Override
	public void setSeed(long seedIn) {
		super.setSeed(seedIn);
		if (jrand != null) {
			jrand.setSeed(seedIn, false);
		}
	}

	public long getSeed() {
//		long saved = timesCalled;
//		long seed = reverse(super.nextLong()) ^ 0x5deece66dL;
//		super.setSeed(seed);
//		timesCalled = saved;
//		return seed ^ 0x5deece66dL;
		return jrand.getSeed();
	}

//	public static long reverse(long in) {
//		return (((7847617 * ((24667315 * (in >>> 32) + 18218081 * (in & 0xffffffffL) + 67552711) >> 32) - 18218081 * ((-4824621 * (in >>> 32) + 7847617 * (in & 0xffffffffL) + 7847617) >> 32)) - 11) * 246154705703781L) & 0xffffffffffffL;
//	}

//	public String getName() {
//		return this.name;
//	}
//
//	public String getDescription() {
//		return description;
//	}

//	public long getTimesCalled() {
//		return timesCalled;
//	}

	@Override
	public long nextLong() {
		long seedstored = getSeed();
		long value = jrand.nextLong();
		fireGetEvent("nextLong()", seedstored, Long.toString(value));
		return value;
	}

	@Override
	public double nextDouble() {
		long seedstored = getSeed();
		double value = jrand.nextDouble();
		fireGetEvent("nextDouble()", seedstored, Double.toString(value));
		return value;
	}

	@Override
	public boolean nextBoolean() {
		long seedstored = getSeed();
		boolean value = jrand.nextBoolean();
		fireGetEvent("nextBoolean()", seedstored, Boolean.toString(value));
		return value;
	}

	@Override
	public int nextInt() {
		long seedstored = getSeed();
		int value = jrand.nextInt();
		fireGetEvent("nextInt()", seedstored, Integer.toString(value));
		return value;
	}

	@Override
	public int nextInt(int bound) {
		long seedstored = getSeed();
		int value = jrand.nextInt(bound);
		fireGetEvent(String.format("nextInt(%s)", bound), seedstored, Integer.toString(value));
		return value;
	}

	@Override
	public float nextFloat() {
		long seedstored = getSeed();
		float value = jrand.nextFloat();
		fireGetEvent("nextFloat", seedstored, Float.toString(value));
		return value;
	}

	@Override
	public double nextGaussian() {
		long seedstored = getSeed();
		double value = jrand.nextGaussian();
		fireGetEvent("nextGaussian", seedstored, Double.toString(value));
		return value;
	}

	public void advance() {
		advance(1);
	}

	public void advance(long i) {
		jrand.advance(i);
	}

	public long distance(RandomBase random) {
		return RandomBase.distance(this.getSeed(), random.getSeed());
	}

	public long distance(long seed) {
		return RandomBase.distance(this.getSeed(), seed);
	}

	public static long distance(RandomBase random1, RandomBase random2) {
		return RandomBase.distance(random1.getSeed(), random2.getSeed());
	}

	public static long distance(long seed, long seed2) {
		return LCG.JAVA.distance(seed, seed2);
	}

	@Override
	public String toString() {
		return Long.toString(getSeed());
	}

	public void fireSetEvent(String eventType, long seed, String value) {
//		fireRNGEvent(eventType, seed, value, stackTraceOffset);
	}

	public void fireGetEvent(String eventType, long seed, String value) {
		fireRNGEvent(eventType, seed, value, 10);
	}

	public long getInitialSeed() {
		return initialSeed;
	}

	public void fireRNGEvent(String eventType, long seed, String value, int stackTraceOffset) {
		EventListenerRegistry.fireEvent(EventKillTheRNGServer.EventRNG.class, side, eventType, seed, value, this.getClass().getSimpleName(), stackTraceOffset);
	}

	public enum RNGSide {
		Server,
		Client
	}
}
