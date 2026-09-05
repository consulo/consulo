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
package consulo.component.store.impl.internal.json;

import consulo.component.impl.internal.PropertiesState;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesStateFormatTest {
    private static final String LEGACY_XML = """
        <component name="PropertiesComponent">
          <property name="DefaultHtmlDoctype.MigrateToHtml5" value="true" />
          <property name="junit_statistics_table_columnsWidth5" value="75" />
          <property name="MemberChooser.sorted" value="false" />
        </component>
        """;

    private static Map<String, String> expected() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("DefaultHtmlDoctype.MigrateToHtml5", "true");
        map.put("junit_statistics_table_columnsWidth5", "75");
        map.put("MemberChooser.sorted", "false");
        return map;
    }

    private static PropertiesState readXml(String xml) throws Exception {
        Element element = JDOMUtil.loadDocument(xml).getRootElement();
        return XmlSerializer.deserialize(element, PropertiesState.class);
    }

    private static Element writeXml(PropertiesState state) {
        return XmlSerializer.serializeIfNotDefault(state, new SkipDefaultValuesSerializationFilters());
    }

    @Test
    public void stateIsJsonCapable() {
        assertThat(StateJsonBinder.isJsonCapable(PropertiesState.class))
            .describedAs("PropertiesState must stay json capable - a final field or a concrete "
                + "collection type silently degrades it to an empty object")
            .isTrue();
    }

    @Test
    public void legacyXmlIsReadIntoProperties() throws Exception {
        PropertiesState state = readXml(LEGACY_XML);

        assertThat(state.properties).containsExactlyEntriesOf(expected());
    }

    @Test
    public void valuesStoredAsXmlAreReadBackIdenticallyThroughJson() throws Exception {
        PropertiesState fromXml = readXml(LEGACY_XML);

        byte[] json = StateJsonBinder.serialize(fromXml);
        PropertiesState fromJson = StateJsonBinder.deserialize(PropertiesState.class, json);

        assertThat(fromJson.properties).containsExactlyEntriesOf(fromXml.properties);
        assertThat(fromJson.properties).containsExactlyEntriesOf(expected());
    }

    @Test
    public void jsonFormIsAPlainNameToValueObject() throws Exception {
        byte[] json = StateJsonBinder.serialize(readXml(LEGACY_XML));

        assertThat(new String(json, StandardCharsets.UTF_8)).isEqualTo(
            "{\"properties\":{"
                + "\"DefaultHtmlDoctype.MigrateToHtml5\":\"true\","
                + "\"junit_statistics_table_columnsWidth5\":\"75\","
                + "\"MemberChooser.sorted\":\"false\"}}");
    }

    @Test
    public void unusualValuesSurviveAsPlainStrings() throws Exception {
        PropertiesState state = new PropertiesState();
        state.properties.put("leadingZero", "007");
        state.properties.put("tooBigForLong", "123456789012345678901234567890");
        state.properties.put("decimal", "1.50");
        state.properties.put("spaced", " 5");
        state.properties.put("quoted", "say \"hi\"");
        state.properties.put("empty", "");

        byte[] json = StateJsonBinder.serialize(state);

        assertThat(StateJsonBinder.deserialize(PropertiesState.class, json).properties)
            .containsExactlyInAnyOrderEntriesOf(state.properties);
    }

    @Test
    public void xmlStaysWritableInTheLegacyPropertyShape() throws Exception {
        Element written = writeXml(readXml(LEGACY_XML));

        assertThat(written.getChildren("property")).hasSize(3);

        Map<String, String> asAttributes = new LinkedHashMap<>();
        for (Element property : written.getChildren("property")) {
            asAttributes.put(property.getAttributeValue("name"), property.getAttributeValue("value"));
        }
        assertThat(asAttributes).containsExactlyInAnyOrderEntriesOf(expected());

        assertThat(readXml(JDOMUtil.writeElement(written)).properties)
            .containsExactlyInAnyOrderEntriesOf(expected());
    }

    @Test
    public void xmlSortsKeysWhileJsonKeepsDocumentOrder() throws Exception {
        PropertiesState fromXml = readXml(LEGACY_XML);

        assertThat(fromXml.properties.keySet()).containsExactly(
            "DefaultHtmlDoctype.MigrateToHtml5",
            "junit_statistics_table_columnsWidth5",
            "MemberChooser.sorted");

        assertThat(writeXml(fromXml).getChildren("property").stream()
            .map(property -> property.getAttributeValue("name")).toList())
            .containsExactly(
                "DefaultHtmlDoctype.MigrateToHtml5",
                "MemberChooser.sorted",
                "junit_statistics_table_columnsWidth5");
    }

    @Test
    public void emptyStateWritesNoProperties() throws Exception {
        byte[] json = StateJsonBinder.serialize(new PropertiesState());

        assertThat(new String(json, StandardCharsets.UTF_8)).isEqualTo("{}");
        assertThat(StateJsonBinder.isEmptyState(json)).isTrue();
    }

    @Test
    public void valuesSurviveAFullXmlToJsonToXmlTrip() throws Exception {
        PropertiesState fromXml = readXml(LEGACY_XML);
        byte[] json = StateJsonBinder.serialize(fromXml);
        PropertiesState fromJson = StateJsonBinder.deserialize(PropertiesState.class, json);

        Element backToXml = writeXml(fromJson);

        assertThat(readXml(JDOMUtil.writeElement(backToXml)).properties)
            .containsExactlyInAnyOrderEntriesOf(expected());
    }
}
