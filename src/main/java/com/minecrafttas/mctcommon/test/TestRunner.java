package com.minecrafttas.mctcommon.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.minecrafttas.mctcommon.MCTCommon;
import com.minecrafttas.mctcommon.events.EventClient.EventClientInit;
import com.minecrafttas.mctcommon.events.EventListenerRegistry;
import com.minecrafttas.mctcommon.registry.AbstractRegistry;
import com.minecrafttas.tasmod.TASmod;
import com.minecrafttas.tasmod.util.Component;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextFormatting;

public class TestRunner extends AbstractRegistry<TestBase> implements EventClientInit {

	private Thread testRunnerThread;

	public TestRunner() {
		super("TEST_RUNNER", new LinkedHashMap<>());
	}

	public void register(String testName) {
		try {
			Class<?> testMotion = Class.forName("com.minecrafttas.tasmod.test." + testName);
			register((TestBase) testMotion.newInstance());
		} catch (Exception e) {
			MCTCommon.LOGGER.catching(e);
		}
	}

	@Override
	public void register(TestBase registryObject) {
		EventListenerRegistry.register(registryObject);
		super.register(registryObject);
	}

	@Override
	public void onClientInit(Minecraft mc) {
		if (isTestingEnabled()) {
			testRunnerThread = new Thread(() -> {
				runTests();
			}, "TestRunner");
			testRunnerThread.start();
		}
	}

	private void runTests() {
		MCTCommon.LOGGER.info(MCTCommon.Test, "Running Tests");
		Collection<TestBase> testClasses = this.REGISTRY.values();

		/*
		 * Check if a Favored Annotation is present and only run these classes.
		 * If not, then run all classes
		 */
		List<TestBase> favouredClassCollection = new ArrayList<>();
		for (TestBase testClass : testClasses) {
			if (testClass.getClass().isAnnotationPresent(Favoured.class))
				favouredClassCollection.add(testClass);
		}

		testClasses = favouredClassCollection.isEmpty() ? testClasses : favouredClassCollection;

		/*
		 * Check if a Favored Annotation is present on the methods
		 */
		for (TestBase testClass : testClasses) {

			Method[] methods = testClass.getClass().getMethods();

			List<Method> methodCollection = new ArrayList<>();
			List<Method> favouredMethodCollection = new ArrayList<>();
			for (Method testMethod : methods) {
				if (testMethod.isAnnotationPresent(Test.class)) {
					methodCollection.add(testMethod);

					if (testMethod.isAnnotationPresent(Favoured.class))
						favouredMethodCollection.add(testMethod);
				}
			}

			List<Method> methodsToRun = favouredMethodCollection.isEmpty() ? methodCollection : favouredMethodCollection;

			/*
			 * Run the tests
			 */
			testClass.beforeAll();
			for (Method testMethod : methodsToRun) {
				String testName = testMethod.getName();
				String testDescription = testMethod.getAnnotation(Test.class).value();
				try {
					Minecraft mc = Minecraft.getMinecraft();
					mc.addScheduledTask(() -> {
						mc.ingameGUI.getChatGUI().clearChatMessages(true);
						mc.ingameGUI.addChatMessage(ChatType.CHAT, Component.translatable("Running test %s", Component.literal(testName).withStyle(TextFormatting.AQUA)).build());
						mc.ingameGUI.addChatMessage(ChatType.CHAT, Component.literal(testDescription).withStyle(TextFormatting.GOLD).build());
						mc.ingameGUI.addChatMessage(ChatType.CHAT, Component.literal("").build());
					});
					testClass.beforeEach();
					testMethod.invoke(testClass);
					testClass.afterEach();
					TASmod.LOGGER.info("Test {} PASSED", testName);
					mc.addScheduledTask(() -> {
						mc.ingameGUI.addChatMessage(ChatType.CHAT, Component.translatable("Test %s", Component.literal("PASSED").withStyle(TextFormatting.GREEN)).build());
					});
				} catch (Exception e) {
					TASmod.LOGGER.error("Test {} FAILED", testName);
					if (testDescription != null)
						TASmod.LOGGER.error(testDescription);
					TASmod.LOGGER.catching(e.getCause());
					Minecraft mc = Minecraft.getMinecraft();
					mc.addScheduledTask(() -> {
						mc.ingameGUI.addChatMessage(ChatType.CHAT, Component.translatable("Test %s", Component.literal("FAILED").withStyle(TextFormatting.RED)).build());
					});
				}
				try {
					Thread.sleep(750);
				} catch (InterruptedException e) {
					TASmod.LOGGER.catching(e);
				}
			}
			testClass.afterAll();
		}
		System.exit(0);
	}

	public static boolean isTestingEnabled() {
		return System.getProperty("mctcommon.testrunner", "false").equals("true");
	}
}
