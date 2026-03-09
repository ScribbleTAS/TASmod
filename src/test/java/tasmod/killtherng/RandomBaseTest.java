package tasmod.killtherng;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.ktrng.RandomBase;

import kaptainwutax.seedutils.rand.JRand;

class TestRNGTest {

	class TestRNG extends RandomBase {

		public TestRNG() {
			super();
		}

		public TestRNG(long seed) {
			super(seed);
		}

		@Override
		public String getExtensionName() {
			return "TestRNG";
		}

	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void testInitialSeed() {
		long expected = 12345L;
		TestRNG testRandom = new TestRNG(expected);
		long actual = testRandom.getSeed();
		assertEquals(12345L, actual);
	}

	@Test
	void testSetSeed() {
		TestRNG testRandom = new TestRNG(1L);
		testRandom.setSeed(12345L);
		assertEquals(12345L, testRandom.getSeed());
	}

	@Test
	void testAdvance() {
		JRand thing = JRand.ofInternalSeed(12345L);
		TestRNG testRandom = new TestRNG(12345L);

		testRandom.advance();
		thing.advance(1);

		assertEquals(thing.getSeed(), testRandom.getSeed());
	}

	@Test
	void testAdvancingLong() {
		JRand thing = JRand.ofInternalSeed(12345L);
		TestRNG testRandom = new TestRNG(12345L);

		long expectedValue = thing.nextLong();
		long actualValue = testRandom.nextLong();

		assertEquals(expectedValue, actualValue);
		assertEquals(thing.getSeed(), testRandom.getSeed());
	}

	@Test
	void testAdvancingLong2() {
		JRand thing = JRand.ofInternalSeed(12345L);
		TestRNG testRandom = new TestRNG(12345L);

		testRandom.nextLong();
		thing.advance(2);

		assertEquals(thing.getSeed(), testRandom.getSeed());
	}

	@Test
	void testAdvancingDifferentRNGs() {
		TestRNG testRandom1 = new TestRNG(12345L);
		TestRNG testRandom2 = new TestRNG(12345L);

		testRandom1.nextInt();
		testRandom2.nextInt(6);

		assertEquals(testRandom1.getSeed(), testRandom2.getSeed());
	}
}
