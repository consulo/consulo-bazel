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

import org.jdom.Element;

/**
 * A {@link BazelDataSerializer} of a record, written component by component with {@link BazelRecordXml}.
 * Implementations are {@code @ExtensionImpl} classes with a no-arg constructor calling {@code super(id, type)}.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public abstract class BazelRecordDataSerializer<T extends Record> implements BazelDataSerializer<T> {
    private final String myId;
    private final Class<T> myDataClass;

    protected BazelRecordDataSerializer(String id, Class<T> dataClass) {
        myId = id;
        myDataClass = dataClass;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public Class<T> getDataClass() {
        return myDataClass;
    }

    @Override
    public void write(T data, Element element) {
        BazelRecordXml.writeRecord(data, element);
    }

    @Override
    public T read(Element element) {
        return BazelRecordXml.readRecord(myDataClass, element);
    }
}
