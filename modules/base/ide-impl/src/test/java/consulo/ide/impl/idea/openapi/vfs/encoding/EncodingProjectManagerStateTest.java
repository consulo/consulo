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

import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author VISTALL
 * @since 2026-09-08
 */
public class EncodingProjectManagerStateTest {
    private static final String XML = """
        <x native2AsciiForPropertiesFiles="true" defaultCharsetForPropertiesFiles="UTF-8" defaultCharsetForConsole="windows-1251" addBOMForNewFiles="ALWAYS">
          <file url="file://$PROJECT_DIR$/src" charset="UTF-8" />
          <file url="PROJECT" charset="windows-1251" />
        </x>""";

    @Test
    public void readLegacyXml() throws Exception {
        EncodingProjectManagerState state = deserialize(XML);

        assertTrue(state.native2AsciiForPropertiesFiles);
        assertEquals("UTF-8", state.defaultCharsetForPropertiesFiles);
        assertEquals("windows-1251", state.defaultCharsetForConsole);
        assertEquals("ALWAYS", state.addBOMForNewFiles);

        assertEquals(2, state.files.size());
        assertEquals("file://$PROJECT_DIR$/src", state.files.get(0).url);
        assertEquals("UTF-8", state.files.get(0).charset);
        assertEquals("PROJECT", state.files.get(1).url);
        assertEquals("windows-1251", state.files.get(1).charset);
    }

    @Test
    public void writeBackSameXml() throws Exception {
        assertEquals(XML, serialize(deserialize(XML)));
    }

    @Test
    public void mappingsOnlyKeepNoAttributes() throws Exception {
        String xml = """
            <x>
              <file url="file://$PROJECT_DIR$/src" charset="UTF-8" />
            </x>""";

        assertEquals(xml, serialize(deserialize(xml)));
    }

    @Test
    public void emptyStateIsNotSerialized() {
        assertNull(XmlSerializer.serializeIfNotDefault(new EncodingProjectManagerState(), new SkipDefaultValuesSerializationFilters()));
    }

    private static EncodingProjectManagerState deserialize(String xml) throws IOException, JDOMException {
        Element element = JDOMUtil.loadDocument(xml).getRootElement();
        return Objects.requireNonNull(XmlSerializer.deserialize(element, EncodingProjectManagerState.class));
    }

    private static String serialize(EncodingProjectManagerState state) {
        Element element = XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters());
        return JDOMUtil.writeElement(Objects.requireNonNull(element));
    }
}
