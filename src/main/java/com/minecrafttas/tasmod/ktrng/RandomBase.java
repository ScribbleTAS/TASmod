package com.minecrafttas.tasmod.ktrng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.util.FileThread;

import kaptainwutax.seedutils.lcg.LCG;
import kaptainwutax.seedutils.rand.JRand;

public class RandomBase extends Random {

	private String name;
	private String description;

	private long initialSeed;
	public static FileThread writerThread;

	private JRand.Debugger jrand;

	public RandomBase(long seed) {
		super(seed);
		this.initialSeed = seed;
		jrand = new JRand(seed, false).asDebugger();
	}

	@Override
	public void setSeed(long seedIn) {
		super.setSeed(seedIn);
		if (jrand != null)
			jrand.setSeed(seedIn, false);
//		super.setSeed(seedIn ^ 0x5deece66dL);
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
//		timesCalled++;
		long seedstored = getSeed();
		long value = jrand.nextLong();
		fireGetEvent("nextLong()", seedstored, Long.toString(value));
		return value;
	}

	@Override
	public double nextDouble() {
//		timesCalled++;
		long seedstored = getSeed();
		double value = jrand.nextDouble();
		fireGetEvent("nextDouble()", seedstored, Double.toString(value));
		return value;
	}

	@Override
	public boolean nextBoolean() {
//		timesCalled++;
		long seedstored = getSeed();
		boolean value = jrand.nextBoolean();
		fireGetEvent("nextBoolean()", seedstored, Boolean.toString(value));
		return value;
	}

	@Override
	public int nextInt() {
//		timesCalled++;
		long seedstored = getSeed();
		int value = jrand.nextInt();
		fireGetEvent("nextInt()", seedstored, Integer.toString(value));
		return value;
	}

	@Override
	public int nextInt(int bound) {
//		timesCalled++;
		long seedstored = getSeed();
		int value = jrand.nextInt(bound);
		fireGetEvent(String.format("nextInt(%s)", bound), seedstored, Integer.toString(value));
		return value;
	}

	@Override
	public float nextFloat() {
		return jrand.nextFloat();
	}

	@Override
	public double nextGaussian() {
		double value = 0;
		value = jrand.nextGaussian();
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RandomBase) {
			RandomBase custom = (RandomBase) obj;
			return custom.name.equals(name);
		} else {
			return super.equals(obj);
		}
	}

	public void fireSetEvent(String eventType, long seed, String value, int stackTraceOffset) {
//		fireEvent(eventType, seed, value, stackTraceOffset);
	}

	public void fireGetEvent(String eventType, long seed, String value) {
//		fireEvent(eventType, seed, value, 3);
	}

	public void fireEvent(String eventType, long seed, String value, int stackTraceOffset) {
		if (!TASmod.debugRand.isActive()) {
			return;
		}

		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		List<String> classOut = new ArrayList<>();
		for (int i = stackTraceOffset; i < stackTraceOffset + 4; i++) {
			String out = formatStackTraceElement(stackTraceElements[i]);
			if (out != null)
				classOut.add(out);
		}
		String out = String.format("%s %s %s\t%s\t%s", eventType, seed, value, this.getClass().getSimpleName(), String.join(", ", classOut));
		TASmod.debugRand.writeDebug(out);
	}

	private String formatStackTraceElement(StackTraceElement stackTraceElement) {
		String methodName = stackTraceElement.getMethodName();
		String[] classNames = stackTraceElement.getClassName().split("\\.");
		String className = classNames[classNames.length - 1];
		if (methodName.equals("showBarrierParticles"))
			return null;
		String classOut = className + "." + methodName +
				(stackTraceElement.isNativeMethod() ? "(Native Method)" : (stackTraceElement.getFileName() != null && stackTraceElement.getLineNumber() >= 0 ? "(" + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber()
						+ ")" : (stackTraceElement.getFileName() != null ? "(" + stackTraceElement.getFileName() + ")" : "(Unknown Source)")));
		return classOut;
	}

	public long getInitialSeed() {
		return initialSeed;
	}
}
