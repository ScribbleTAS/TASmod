package tasmod.virtual;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.events.EventListenerRegistry.EventBase;
import com.minecrafttas.tasmod.virtual.VirtualKey;
import com.minecrafttas.tasmod.virtual.VirtualKeyboard;

public class VirtualInputEventFiring {
	
	interface EventTest extends EventBase{
		
		void onTest(VirtualKeyboard keyboard);
	}
	
	interface EventCopy extends EventBase{
		
		void onCopy(VirtualKeyboard keyboard);
	}
	
	@BeforeAll
	static void beforeAll() {
		EventTest clear = (keyboard)-> {
			keyboard.clear();
		};
		EventCopy copy = (keyboard)-> {
			VirtualKeyboard newkeyboard = new VirtualKeyboard();
			newkeyboard.updateFromEvent(VirtualKey.A, true, 'a');
			newkeyboard.updateFromEvent(VirtualKey.D, true, 'd');
			keyboard.deepCopyFrom(newkeyboard);
		};
		EventListenerRegistry.register(clear, copy);
	}
	
	@Test
	void testClear() {
		VirtualKeyboard keyboard = new VirtualKeyboard();
		
		keyboard.updateFromEvent(VirtualKey.W, true, 'w');
		keyboard.updateFromEvent(VirtualKey.S, true, 's');
		
		EventListenerRegistry.fireEvent(EventTest.class, keyboard);
		
		assertTrue(keyboard.getPressedKeys().isEmpty());
	}
	
	@Test
	void testCopy() {
		
		VirtualKeyboard actual = new VirtualKeyboard();
		
		actual.updateFromEvent(VirtualKey.W, true, 'w');
		actual.updateFromEvent(VirtualKey.S, true, 's');
		
		VirtualKeyboard expected = new VirtualKeyboard();
		expected.updateFromEvent(VirtualKey.A, true, 'a');
		expected.updateFromEvent(VirtualKey.D, true, 'd');
		
		EventListenerRegistry.fireEvent(EventCopy.class, actual);
		
		assertEquals(expected, actual);
	}
}
