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
package consulo.ui.ex.tree;

import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The format is shared with the awt {@code TreeState}, so it is pinned here rather than left to the
 * serializer's defaults.
 *
 * @author VISTALL
 * @since 2026-09-08
 */
public class UITreeStateTest {
    private static final String XML = """
        <subPane>
          <expand>
            <path>
              <item name="" type="6f2fa9c2:ProjectViewProjectNode" />
              <item name="src" type="1a2b3c4d:PsiDirectoryNode" />
            </path>
          </expand>
          <select>
            <path>
              <item name="" type="6f2fa9c2:ProjectViewProjectNode" />
              <item name="Main.java" type="5e6f7a8b:PsiFileNode" user="Main.java" />
            </path>
          </select>
        </subPane>""";

    @Test
    public void readAwtFormat() throws Exception {
        UITreeState state = deserialize(XML);

        assertFalse(state.isEmpty());

        assertEquals(1, state.expandedPaths.size());
        UITreePath expanded = state.expandedPaths.get(0);
        assertEquals(2, expanded.items.size());
        assertEquals("", expanded.items.get(0).id);
        assertEquals("6f2fa9c2:ProjectViewProjectNode", expanded.items.get(0).type);
        assertEquals("src", expanded.items.get(1).id);
        assertEquals("1a2b3c4d:PsiDirectoryNode", expanded.items.get(1).type);
        assertNull(expanded.items.get(1).userStr);

        assertEquals(1, state.selectedPaths.size());
        UITreePath selected = state.selectedPaths.get(0);
        assertEquals(2, selected.items.size());
        assertEquals("Main.java", selected.items.get(1).id);
        assertEquals("Main.java", selected.items.get(1).userStr);
    }

    @Test
    public void writeBackSameXml() throws Exception {
        assertEquals(XML, serialize(deserialize(XML)));
    }

    @Test
    public void emptyStateStillWritesBothWrappers() {
        assertEquals("""
            <subPane>
              <expand />
              <select />
            </subPane>""", serialize(new UITreeState()));
    }

    @Test
    public void emptyStateIsEmpty() throws Exception {
        assertTrue(deserialize("<subPane />").isEmpty());
    }

    private static UITreeState deserialize(String xml) throws IOException, JDOMException {
        Element element = JDOMUtil.loadDocument(xml).getRootElement();
        return Objects.requireNonNull(XmlSerializer.deserialize(element, UITreeState.class));
    }

    private static String serialize(UITreeState state) {
        Element element = new Element("subPane");
        XmlSerializer.serializeInto(state, element);
        return JDOMUtil.writeElement(element);
    }
}
