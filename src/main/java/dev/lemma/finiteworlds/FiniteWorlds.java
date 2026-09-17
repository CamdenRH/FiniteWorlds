package dev.lemma.finiteworlds;

import dev.lemma.finiteworlds.worldgen.FiniteChunkGenerator;

import net.fabricmc.api.ModInitializer;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FiniteWorlds implements ModInitializer {

	public static final String MOD_ID = "finite-worlds";

	public static final Logger LOGGER =
			LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		Registry.register(
				Registries.CHUNK_GENERATOR,
				id("finite"),
				FiniteChunkGenerator.CODEC
		);

		LOGGER.info(
				"Finite Worlds initialized."
		);
	}

	public static Identifier id(String path) {
		return Identifier.of(
				MOD_ID,
				path
		);
	}
}