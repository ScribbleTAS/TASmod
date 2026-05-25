package tasmod.util;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.util.SortedList;

class SortedListTest {

	@Test
	void testSorting() {
		SortedList<Integer> actual = new SortedList<>((number, number2) -> {
			return number.compareTo(number2);
		});

		List<Integer> expected = new LinkedList<>();
		expected.add(1);
		expected.add(2);
		expected.add(3);
		expected.add(4);
		expected.add(5);
		expected.add(6);
		expected.add(7);
		expected.add(8);
		expected.add(9);
		expected.add(10);

		actual.add(5);
		actual.add(4);
		actual.add(10);
		actual.add(6);
		actual.add(1);
		actual.add(8);
		actual.add(2);
		actual.add(7);
		actual.add(3);
		actual.add(9);

		assertIterableEquals(expected, actual);
	}
}
