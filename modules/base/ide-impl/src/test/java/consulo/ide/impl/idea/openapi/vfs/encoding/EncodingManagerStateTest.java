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
package consulo.ide.impl.idea.openapi.vfs.encoding;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.ArrayAnalyzer;
import com.dslplatform.json.runtime.CollectionAnalyzer;
import com.dslplatform.json.runtime.EnumAnalyzer;
import com.dslplatform.json.runtime.MapAnalyzer;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author VISTALL
 * @since 2026-09-08
 */
public class EncodingManagerStateTest {
    private static final String XML = """
        <EncodingManagerState default_encoding="windows-1251" default_console_encoding="UTF-8" />""";

    @Test
    public void readLegacyXml() throws Exception {
        EncodingManagerState state = deserialize(XML);

        assertEquals("windows-1251", state.getDefaultCharsetName());
        assertEquals("UTF-8", state.getDefaultConsoleEncodingName());
    }

    @Test
    public void writeBackSameXml() throws Exception {
        assertEquals(XML, serialize(deserialize(XML)));
    }

    @Test
    public void defaultsAreNotSerialized() {
        EncodingManagerState state = new EncodingManagerState();

        assertEquals(StandardCharsets.UTF_8.name(), state.getDefaultCharsetName());
        assertEquals("", state.getDefaultConsoleEncodingName());
        assertNull(XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters()));
    }

    @Test
    public void stateIsJsonCapable() {
        DslJson<Object> binder = jsonBinder();

        assertNotNull(binder.tryFindWriter(EncodingManagerState.class), "no json writer registered - is @CompiledJson present?");
        assertNotNull(binder.tryFindReader(EncodingManagerState.class), "no json reader registered - is @CompiledJson present?");
    }

    @Test
    public void readJson() throws Exception {
        byte[] json = """
            {"defaultCharsetName":"windows-1251","defaultConsoleEncodingName":"UTF-8"}""".getBytes(StandardCharsets.UTF_8);

        EncodingManagerState state = Objects.requireNonNull(jsonBinder().deserialize(EncodingManagerState.class, json, json.length));

        assertEquals("windows-1251", state.getDefaultCharsetName());
        assertEquals("UTF-8", state.getDefaultConsoleEncodingName());
    }

    @Test
    public void writeBackSameJson() throws Exception {
        EncodingManagerState state = deserialize(XML);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        jsonBinder().serialize(state, out);
        byte[] json = out.toByteArray();

        EncodingManagerState reread = Objects.requireNonNull(jsonBinder().deserialize(EncodingManagerState.class, json, json.length));

        assertEquals("windows-1251", reread.getDefaultCharsetName());
        assertEquals("UTF-8", reread.getDefaultConsoleEncodingName());
    }

    private static DslJson<Object> jsonBinder() {
        return new DslJson<>(new DslJson.Settings<>()
            .resolveReader(MapAnalyzer.READER)
            .resolveWriter(MapAnalyzer.WRITER)
            .resolveReader(CollectionAnalyzer.READER)
            .resolveWriter(CollectionAnalyzer.WRITER)
            .resolveReader(ArrayAnalyzer.READER)
            .resolveWriter(ArrayAnalyzer.WRITER)
            .resolveWriter(EnumAnalyzer.CONVERTER)
            .resolveReader(EnumAnalyzer.CONVERTER)
            .includeServiceLoader(EncodingManagerState.class.getClassLoader()));
    }

    private static EncodingManagerState deserialize(String xml) throws IOException, JDOMException {
        Element element = JDOMUtil.loadDocument(xml).getRootElement();
        return Objects.requireNonNull(XmlSerializer.deserialize(element, EncodingManagerState.class));
    }

    private static String serialize(EncodingManagerState state) {
        Element element = XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters());
        return JDOMUtil.writeElement(Objects.requireNonNull(element));
    }
}
