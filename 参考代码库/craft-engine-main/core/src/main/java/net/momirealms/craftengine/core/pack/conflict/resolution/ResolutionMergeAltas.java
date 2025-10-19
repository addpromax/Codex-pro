package net.momirealms.craftengine.core.pack.conflict.resolution;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashSet;
import java.util.Map;

public class ResolutionMergeAltas implements Resolution {
    public static final Factory FACTORY = new Factory();
    public static final ResolutionMergeAltas INSTANCE = new ResolutionMergeAltas();

    @Override
    public void run(PathContext existing, PathContext conflict) {
        try {
            JsonObject j1 = GsonHelper.readJsonFile(existing.path()).getAsJsonObject();
            JsonObject j2 = GsonHelper.readJsonFile(conflict.path()).getAsJsonObject();
            JsonObject j3 = new JsonObject();
            JsonArray ja1 = j1.getAsJsonArray("sources");
            JsonArray ja2 = j2.getAsJsonArray("sources");
            JsonArray ja3 = new JsonArray();
            HashSet<String> elements = new HashSet<>();
            for (JsonElement je : ja1) {
                if (elements.add(je.toString())) {
                    ja3.add(je);
                }
            }
            for (JsonElement je : ja2) {
                if (elements.add(je.toString())) {
                    ja3.add(je);
                }
            }
            j3.add("sources", ja3);
            GsonHelper.writeJsonFile(j3, existing.path());
        } catch (Exception e) {
            CraftEngine.instance().logger().severe("Failed to merge json when resolving file conflicts", e);
        }
    }

    @Override
    public Key type() {
        return Resolutions.MERGE_ATLAS;
    }

    public static class Factory implements ResolutionFactory {

        @Override
        public Resolution create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
