package com.minecrafttas.mctcommon.json;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FineTypeAdapterGenerator {

	protected final String packagge;

	public FineTypeAdapterGenerator(Path out, String packagge) {
		out = out.resolve("typeadapters2");
		try {
			Files.createDirectories(out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.packagge = packagge;
	}

	public <T> void generate(T object) {

		Class<?> clazz = object.getClass();
		List<String> content = generateClass(clazz);
	}

	public static List<String> generateClass(Class<?> clazz) {
		List<String> out = new ArrayList<>();
		String className = clazz.getSimpleName();
		List<Field> fields = FineTypeAdapter.getFieldList(clazz);

		out.addAll(Arrays.asList(String.format("package com.minecrafttas.tasmod.savestates.typeadapters;\n"
				+ "\n"
				+ "import com.minecrafttas.mctcommon.json.FineField.FineMode;\n"
				+ "import com.minecrafttas.mctcommon.json.FineTarget;\n"
				+ "import com.minecrafttas.mctcommon.json.FineTypeAdapter;\n"
				+ "\n"
				+ "import %s;\n"
				+ "\n"
				+ "@FineTarget(%s.class)\n"
				+ "public class %sTypeAdapter extends FineTypeAdapter {\n"
				+ "\n"
				+ "\tpublic %sTypeAdapter() {", clazz.getName(), className, className, className).split("\n")));

		for (Field field : fields) {
			out.add(String.format("\t\tregister(\"%s\", FineMode.FINE);", field.getName()));
		}

		out.addAll(Arrays.asList(String.format("\t}\n"
				+ "}\n"
				+ "\n").split("\n")));

		return out;
	}
}
