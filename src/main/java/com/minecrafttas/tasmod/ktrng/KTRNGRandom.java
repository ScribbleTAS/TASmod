package com.minecrafttas.tasmod.ktrng;

import java.util.Random;

import kaptainwutax.seedutils.lcg.LCG;
import kaptainwutax.seedutils.rand.JRand;

public class KTRNGRandom extends Random {

	private String name;
	private String description;

	private long timesCalled = 0;

	private boolean enabled;
	private boolean client;

	public KTRNGRandom(String name, String description, boolean enabled, boolean client) {
		super(0L);
		this.name = name;
		this.description = description;
		this.enabled = enabled;
		this.client = client;
	}

	public void setSeed(long seedIn) {
		timesCalled = 0;
		super.setSeed(seedIn ^ 0x5deece66dL);
	}

	public void setSeed(long seedIn, boolean shouldIncrease) {
		timesCalled++;
		super.setSeed(seedIn ^ 0x5deece66dL);
	}

	public long getSeed() {
		long saved = timesCalled;
		long seed = reverse(super.nextLong()) ^ 0x5deece66dL;
		super.setSeed(seed);
		timesCalled = saved;
		return seed ^ 0x5deece66dL;
	}

	public static long reverse(long in) {
		return (((7847617 * ((24667315 * (in >>> 32) + 18218081 * (in & 0xffffffffL) + 67552711) >> 32) - 18218081 * ((-4824621 * (in >>> 32) + 7847617 * (in & 0xffffffffL) + 7847617) >> 32)) - 11) * 246154705703781L) & 0xffffffffffffL;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return description;
	}

	public long getTimesCalled() {
		return timesCalled;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isClient() {
		return client;
	}

	@Override
	public long nextLong() {
		timesCalled++;
		long seedstored = getSeed();
		long value = super.nextLong();
		fireEvent(seedstored, Long.toString(value));
		return value;
	}

	@Override
	public double nextDouble() {
		timesCalled++;
		long seedstored = getSeed();
		double value = super.nextDouble();
		fireEvent(seedstored, Double.toString(value));
		return value;
	}

	@Override
	public boolean nextBoolean() {
		timesCalled++;
		long seedstored = getSeed();
		boolean value = super.nextBoolean();
		fireEvent(seedstored, Boolean.toString(value));
		return value;
	}

	@Override
	public int nextInt() {
		timesCalled++;
		long seedstored = getSeed();
		int value = super.nextInt();
		fireEvent(seedstored, Integer.toString(value));
		return value;
	}

	@Override
	public int nextInt(int bound) {
		timesCalled++;
		long seedstored = getSeed();
		int value = super.nextInt(bound);
		fireEvent(seedstored, Integer.toString(value));
		return value;
	}

	@Override
	public float nextFloat() {
		return super.nextFloat();
	}

	@Override
	public void nextBytes(byte[] bytes) {
		super.nextBytes(bytes);
	}

	@Override
	public double nextGaussian() {
		timesCalled++;
		double value = 0;
		value = super.nextGaussian();
		return value;
	}

	public void advance() {
		advance(1);
	}

	public void advance(long i) {
		JRand thing = JRand.ofInternalSeed(getSeed());
		thing.advance(i);
		setSeed(thing.getSeed());
	}

	public long getSeedAt(int steps) {
		JRand thing = new JRand(getSeed()).combine(steps);
		return thing.getSeed();
	}

	public long distance(KTRNGRandom random) {
		return KTRNGRandom.distance(this.getSeed(), random.getSeed());
	}

	public long distance(long seed) {
		return KTRNGRandom.distance(this.getSeed(), seed);
	}

	public static long distance(KTRNGRandom random1, KTRNGRandom random2) {
		return KTRNGRandom.distance(random1.getSeed(), random2.getSeed());
	}

	public static long distance(long seed, long seed2) {
		return LCG.JAVA.distance(seed, seed2);
	}

	@Override
	public String toString() {
		return name + ": " + enabled;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof KTRNGRandom) {
			KTRNGRandom custom = (KTRNGRandom) obj;
			return custom.name.equals(name);
		} else {
			return super.equals(obj);
		}
	}

	public void fireEvent(long seed, String value) {
		// TODO Implement
	}
}
