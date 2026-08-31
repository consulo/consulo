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
package consulo.component.store.impl.internal;

import consulo.util.xml.serializer.JDOMExternalizable;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultStateSerializerTest {
    @Test
    public void elementStateIsPassedThroughUntouched() {
        Element state = new Element("component");
        state.setAttribute("name", "Foo");

        assertThat(DefaultStateSerializer.serializeState(state, null)).isSameAs(state);
    }

    @Test
    public void elementStateIsDeserializedAsIdentity() {
        Element state = new Element("component");

        assertThat(DefaultStateSerializer.deserializeState(state, Element.class)).isSameAs(state);
    }

    @Test
    public void nullElementDeserializesToNull() {
        assertThat(DefaultStateSerializer.deserializeState(null, Bean.class)).isNull();
        assertThat(DefaultStateSerializer.deserializeState(null, Element.class)).isNull();
    }

    @Test
    public void jdomExternalizableUsesWriteExternal() {
        Legacy legacy = new Legacy();
        legacy.myValue = "written";

        Element element = Objects.requireNonNull(DefaultStateSerializer.serializeState(legacy, null));

        assertThat(element.getAttributeValue("value")).isEqualTo("written");
    }

    @Test
    public void jdomExternalizableUsesReadExternal() {
        Element element = new Element("temp_element");
        element.setAttribute("value", "read");

        Legacy legacy = Objects.requireNonNull(DefaultStateSerializer.deserializeState(element, Legacy.class));

        assertThat(legacy.myValue).isEqualTo("read");
    }

    @Test
    public void beanRoundTripsThroughXmlSerializer() {
        Bean bean = new Bean();
        bean.name = "changed";
        bean.count = 7;
        bean.items.add("a");

        Element element = Objects.requireNonNull(DefaultStateSerializer.serializeState(bean, null));
        Bean restored = Objects.requireNonNull(DefaultStateSerializer.deserializeState(element, Bean.class));

        assertThat(restored.name).isEqualTo("changed");
        assertThat(restored.count).isEqualTo(7);
        assertThat(restored.items).containsExactly("a");
    }

    @Test
    public void beanSerializationSkipsDefaultValues() {
        Bean bean = new Bean();
        bean.count = 7;

        Element element = Objects.requireNonNull(DefaultStateSerializer.serializeState(bean, null));

        assertThat(optionNames(element)).containsExactly("count");
    }

    @Test
    public void beanWithOnlyDefaultValuesSerializesToNull() {
        assertThat(DefaultStateSerializer.serializeState(new Bean(), null)).isNull();
    }

    private static List<String> optionNames(Element element) {
        List<String> names = new ArrayList<>();
        for (Element child : element.getChildren()) {
            String name = child.getAttributeValue("name");
            names.add(name == null ? child.getName() : name);
        }
        return names;
    }

    public static class Bean {
        public String name = "default";
        public int count = 1;
        public List<String> items = new ArrayList<>();
    }

    public static class Legacy implements JDOMExternalizable {
        public String myValue = "";

        @Override
        public void readExternal(Element element) {
            String value = element.getAttributeValue("value");
            myValue = value == null ? "" : value;
        }

        @Override
        public void writeExternal(Element element) {
            element.setAttribute("value", myValue);
        }
    }
}
