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
package consulo.bazel.cpp.workspace;

import consulo.annotation.component.ExtensionImpl;
import consulo.bazel.workspace.BazelDataSerializer;
import consulo.bazel.workspace.BazelRecordXml;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jdom.Element;
import org.jetbrains.bazel.clion.workspace.CcCompilerKind;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the C/C++ resolve configurations. {@link CcWorkspaceModel} holds IDE objects, so it is written in a
 * serializable form and rebuilt through its own API on read:
 * <ul>
 *     <li>the language kind ({@code FileType}) is written as its id and looked up in the {@link FileTypeRegistry};</li>
 *     <li>the compiler kind as its id ({@link CcCompilerKind#getId()});</li>
 *     <li>paths as strings;</li>
 *     <li>the consulo-cpp {@code CFileOptions} are not written: {@link CcWorkspaceModel.CompilerSettings#setCompilerSwitches}
 *     derives them from the switches again.</li>
 * </ul>
 * The sources of a configuration share their switches, so every distinct {@link CcCompilerSwitches} is written once and
 * referenced by index.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionImpl
public class CcWorkspaceModelSerializer implements BazelDataSerializer<CcWorkspaceModel> {
    private static final String CLIENT_KEY = "clientKey";
    private static final String SWITCHES = "switches";
    private static final String SWITCHES_ITEM = "item";
    private static final String CONFIGURATION = "configuration";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String SOURCE = "source";
    private static final String LANGUAGE = "language";
    private static final String URL = "url";
    private static final String LANGUAGE_KIND = "languageKind";
    private static final String COMPILER_KIND = "compilerKind";
    private static final String COMPILER = "compiler";
    private static final String WORKING_DIR = "workingDir";

    private static final List<CcCompilerKind> KNOWN_COMPILER_KINDS = List.of(
        CcCompilerKind.GCC,
        CcCompilerKind.CLANG,
        CcCompilerKind.APPLE_CLANG,
        CcCompilerKind.MSVC,
        CcCompilerKind.CLANG_CL
    );

    @Override
    public String getId() {
        return "ccWorkspaceModel";
    }

    @Override
    public Class<CcWorkspaceModel> getDataClass() {
        return CcWorkspaceModel.class;
    }

    @Override
    public void write(CcWorkspaceModel data, Element element) {
        element.setAttribute(CLIENT_KEY, data.getClientKey());

        SwitchesTable table = new SwitchesTable();
        List<Element> configurations = new ArrayList<>();
        for (CcWorkspaceModel.Configuration configuration : data.getConfigurations()) {
            Element configurationElement = new Element(CONFIGURATION);
            configurationElement.setAttribute(ID, configuration.getUniqueId());
            configurationElement.setAttribute(NAME, configuration.getName());
            for (Map.Entry<String, CcWorkspaceModel.CompilerSettings> source : configuration.getSources().entrySet()) {
                Element sourceElement = new Element(SOURCE);
                sourceElement.setAttribute(URL, source.getKey());
                writeSettings(source.getValue(), sourceElement, table);
                configurationElement.addContent(sourceElement);
            }
            for (CcWorkspaceModel.CompilerSettings settings : configuration.getAllLanguageCompilerSettings().values()) {
                Element languageElement = new Element(LANGUAGE);
                writeSettings(settings, languageElement, table);
                configurationElement.addContent(languageElement);
            }
            configurations.add(configurationElement);
        }

        Element switchesElement = new Element(SWITCHES);
        for (CcCompilerSwitches switches : table.myList) {
            Element item = new Element(SWITCHES_ITEM);
            BazelRecordXml.writeRecord(switches, item);
            switchesElement.addContent(item);
        }
        element.addContent(switchesElement);
        for (Element configuration : configurations) {
            element.addContent(configuration);
        }
    }

    @Override
    public CcWorkspaceModel read(Element element) {
        String clientKey = required(element, CLIENT_KEY);

        List<CcCompilerSwitches> switches = new ArrayList<>();
        Element switchesElement = element.getChild(SWITCHES);
        if (switchesElement != null) {
            for (Element item : switchesElement.getChildren(SWITCHES_ITEM)) {
                switches.add(BazelRecordXml.readRecord(CcCompilerSwitches.class, item));
            }
        }

        Map<String, FileType> fileTypes = new HashMap<>();
        CcWorkspaceModel model = new CcWorkspaceModel(clientKey);
        for (Element configurationElement : element.getChildren(CONFIGURATION)) {
            CcWorkspaceModel.Configuration configuration =
                model.addConfiguration(required(configurationElement, ID), required(configurationElement, NAME));
            for (Element sourceElement : configurationElement.getChildren(SOURCE)) {
                CcWorkspaceModel.CompilerSettings settings =
                    configuration.addSource(required(sourceElement, URL), languageKind(sourceElement, fileTypes));
                readSettings(settings, sourceElement, switches);
            }
            for (Element languageElement : configurationElement.getChildren(LANGUAGE)) {
                CcWorkspaceModel.CompilerSettings settings =
                    configuration.getLanguageCompilerSettings(languageKind(languageElement, fileTypes));
                readSettings(settings, languageElement, switches);
            }
        }
        return model;
    }

    private static void writeSettings(CcWorkspaceModel.CompilerSettings settings, Element element, SwitchesTable table) {
        element.setAttribute(LANGUAGE_KIND, settings.getLanguageKind().getId());
        String compilerKind = settings.getCompilerKind().getId();
        if (compilerKind != null) {
            element.setAttribute(COMPILER_KIND, compilerKind);
        }
        Path compiler = settings.getCompiler();
        Path workingDir = settings.getCompilerWorkingDir();
        if (compiler != null && workingDir != null) {
            element.setAttribute(COMPILER, compiler.toString());
            element.setAttribute(WORKING_DIR, workingDir.toString());
        }
        element.setAttribute(SWITCHES, String.valueOf(table.indexOf(settings.getCompilerSwitches())));
    }

    private static void readSettings(CcWorkspaceModel.CompilerSettings settings, Element element, List<CcCompilerSwitches> switches) {
        String compiler = element.getAttributeValue(COMPILER);
        String workingDir = element.getAttributeValue(WORKING_DIR);
        if (compiler != null && workingDir != null) {
            settings.setCompiler(compilerKind(element.getAttributeValue(COMPILER_KIND)), Path.of(compiler), Path.of(workingDir));
        }
        int index = Integer.parseInt(required(element, SWITCHES));
        if (index < 0 || index >= switches.size()) {
            throw new IllegalArgumentException("Unknown switches " + index);
        }
        settings.setCompilerSwitches(switches.get(index));
    }

    private static CcCompilerKind compilerKind(@Nullable String id) {
        if (id == null) {
            return CcCompilerKind.UNKNOWN;
        }
        for (CcCompilerKind kind : KNOWN_COMPILER_KINDS) {
            if (id.equals(kind.getId())) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown compiler kind " + id);
    }

    private static FileType languageKind(Element element, Map<String, FileType> cache) {
        String id = required(element, LANGUAGE_KIND);
        FileType fileType = cache.get(id);
        if (fileType == null) {
            fileType = FileTypeRegistry.getInstance().findFileTypeByName(id);
            if (fileType == null) {
                throw new IllegalArgumentException("Unknown file type " + id);
            }
            cache.put(id, fileType);
        }
        return fileType;
    }

    private static String required(Element element, String attribute) {
        String value = element.getAttributeValue(attribute);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + attribute + " of " + element.getName());
        }
        return value;
    }

    /**
     * The distinct switches, by identity first (the sources of a configuration share one instance) and then by value.
     */
    private static final class SwitchesTable {
        private final List<CcCompilerSwitches> myList = new ArrayList<>();
        private final Map<CcCompilerSwitches, Integer> myByIdentity = new IdentityHashMap<>();
        private final Map<CcCompilerSwitches, Integer> myByValue = new HashMap<>();

        int indexOf(CcCompilerSwitches switches) {
            Integer index = myByIdentity.get(switches);
            if (index != null) {
                return index;
            }
            index = myByValue.get(switches);
            if (index == null) {
                index = myList.size();
                myList.add(switches);
                myByValue.put(switches, index);
            }
            myByIdentity.put(switches, index);
            return index;
        }
    }
}
