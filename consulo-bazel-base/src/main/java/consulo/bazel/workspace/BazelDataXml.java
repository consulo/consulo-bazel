/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.bazel.workspace;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The XML of a set of Bazel data values ({@code Map<Class<?>, Object>}), shared by {@link BazelModuleExtension} and
 * {@link BazelProjectDataStorage}:
 * <pre>{@code
 * <... formatVersion="1" pluginVersion="..." complete="true">
 *   <data id="bazelProjectDirectories">...</data>
 * </...>
 * }</pre>
 * Values are written by their {@link BazelDataSerializer}. A {@code data} element without a registered serializer (its
 * language plugin is not installed now) is kept as unknown XML and written back unchanged. {@code complete="false"} marks
 * a state that misses a value (no serializer for its type, or the value could not be written); on read, it also marks a
 * version mismatch or a malformed value. An incomplete state makes the project resync on startup.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
final class BazelDataXml {
    private static final Logger LOG = Logger.getInstance(BazelDataXml.class);

    /**
     * Increment when the XML layout or the meaning of a serializer id changes.
     */
    static final int FORMAT_VERSION = 1;

    static final String DATA = "data";
    private static final String ID = "id";
    private static final String FORMAT_VERSION_ATTRIBUTE = "formatVersion";
    private static final String PLUGIN_VERSION_ATTRIBUTE = "pluginVersion";
    private static final String COMPLETE_ATTRIBUTE = "complete";

    /**
     * @param values  the values by type
     * @param unknown {@code data} elements without a serializer
     * @param complete false when a value is missing
     */
    record DataSet(Map<Class<?>, Object> values, List<Element> unknown, boolean complete) {
        static final DataSet EMPTY = new DataSet(Map.of(), List.of(), true);
        static final DataSet INCOMPLETE = new DataSet(Map.of(), List.of(), false);

        DataSet {
            values = Map.copyOf(values);
            unknown = List.copyOf(unknown);
        }
    }

    private BazelDataXml() {
    }

    static String pluginVersion() {
        PluginDescriptor plugin = PluginManager.getPlugin(BazelDataXml.class);
        String version = plugin == null ? null : plugin.getVersion();
        return version == null ? "unknown" : version;
    }

    static void writeHeader(Element element, boolean complete) {
        element.setAttribute(FORMAT_VERSION_ATTRIBUTE, String.valueOf(FORMAT_VERSION));
        element.setAttribute(PLUGIN_VERSION_ATTRIBUTE, pluginVersion());
        element.setAttribute(COMPLETE_ATTRIBUTE, String.valueOf(complete));
    }

    /**
     * Whether the state was written by this format and plugin version.
     */
    static boolean isCurrentVersion(Element element) {
        return String.valueOf(FORMAT_VERSION).equals(element.getAttributeValue(FORMAT_VERSION_ATTRIBUTE)) &&
            pluginVersion().equals(element.getAttributeValue(PLUGIN_VERSION_ATTRIBUTE));
    }

    static boolean isMarkedComplete(Element element) {
        return "true".equals(element.getAttributeValue(COMPLETE_ATTRIBUTE));
    }

    /**
     * Appends the {@code data} elements of {@code data} to {@code parent}.
     *
     * @return false when a value could not be written
     */
    static boolean writeData(DataSet data, Element parent) {
        boolean complete = data.complete();
        List<Element> elements = new ArrayList<>();
        for (Map.Entry<Class<?>, Object> entry : data.values().entrySet()) {
            BazelDataSerializer<Object> serializer = BazelDataSerializer.findByClass(entry.getKey());
            if (serializer == null) {
                LOG.warn("No BazelDataSerializer for " + entry.getKey().getName() + ", the Bazel project data is saved incomplete");
                complete = false;
                continue;
            }
            try {
                Element element = new Element(DATA);
                element.setAttribute(ID, serializer.getId());
                serializer.write(entry.getValue(), element);
                elements.add(element);
            }
            catch (RuntimeException e) {
                LOG.warn("Cannot save Bazel data " + entry.getKey().getName() + ", the Bazel project data is saved incomplete", e);
                complete = false;
            }
        }
        // a stable order keeps the file unchanged between saves
        elements.sort(Comparator.comparing(it -> it.getAttributeValue(ID)));
        for (Element element : elements) {
            parent.addContent(element);
        }
        for (Element element : data.unknown()) {
            parent.addContent(element.clone());
        }
        return complete;
    }

    /**
     * Reads the {@code data} children of {@code parent}.
     */
    static DataSet readData(Element parent) {
        boolean complete = true;
        Map<Class<?>, Object> values = new HashMap<>();
        List<Element> unknown = new ArrayList<>();
        for (Element element : parent.getChildren(DATA)) {
            String id = element.getAttributeValue(ID);
            BazelDataSerializer<Object> serializer = id == null ? null : BazelDataSerializer.findById(id);
            if (serializer == null) {
                // consulo: the type belongs to a language plugin that is not installed now
                unknown.add(element.clone());
                continue;
            }
            try {
                values.put(serializer.getDataClass(), serializer.read(element));
            }
            catch (RuntimeException e) {
                LOG.warn("Cannot restore Bazel data " + id + ", the project will be synced", e);
                complete = false;
            }
        }
        return new DataSet(values, unknown, complete);
    }
}
