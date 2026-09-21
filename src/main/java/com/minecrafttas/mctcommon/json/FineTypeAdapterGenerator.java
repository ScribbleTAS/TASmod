package com.minecrafttas.mctcommon.json;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FineTypeAdapterGenerator {

	protected final String className;

	protected final String packagge;

	protected final List<String> importLines = new ArrayList<>();
	protected final Map<Class<?>, List<String>> classLines = new LinkedHashMap<>();

	public FineTypeAdapterGenerator(String className, String packagge) {
		this.className = className;
		this.packagge = packagge;
		fillDefaultImports();
	}

	protected void fillDefaultImports() {
		importLines.add("import com.minecrafttas.mctcommon.json.FineField.FineMode;");
		importLines.add("import com.minecrafttas.mctcommon.json.FineTarget;");
		importLines.add("import com.minecrafttas.mctcommon.json.FineTypeAdapter;");
		importLines.add("");
	}

	public List<String> generate() {
		List<String> out = new ArrayList<>();
		out.add(String.format("package %s;", packagge));
		out.add("");
		out.addAll(importLines);
		out.add("");
		Collection<List<String>> classLines = this.classLines.values();
		classLines.forEach(e -> e.forEach(e1 -> out.add(e1)));
		return out;
	}

	public List<String> generateMulti() {
		List<String> out = new ArrayList<>();
		out.add(String.format("package %s;", packagge));
		out.add("");
		out.addAll(importLines);
		out.add("");
		out.add("@FineMultiTarget");
		out.add(String.format("public class %s {", className));
		out.add("");
		Collection<List<String>> classLines = this.classLines.values();
		classLines.forEach(e -> e.forEach(e1 -> out.add("\t" + e1)));
		out.add("}");
		return out;
	}

	public void addClass(Class<?>... classes) {
		for (Class<?> clazz : classes) {
			addClass(clazz);
		}
	}

	public <T> void addClass(T object) {
		Class<?> clazz = object.getClass();
		addClass(clazz);
	}

	public void addClass(Class<?> clazz) {
		if (classLines.containsKey(clazz))
			return;

		Class<?> superclazz = clazz.getSuperclass();
		if (superclazz != Object.class)
			addClass(superclazz);

		String clazzName = clazz.getSimpleName();
		List<Field> fields = FineTypeAdapter.getFieldList(clazz);

		importLines.add(String.format("import %s;", clazz.getName()));
		classLines.put(clazz, splitNewLine(String.format(""
				+ "@FineTarget(%s.class)\n"
				+ "public class %sTypeAdapter extends FineTypeAdapter {\n"
				+ "\n"
				+ "\tpublic %sTypeAdapter() {", clazzName, clazzName, clazzName)));

		for (Field field : fields) {
			classLines.get(clazz).add(String.format("\t\tregister(\"%s\", FineMode.FINE);", field.getName()));
		}

		classLines.get(clazz).add("\t}");
		classLines.get(clazz).add("}");
		classLines.get(clazz).add("");
	}

	protected List<String> splitNewLine(String text) {
		return new ArrayList<>(Arrays.asList(text.split("\n")));
	}
}
