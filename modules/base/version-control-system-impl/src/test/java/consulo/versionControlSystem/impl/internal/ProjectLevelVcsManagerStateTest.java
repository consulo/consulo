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
import consulo.versionControlSystem.VcsShowConfirmationOption;
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
public class ProjectLevelVcsManagerStateTest {
    private static final String XML = """
        <state settingsEditedManually="true">
          <OptionsSetting value="false" id="Add" />
          <ConfirmationsSetting value="1" id="Add" />
          <ConfirmationsSetting value="2" id="Remove" />
        </state>""";

    @Test
    public void readLegacyXml() throws Exception {
        ProjectLevelVcsManagerState state = deserialize(XML);

        assertTrue(state.settingsEditedManually);

        assertEquals(1, state.options.size());
        assertFalse(state.options.get(0).value);
        assertEquals("Add", state.options.get(0).id);

        assertEquals(2, state.confirmations.size());
        assertEquals("Add", state.confirmations.get(0).id);
        assertEquals(
            VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY,
            VcsShowConfirmationOption.Value.fromString(state.confirmations.get(0).value)
        );
        assertEquals("Remove", state.confirmations.get(1).id);
        assertEquals(
            VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY,
            VcsShowConfirmationOption.Value.fromString(state.confirmations.get(1).value)
        );
    }

    @Test
    public void writeBackSameXml() throws Exception {
        assertEquals(XML, serialize(deserialize(XML)));
    }

    @Test
    public void shownOptionKeepsItsValueAttribute() throws Exception {
        String xml = """
            <state>
              <OptionsSetting value="false" id="Add" />
            </state>""";

        assertEquals(xml, serialize(deserialize(xml)));
    }

    @Test
    public void emptyStateIsNotSerialized() {
        assertNull(XmlSerializer.serializeIfNotDefault(new ProjectLevelVcsManagerState(), new SkipDefaultValuesSerializationFilters()));
    }

    private static ProjectLevelVcsManagerState deserialize(String xml) throws IOException, JDOMException {
        Element element = JDOMUtil.loadDocument(xml).getRootElement();
        return Objects.requireNonNull(XmlSerializer.deserialize(element, ProjectLevelVcsManagerState.class));
    }

    private static String serialize(ProjectLevelVcsManagerState state) {
        Element element = XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters());
        return JDOMUtil.writeElement(Objects.requireNonNull(element));
    }
}
