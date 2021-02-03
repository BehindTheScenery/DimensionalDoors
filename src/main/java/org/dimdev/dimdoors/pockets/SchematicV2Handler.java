package org.dimdev.dimdoors.pockets;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.*;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.dimdoors.pockets.generator.PocketGenerator;
import org.dimdev.dimdoors.pockets.virtual.VirtualPocket;
import org.dimdev.dimdoors.util.PocketGenerationParameters;
import org.dimdev.dimdoors.util.schematic.v2.Schematic;

public class SchematicV2Handler {
    private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().create();
    private static final SchematicV2Handler INSTANCE = new SchematicV2Handler();
    private final Map<String, PocketGenerator> pocketGeneratorMap = Maps.newHashMap();
    private final Map<String, PocketGroup> pocketGroups = Maps.newHashMap();
	private final Map<Identifier, PocketTemplateV2> templates = Maps.newHashMap();
    private boolean loaded = false;

    private SchematicV2Handler() {
    }

    public void load() {
        if (this.loaded) {
            throw new UnsupportedOperationException("Attempted to load schematics twice!");
        }
        this.loaded = true;
        long startTime = System.currentTimeMillis();

		try {
			Path path = Paths.get(SchematicV2Handler.class.getResource("/data/dimdoors/pockets/generators").toURI());
			loadCompound(path, new String[0], this::loadPocketGenerator);
			LOGGER.info("Loaded pockets in {} seconds", System.currentTimeMillis() - startTime);
		} catch (URISyntaxException e) {
			LOGGER.error(e);
		}

		startTime = System.currentTimeMillis();
		try {
			Path path = Paths.get(SchematicV2Handler.class.getResource("/data/dimdoors/pockets/groups").toURI());
			loadCompound(path, new String[0], this::loadPocketGroup);
			LOGGER.info("Loaded pocket groups in {} seconds", System.currentTimeMillis() - startTime);
		} catch (URISyntaxException e) {
			LOGGER.error(e);
		}
    }

    private void loadCompound(Path path, String[] idParts, BiConsumer<String, CompoundTag> loader) {
		if (Files.isDirectory(path)) {
			try {
				for (Path directoryPath : Files.newDirectoryStream(path)) {
					String[] directoryIdParts = Arrays.copyOf(idParts, idParts.length + 1);
					String fileName = directoryPath.getFileName().toString();
					if (Files.isRegularFile(directoryPath)) fileName = fileName.substring(0, fileName.lastIndexOf('.')); // cut extension
					directoryIdParts[directoryIdParts.length - 1] = fileName;
					loadCompound(directoryPath, directoryIdParts, loader);
				}
			} catch (IOException e) {
				LOGGER.error("could not load pocket data in path " + path.toString() + " due to malformed json.", e);
			}
		} else if(Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json")) {
			String id = String.join(".", idParts);
			try {
				JsonObject json = GSON.fromJson(String.join("", Files.readAllLines(path)), JsonObject.class);
				CompoundTag tag = CompoundTag.CODEC.decode(JsonOps.INSTANCE, json).getOrThrow(false, LOGGER::error).getFirst();
				loader.accept(id, tag);
			} catch (IOException e) {
				LOGGER.error("could not load pocket data in path " + path.toString() + " due to malformed json.", e);
			}
		}
	}

	private void loadPocketGroup(String id, CompoundTag tag) {
		PocketGroup group = new PocketGroup(id).fromTag(tag);
		pocketGroups.put(id, group);
	}

	private void loadPocketGenerator(String id, CompoundTag tag) {
		PocketGenerator gen =  PocketGenerator.deserialize(tag);
		if (gen != null) pocketGeneratorMap.put(id, gen);
	}

    public void loadSchematic(Identifier templateID, String id) {
    	try {
			if (templates.containsKey(templateID)) return;
			Path schemPath = Paths.get(SchematicV2Handler.class.getResource(String.format("/data/dimdoors/pockets/schematic/%s.schem", id.replaceAll("\\.", "/"))).toURI());
			CompoundTag schemTag = NbtIo.readCompressed(Files.newInputStream(schemPath));
			Schematic schematic = Schematic.fromTag(schemTag);
			PocketTemplateV2 template = new PocketTemplateV2(schematic, id);
			templates.put(templateID, template);
		} catch (URISyntaxException | IOException e) {
			LOGGER.error("Could not load schematic!", e);
		}
	}

	public VirtualPocket getRandomPocketFromGroup(String group, PocketGenerationParameters parameters) {
    	return pocketGroups.get(group).getPocketList().getNextRandomWeighted(parameters);
	}

    public static SchematicV2Handler getInstance() {
        return INSTANCE;
    }

    public Map<Identifier, PocketTemplateV2> getTemplates() {
        return this.templates;
    }

    public Map<String, PocketGroup> getPocketGroups() {
        return this.pocketGroups;
    }

    public PocketGenerator getGenerator(String id) {
    	return pocketGeneratorMap.get(id);
	}
}
