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
package consulo.component.persist;

import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentSerializationUtilTest {
    @Test
    public void resolvesStateClassFromDirectImplementation() {
        assertThat(ComponentSerializationUtil.getStateClass(Direct.class)).isEqualTo(Bean.class);
    }

    @Test
    public void resolvesStateClassThroughAbstractBase() {
        assertThat(ComponentSerializationUtil.getStateClass(ConcreteOfAbstract.class)).isEqualTo(Bean.class);
    }

    @Test
    public void resolvesStateClassThroughIntermediateInterface() {
        assertThat(ComponentSerializationUtil.getStateClass(ViaInterface.class)).isEqualTo(Bean.class);
    }

    @Test
    public void resolvesElementStateClass() {
        assertThat(ComponentSerializationUtil.getStateClass(ElementBased.class)).isEqualTo(Element.class);
    }

    @Test
    public void loadComponentStateDeserializesIntoComponent() {
        Element element = new Element("state");
        Element option = new Element("option");
        option.setAttribute("name", "name");
        option.setAttribute("value", "loaded");
        element.addContent(option);

        Direct component = new Direct();
        ComponentSerializationUtil.loadComponentState(component, element);

        assertThat(component.myLoadCount).isEqualTo(1);
        assertThat(component.myLoadedName).isEqualTo("loaded");
    }

    @Test
    public void loadComponentStateIgnoresNullElement() {
        Direct component = new Direct();
        ComponentSerializationUtil.loadComponentState(component, null);

        assertThat(component.myLoadCount).isZero();
    }

    public static class Bean {
        public String name = "default";
    }

    public static class Direct implements PersistentStateComponent<Bean> {
        public String myLoadedName = "";
        public int myLoadCount;

        @Override
        public @Nullable Bean getState() {
            return null;
        }

        @Override
        public void loadState(Bean state) {
            myLoadedName = state.name;
            myLoadCount++;
        }
    }

    public abstract static class AbstractBase implements PersistentStateComponent<Bean> {
        @Override
        public @Nullable Bean getState() {
            return null;
        }

        @Override
        public void loadState(Bean state) {
        }
    }

    public static class ConcreteOfAbstract extends AbstractBase {
    }

    public interface BeanStateComponent extends PersistentStateComponent<Bean> {
    }

    public static class ViaInterface implements BeanStateComponent {
        @Override
        public @Nullable Bean getState() {
            return null;
        }

        @Override
        public void loadState(Bean state) {
        }
    }

    public static class ElementBased implements PersistentStateComponent<Element> {
        @Override
        public @Nullable Element getState() {
            return null;
        }

        @Override
        public void loadState(Element state) {
        }
    }
}
