package com.minecrafttas.mctcommon.json;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Target;

@Target(TYPE)
public @interface FineTarget {
	Class<?> value();
}
