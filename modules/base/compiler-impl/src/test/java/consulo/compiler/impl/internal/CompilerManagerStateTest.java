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
package consulo.compiler.impl.internal;

import consulo.compiler.setting.ExcludedEntriesConfigurationState;
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
public class CompilerManagerStateTest {
    private static final String XML = """
        <state url="file://$PROJECT_DIR$/out">
          <module name="core">
            <output url="file://$PROJECT_DIR$/out/production/core" type="production" />
            <output url="file://$PROJECT_DIR$/out/test/core" type="test" />
          </module>
          <module name="util" exclude="false">
            <output url="file://$PROJECT_DIR$/out/production/util" type="production" />
          </module>
          <exclude-from-compilation>
            <file url="file://$PROJECT_DIR$/core/src/Generated.java" />
            <directory url="file://$PROJECT_DIR$/core/gen" includeSubdirectories="true" />
          </exclude-from-compilation>
        </state>""";

    @Test
    public void readLegacyXml() throws Exception {
        CompilerManagerState state = deserialize(XML);

        assertEquals("file://$PROJECT_DIR$/out", state.url);
        assertEquals(2, state.modules.size());

        CompilerManagerModuleState core = state.modules.get(0);
        assertEquals("core", core.name);
        assertTrue(core.exclude);
        assertEquals(2, core.outputs.size());
        assertEquals("file://$PROJECT_DIR$/out/production/core", core.outputs.get(0).url);
        assertEquals("production", core.outputs.get(0).type);
        assertEquals("file://$PROJECT_DIR$/out/test/core", core.outputs.get(1).url);
        assertEquals("test", core.outputs.get(1).type);

        CompilerManagerModuleState util = state.modules.get(1);
        assertEquals("util", util.name);
        assertFalse(util.exclude);
        assertEquals(1, util.outputs.size());
        assertEquals("file://$PROJECT_DIR$/out/production/util", util.outputs.get(0).url);
        assertEquals("production", util.outputs.get(0).type);

        ExcludedEntriesConfigurationState exclude = Objects.requireNonNull(state.excludeFromCompilation);
        assertEquals(1, exclude.files.size());
        assertEquals("file://$PROJECT_DIR$/core/src/Generated.java", exclude.files.get(0).url);
        assertEquals(1, exclude.directories.size());
        assertEquals("file://$PROJECT_DIR$/core/gen", exclude.directories.get(0).url);
        assertTrue(exclude.directories.get(0).includeSubdirectories);
    }

    @Test
    public void directoryWithoutSubdirectoriesLoosesDefaultAttribute() throws Exception {
        String xml = """
            <state>
              <exclude-from-compilation>
                <directory url="file://$PROJECT_DIR$/core/gen" includeSubdirectories="false" />
              </exclude-from-compilation>
            </state>""";

        CompilerManagerState state = deserialize(xml);
        assertFalse(Objects.requireNonNull(state.excludeFromCompilation).directories.get(0).includeSubdirectories);

        assertEquals("""
            <state>
              <exclude-from-compilation>
                <directory url="file://$PROJECT_DIR$/core/gen" />
              </exclude-from-compilation>
            </state>""", serialize(state));
    }

    @Test
    public void writeBackSameXml() throws Exception {
        assertEquals(XML, serialize(deserialize(XML)));
    }

    @Test
    public void emptyStateIsNotSerialized() {
        assertNull(XmlSerializer.serializeIfNotDefault(new CompilerManagerState(), new SkipDefaultValuesSerializationFilters()));
    }

    private static CompilerManagerState deserialize(String xml) throws IOException, JDOMException {
        Element element = JDOMUtil.loadDocument(xml).getRootElement();
        return Objects.requireNonNull(XmlSerializer.deserialize(element, CompilerManagerState.class));
    }

    private static String serialize(CompilerManagerState state) {
        Element element = XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters());
        return JDOMUtil.writeElement(Objects.requireNonNull(element));
    }
}
