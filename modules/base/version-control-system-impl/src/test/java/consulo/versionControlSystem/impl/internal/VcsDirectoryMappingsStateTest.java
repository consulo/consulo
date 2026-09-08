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
package consulo.versionControlSystem.impl.internal;

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
public class VcsDirectoryMappingsStateTest {
    private static final String XML = """
        <state>
          <mapping directory="" vcs="Git" />
          <mapping directory="$PROJECT_DIR$/lib" vcs="Svn" />
        </state>""";

    @Test
    public void readLegacyXml() throws Exception {
        VcsDirectoryMappingsState state = deserialize(XML);

        assertFalse(state.defaultProject);
        assertEquals(2, state.mappings.size());
        assertEquals("", state.mappings.get(0).directory);
        assertEquals("Git", state.mappings.get(0).vcs);
        assertEquals("$PROJECT_DIR$/lib", state.mappings.get(1).directory);
        assertEquals("Svn", state.mappings.get(1).vcs);
    }

    @Test
    public void writeBackSameXml() throws Exception {
        assertEquals(XML, serialize(deserialize(XML)));
    }

    @Test
    public void defaultProjectIsKept() throws Exception {
        String xml = """
            <state defaultProject="true">
              <mapping directory="" vcs="" />
            </state>""";

        VcsDirectoryMappingsState state = deserialize(xml);
        assertTrue(state.defaultProject);
        assertEquals(xml, serialize(state));
    }

    @Test
    public void emptyStateIsNotSerialized() {
        assertNull(XmlSerializer.serializeIfNotDefault(new VcsDirectoryMappingsState(), new SkipDefaultValuesSerializationFilters()));
    }

    private static VcsDirectoryMappingsState deserialize(String xml) throws IOException, JDOMException {
        Element element = JDOMUtil.loadDocument(xml).getRootElement();
        return Objects.requireNonNull(XmlSerializer.deserialize(element, VcsDirectoryMappingsState.class));
    }

    private static String serialize(VcsDirectoryMappingsState state) {
        Element element = XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters());
        return JDOMUtil.writeElement(Objects.requireNonNull(element));
    }
}
