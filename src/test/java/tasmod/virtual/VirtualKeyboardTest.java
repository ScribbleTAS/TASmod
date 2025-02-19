package tasmod.virtual;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;
import com.minecrafttas.tasmod.virtual.event.VirtualKeyboardEvent;

class VirtualKeyboardTest {

    /**
     * Test the empty constructor
     */
    @Test
    void testEmptyConstructor(){
        VirtualKeyboard actual = new VirtualKeyboard();
        assertTrue(actual.getPressedKeys().isEmpty());
        assertTrue(actual.getCharList().isEmpty());
        assertTrue(actual.isParent());
    }

    /**
     * Test constructor with premade keycode sets
     */
    @Test
    void testSubtickConstructor(){
        Set<Integer> testKeycodeSet = new HashSet<>();
        testKeycodeSet.add(VirtualKey.W.getKeycode());
        testKeycodeSet.add(VirtualKey.S.getKeycode());

        List<Character> testCharList = new ArrayList<>();
        testCharList.add('w');
        testCharList.add('s');

        VirtualKeyboard actual = new VirtualKeyboard(testKeycodeSet, testCharList);

        assertIterableEquals(testKeycodeSet, actual.getPressedKeys());
        assertIterableEquals(testCharList, actual.getCharList());
        assertFalse(actual.isParent());
    }

    /**
     * Test setting the keycodes via setPressed to "pressed"
     */
    @Test
    void testSetPressedByKeycode(){
        VirtualKeyboard actual = new VirtualKeyboard();
        actual.setPressed(VirtualKey.W.getKeycode(), true);

        assertIterableEquals(Arrays.asList(VirtualKey.W.getKeycode()), actual.getPressedKeys());
        assertTrue(actual.isParent());
    }
    
    /**
     * Test setting the keycodes via setPressed to "pressed"
     */
    @Test
    void testFailingSetPressedByKeycode(){
        VirtualKeyboard actual = new VirtualKeyboard();
        actual.setPressed(VirtualKey.LC.getKeycode(), true);

        assertTrue(actual.getPressedKeys().isEmpty());
        assertTrue(actual.isParent());
    }

    /**
     * Test setting the keynames via setPressed to "pressed"
     */
    @Test
    void testSetPressedByKeyname(){
        VirtualKeyboard actual = new VirtualKeyboard();
        actual.setPressed("W", true);

        assertIterableEquals(Arrays.asList(VirtualKey.W.getKeycode()), actual.getPressedKeys());
        assertTrue(actual.isParent());
    }

    /**
     * Test setting the keycodes via setPressed to "unpressed"
     */
    @Test
    void testSetUnPressedByKeycode(){
        Set<Integer> testKeycodeSet = new HashSet<>();
        testKeycodeSet.add(VirtualKey.W.getKeycode());
        testKeycodeSet.add(VirtualKey.S.getKeycode());
        VirtualKeyboard actual = new VirtualKeyboard(testKeycodeSet, new ArrayList<>());
        actual.setPressed(VirtualKey.W.getKeycode(), false);

        assertIterableEquals(Arrays.asList(VirtualKey.S.getKeycode()), actual.getPressedKeys());
    }

    /**
     * Test setting the keynames via setPressed to "unpressed"
     */
    @Test
    void testSetUnPressedByKeyname(){
        Set<Integer> testKeycodeSet = new HashSet<>();
        testKeycodeSet.add(VirtualKey.W.getKeycode());
        testKeycodeSet.add(VirtualKey.S.getKeycode());
        VirtualKeyboard actual = new VirtualKeyboard(testKeycodeSet, new ArrayList<>());
        actual.setPressed("S", false);

        assertIterableEquals(Arrays.asList(VirtualKey.W.getKeycode()), actual.getPressedKeys());
    }

    /**
     * Test adding a character to the keyboard
     */
    @Test
    void testAddCharacter(){
        VirtualKeyboard actual = new VirtualKeyboard();
        actual.addChar('w', false);

        assertIterableEquals(Arrays.asList('w'), actual.getCharList());
    }

    /**
     * Test the toString method <em>without</em> subticks
     */
    @Test
    void testToString(){
        Set<Integer> testKeycodeSet = new LinkedHashSet<>();
        testKeycodeSet.add(VirtualKey.W.getKeycode());
        testKeycodeSet.add(VirtualKey.S.getKeycode());

        List<Character> testCharList = new ArrayList<>();
        testCharList.add('w');
        testCharList.add('s');

        VirtualKeyboard actual = new VirtualKeyboard(testKeycodeSet, testCharList);
        VirtualKeyboard actual2 = new VirtualKeyboard(testKeycodeSet, new ArrayList<>());

        assertEquals("W,S;ws", actual.toString());
        assertEquals("W,S;", actual2.toString());
    }

    /**
     * Test the toString method <em>with</em> subticks
     */
    @Test
    void testToStringSubticks(){
        VirtualKeyboard actual = new VirtualKeyboard();

        actual.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
        actual.updateFromEvent(VirtualKey.S.getKeycode(), true, 's');

        assertEquals("W;w\nW,S;s", actual.toString());
    }

    /**
     * Test equals method
     */
    @Test
    void testEquals() {
        Set<Integer> testKeycodeSet = new HashSet<>();
        testKeycodeSet.add(VirtualKey.W.getKeycode());
        testKeycodeSet.add(VirtualKey.S.getKeycode());

        List<Character> testCharList = new ArrayList<>();
        testCharList.add('w');
        testCharList.add('s');

        VirtualKeyboard actual = new VirtualKeyboard(testKeycodeSet, testCharList);
        VirtualKeyboard actual2 = new VirtualKeyboard(testKeycodeSet, testCharList);
        
        assertEquals(actual, actual2);
    }

    /**
     * Test where equals will fail
     */
    @Test
    void testNotEquals() {
        Set<Integer> testKeycodeSet = new HashSet<>();
        testKeycodeSet.add(VirtualKey.W.getKeycode());
        testKeycodeSet.add(VirtualKey.S.getKeycode());

        List<Character> testCharList = new ArrayList<>();
        testCharList.add('w');
        testCharList.add('s');
        
        List<Character> testCharList2 = new ArrayList<>();
        testCharList2.add('w');
        testCharList2.add('S');
        
        List<Character> testCharList3 = new ArrayList<>();
        testCharList3.add('w');

        VirtualKeyboard actual = new VirtualKeyboard(testKeycodeSet, testCharList);
        VirtualKeyboard test2 = new VirtualKeyboard(testKeycodeSet, testCharList2);
        VirtualKeyboard test3 = new VirtualKeyboard(testKeycodeSet, testCharList3);
        
        assertNotEquals(actual, test2);
        assertNotEquals(actual, test3);
        assertNotEquals(actual, null);
    }

    /**
     * Test shallow cloning the keyboard
     */
    @Test
    void testShallowClone() {
        Set<Integer> testKeycodeSet = new HashSet<>();
        testKeycodeSet.add(VirtualKey.W.getKeycode());
        testKeycodeSet.add(VirtualKey.S.getKeycode());

        List<Character> testCharList = new ArrayList<>();
        testCharList.add('w');
        testCharList.add('s');

        VirtualKeyboard expected = new VirtualKeyboard(testKeycodeSet, testCharList);
        VirtualKeyboard actual = expected.shallowClone();
        
        assertEquals(expected, actual);
    }

	/**
	 * Test deep cloning the keyboard
	 */
	@Test
	void testDeepClone() {
		VirtualKeyboard expected = new VirtualKeyboard();
		expected.updateFromEvent(VirtualKey.W, true, 'w');
		expected.updateFromEvent(VirtualKey.S, true, 's');

		VirtualKeyboard actual = expected.clone();

		assertEquals(expected, actual);
		assertIterableEquals(expected.getSubticks(), actual.getSubticks());
	}

    /**
     * Test moveFrom method
     */
    @Test
    void testMoveFrom(){
    	VirtualKeyboard moveFrom = new VirtualKeyboard();
    	VirtualKeyboard actual = new VirtualKeyboard();
    	
    	moveFrom.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
    	moveFrom.updateFromEvent(VirtualKey.A.getKeycode(), true, 'a');
    	
    	VirtualKeyboard expected = moveFrom.clone();
    	
    	actual.updateFromEvent(VirtualKey.S.getKeycode(), true, 's');
    	actual.updateFromEvent(VirtualKey.D.getKeycode(), true, 'd');

    	actual.moveFrom(null);
        actual.moveFrom(moveFrom);

        assertIterableEquals(expected.getPressedKeys(), actual.getPressedKeys());
        assertIterableEquals(expected.getCharList(), actual.getCharList());

        assertTrue(moveFrom.getSubticks().isEmpty());
        assertTrue(moveFrom.getCharList().isEmpty());
    }

    /**
     * Test copyFrom method
     */
    @Test
    void testCopyFrom() {
    	VirtualKeyboard copyFrom = new VirtualKeyboard();
    	VirtualKeyboard actual = new VirtualKeyboard();
    	
    	copyFrom.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
    	copyFrom.updateFromEvent(VirtualKey.A.getKeycode(), true, 'a');
    	
    	VirtualKeyboard expected = copyFrom.clone();
    	
    	actual.updateFromEvent(VirtualKey.S.getKeycode(), true, 's');
    	actual.updateFromEvent(VirtualKey.D.getKeycode(), true, 'd');

    	actual.copyFrom(null);
        actual.copyFrom(copyFrom);

        assertIterableEquals(expected.getPressedKeys(), actual.getPressedKeys());
        assertIterableEquals(expected.getCharList(), actual.getCharList());

        assertFalse(copyFrom.getSubticks().isEmpty());
        assertFalse(copyFrom.getCharList().isEmpty());
    }
    
    /**
     * Test subtick list being filled via update
     */
    @Test
    void testUpdate(){
        VirtualKeyboard actual = new VirtualKeyboard();
        actual.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
        actual.updateFromEvent(VirtualKey.A.getKeycode(), true, 'A');

        List<VirtualKeyboard> expected = new ArrayList<>();
        expected.add(new VirtualKeyboard(new HashSet<Integer>(Arrays.asList(VirtualKey.W.getKeycode())), Arrays.asList('w')));
        expected.add(new VirtualKeyboard(new HashSet<Integer>(Arrays.asList(VirtualKey.W.getKeycode(), VirtualKey.A.getKeycode())), Arrays.asList('A')));

        assertIterableEquals(expected, actual.getAll());
    }
    
    /**
     * Tests update method on a subtick. Should not add a subtick
     */
    @Test
    void testUpdateOnSubtick() {
    	VirtualKeyboard actual = new VirtualKeyboard(new LinkedHashSet<>(), new ArrayList<>(), null, false);
    	
    	actual.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
    }

    /**
     * Tests getDifference
     */
    @Test
    void testGetDifference(){
        VirtualKeyboard test = new VirtualKeyboard(new HashSet<>(Arrays.asList(VirtualKey.W.getKeycode())), Arrays.asList('w'));
        VirtualKeyboard test2 = new VirtualKeyboard(new HashSet<>(Arrays.asList(VirtualKey.W.getKeycode(), VirtualKey.S.getKeycode())), Arrays.asList('S'));
        Queue<VirtualKeyboardEvent> actual = new ConcurrentLinkedQueue<>();
        test.getDifference(test2, actual);
        Queue<VirtualKeyboardEvent> expected = new ConcurrentLinkedQueue<>(Arrays.asList(new VirtualKeyboardEvent(VirtualKey.S.getKeycode(), true, 'S')));

        assertIterableEquals(expected, actual);
    }
    
    /**
     * Tests generating virtual events going from an unpressed keyboard to a pressed keyboard state
     */
    @Test
    void testGetVirtualEventsPress() {
    	VirtualKeyboard unpressed = new VirtualKeyboard();
    	
    	VirtualKeyboard pressed = new VirtualKeyboard();
    	pressed.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
    	
    	// Load actual with the events
    	Queue<VirtualKeyboardEvent> actual = new ConcurrentLinkedQueue<>();
    	unpressed.getVirtualEvents(pressed, actual);
    	
    	// Load expected
    	List<VirtualKeyboardEvent> expected = Arrays.asList(new VirtualKeyboardEvent(VirtualKey.W.getKeycode(), true, 'w'));
    	
    	assertIterableEquals(expected, actual);
    }
    
    /**
     * Tests generating virtual events going from a pressed keyboard to an unpressed keyboard state
     */
    @Test
    void testGetVirtualEventsUnpress() {
    	VirtualKeyboard unpressed = new VirtualKeyboard();
    	
    	VirtualKeyboard pressed = new VirtualKeyboard();
    	pressed.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
    	
    	// Load actual with the events
    	Queue<VirtualKeyboardEvent> actual = new ConcurrentLinkedQueue<>();
    	pressed.getVirtualEvents(unpressed, actual);
    	
    	// Load expected
    	List<VirtualKeyboardEvent> expected = Arrays.asList(new VirtualKeyboardEvent(VirtualKey.W.getKeycode(), false, Character.MIN_VALUE));
    	
    	assertIterableEquals(expected, actual);
    }
    
    /**
     * Test clearing the keyboard
     */
    @Test
    void testClear(){
    	VirtualKeyboard pressed = new VirtualKeyboard();
    	pressed.updateFromEvent(VirtualKey.W.getKeycode(), true, 'w');
    	pressed.updateFromEvent(VirtualKey.S.getKeycode(), true, 's');
    	pressed.updateFromEvent(VirtualKey.A.getKeycode(), true, 'a');
    	
    	pressed.clear();
    	
    	assertTrue(pressed.getPressedKeys().isEmpty());
    	assertTrue(pressed.getSubticks().isEmpty());
    	assertTrue(pressed.getCharList().isEmpty());
    }

    /**
     * Tests virtualEvents behaviour on a subtick, should fail
     */
    @Test
    void testGetVirtualEventsOnSubtick() {
    	
    	VirtualKeyboard pressed = new VirtualKeyboard(new HashSet<>(), new ArrayList<>(), null, false);
    	
    	// Load actual with the events
    	Queue<VirtualKeyboardEvent> actual = new ConcurrentLinkedQueue<>();
    	pressed.getVirtualEvents(pressed, actual);
    	
    	assertTrue(actual.isEmpty());
    }
    
    /**
     * Test repeat events enabled
     */
    @Test
    void testRepeatEvents(){
        VirtualKeyboard testKb = new VirtualKeyboard();

        int keycode = VirtualKey.BACK.getKeycode();

        // Update the keyboard multiple times with the same value
        testKb.updateFromEvent(keycode, true, Character.MIN_VALUE, true);
        testKb.updateFromEvent(keycode, true, Character.MIN_VALUE, true);
        testKb.updateFromEvent(keycode, true, Character.MIN_VALUE, true);

        Queue<VirtualKeyboardEvent> actual = new ConcurrentLinkedQueue<>();
        // Fill "actual" with VirtualKeyboardEvents
        new VirtualKeyboard().getVirtualEvents(testKb, actual);

        List<VirtualKeyboardEvent> expected = new ArrayList<>();
        // Add expected VirtualKeyboardEvents
        expected.add(new VirtualKeyboardEvent(keycode, true, Character.MIN_VALUE));
        expected.add(new VirtualKeyboardEvent(keycode, true, Character.MIN_VALUE));
        expected.add(new VirtualKeyboardEvent(keycode, true, Character.MIN_VALUE));

        assertIterableEquals(expected, actual);
    }

    /**
     * Same as {@link #testRepeatEvents()} but with repeat events disabled
     */
    @Test
    void testRepeatEventsFail(){
        VirtualKeyboard testKb = new VirtualKeyboard();

        int keycode = VirtualKey.BACK.getKeycode();
        // Update the keyboard multiple times with the same value.
        testKb.updateFromEvent(keycode, true, Character.MIN_VALUE, false);
        testKb.updateFromEvent(keycode, true, Character.MIN_VALUE, false);
        testKb.updateFromEvent(keycode, true, Character.MIN_VALUE, false);

        Queue<VirtualKeyboardEvent> actual = new ConcurrentLinkedQueue<>();
        // Fill "actual" with VirtualKeyboardEvents
        new VirtualKeyboard().getVirtualEvents(testKb, actual);

        List<VirtualKeyboardEvent> expected = new ArrayList<>();

        // Only one keyboard event should be added
        expected.add(new VirtualKeyboardEvent(keycode, true, Character.MIN_VALUE));

        assertIterableEquals(expected, actual);
    }
}
