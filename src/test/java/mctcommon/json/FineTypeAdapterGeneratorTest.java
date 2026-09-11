package mctcommon.json;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.minecrafttas.mctcommon.json.FineTypeAdapterGenerator;

import net.minecraft.entity.ai.EntityAINearestAttackableTarget;

class FineTypeAdapterGeneratorTest {

	@Test
	void testGenerate() {
		List<String> actual = FineTypeAdapterGenerator.generateClass(EntityAINearestAttackableTarget.class);
		System.out.println(String.join("\n", actual));
	}

}
