package tasmod.killtherng;

import com.minecrafttas.tasmod.ktrng.RandomBase;

public class RNGTest {

	public static void main(String[] args) {
		long[] longlist = new long[] {
				//@formatter:off
				113621727298730L,
				161845404674820L
				//@formatter:on
		};

		printDistance(longlist);

		longlist = new long[] {
				//@formatter:off
				161845404674820L,
				107865211428631L,
				252587169991970L,
				223075662074294L,
				66246869218509L,
				81210955942352L,
				183553865074233L,
				11371681017552L
				//@formatter:on
		};
//		printDistance(longlist);
	}

	private static void printDistance(long[] list) {
		long prev = list[0];
		for (long l : list) {
			System.out.println(RandomBase.distance(prev, l));
		}
		System.out.println("");
	}
}
