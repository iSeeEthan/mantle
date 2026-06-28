package com.iseeethan.mantle.client;

import com.google.gson.JsonElement;
import com.iseeethan.mantle.world.gen.MantleGenConfig;
import com.mojang.serialization.JsonOps;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class MantleConfigPresets {

    private MantleConfigPresets() {}

    private static final com.google.gson.Gson GSON =
            new com.google.gson.GsonBuilder().setPrettyPrinting().create();

    public static Path dir() {
        Path p = FMLPaths.GAMEDIR.get().resolve("mantle-presets");
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
        }
        return p;
    }

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir())) {
            s.filter(f -> f.getFileName().toString().endsWith(".json"))
             .forEach(f -> {
                 String n = f.getFileName().toString();
                 out.add(n.substring(0, n.length() - 5));
             });
        } catch (IOException ignored) {
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    public static boolean save(String name, MantleGenConfig config) {
        String safe = sanitize(name);
        if (safe.isEmpty()) return false;
        Path file = dir().resolve(safe + ".json");
        JsonElement json = MantleGenConfig.CODEC.encodeStart(JsonOps.INSTANCE, config)
                .result().orElse(null);
        if (json == null) return false;
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, w);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static MantleGenConfig load(String name) {
        Path file = dir().resolve(sanitize(name) + ".json");
        if (!Files.exists(file)) return null;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement json = GSON.fromJson(r, JsonElement.class);
            MantleGenConfig c = MantleGenConfig.CODEC.parse(JsonOps.INSTANCE, json)
                    .result().orElse(null);
            if (c != null) c.clampAll();
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    public static void openFolder() {
        net.minecraft.Util.getPlatform().openUri(dir().toUri());
    }

    public static boolean delete(String name) {
        try {
            return Files.deleteIfExists(dir().resolve(sanitize(name) + ".json"));
        } catch (IOException e) {
            return false;
        }
    }

    private static String sanitize(String name) {
        if (name == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : name.trim().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ' ') sb.append(c);
        }
        return sb.toString().trim().replace(' ', '_');
    }
}
