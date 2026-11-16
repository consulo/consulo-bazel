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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Persists one type of the data attached with {@link BazelModelBuilder#putProjectData}, {@link BazelModelBuilder#putModuleData}
 * and {@link BazelModelBuilder#putLibraryData} (upstream: the custom workspace entities, which the IntelliJ workspace model
 * caches on disk).
 * <p>
 * Module data is saved with the module in the {@link BazelModuleExtension}, project and library data in
 * {@link BazelProjectDataStorage}. A type is saved only when a serializer for it is registered; the serializer lives in the
 * library that owns the type, so data of a language plugin that is not installed is kept as unknown XML and skipped on
 * read. A record whose components are strings, primitives, enums, collections and other records can use
 * {@link BazelRecordDataSerializer}.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BazelDataSerializer<T> {
    @SuppressWarnings("rawtypes")
    ExtensionPointCacheKey<BazelDataSerializer, Map<String, BazelDataSerializer>> BY_ID =
        ExtensionPointCacheKey.groupBy("BazelDataSerializerById", BazelDataSerializer::getId);

    @SuppressWarnings("rawtypes")
    ExtensionPointCacheKey<BazelDataSerializer, Map<Class, BazelDataSerializer>> BY_CLASS =
        ExtensionPointCacheKey.groupBy("BazelDataSerializerByClass", BazelDataSerializer::getDataClass);

    /**
     * The stable name of the type in the XML, kept when the class is renamed or moved.
     */
    String getId();

    Class<T> getDataClass();

    void write(T data, Element element);

    /**
     * @throws RuntimeException when the element is malformed; the whole stored state is then treated as incomplete
     */
    T read(Element element);

    @SuppressWarnings("unchecked")
    static @Nullable BazelDataSerializer<Object> findById(String id) {
        return Application.get().getExtensionPoint(BazelDataSerializer.class).getOrBuildCache(BY_ID).get(id);
    }

    @SuppressWarnings("unchecked")
    static @Nullable BazelDataSerializer<Object> findByClass(Class<?> type) {
        return Application.get().getExtensionPoint(BazelDataSerializer.class).getOrBuildCache(BY_CLASS).get(type);
    }
}
