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

import consulo.component.store.internal.JDOMExternalizableWrapper;
import consulo.component.store.internal.StateComponentInfo;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.RoamingTypeDisabled;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.util.xml.serializer.JDOMExternalizable;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StateComponentInfoTest {
    @Test
    public void buildReadsStateAnnotation() {
        StateComponentInfo info = Objects.requireNonNull(StateComponentInfo.build(new Annotated(), null));

        assertThat(info.getName()).isEqualTo("Annotated");
        assertThat(info.getState().storages()).hasSize(1);
        assertThat(info.getState().storages()[0].value()).isEqualTo("annotated.xml");
    }

    @Test
    public void buildFindsStateAnnotationOnSuperclass() {
        StateComponentInfo info = Objects.requireNonNull(StateComponentInfo.build(new InheritsAnnotation(), null));

        assertThat(info.getName()).isEqualTo("Annotated");
    }

    @Test
    public void buildReturnsNullForNonStateObject() {
        assertThat(StateComponentInfo.build("not a component", null)).isNull();
    }

    @Test
    public void buildThrowsWhenStateAnnotationMissing() {
        assertThatThrownBy(() -> StateComponentInfo.build(new NoAnnotation(), null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No @State annotation found");
    }

    @Test
    public void buildThrowsWhenStoragesEmpty() {
        assertThatThrownBy(() -> StateComponentInfo.build(new NoStorages(), null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No @State.storages()");
    }

    @Test
    public void jdomExternalizableWithoutAnnotationGetsSynthesizedState() {
        StateComponentInfo info = Objects.requireNonNull(StateComponentInfo.build(new Legacy(), null));

        assertThat(info.getName()).isEqualTo(Legacy.class.getName());
        assertThat(info.getComponent()).isInstanceOf(JDOMExternalizableWrapper.class);

        Storage storage = info.getState().storages()[0];
        assertThat(storage.file()).isEqualTo(StoragePathMacros.DEFAULT_FILE);
        assertThat(storage.roamingType()).isEqualTo(RoamingType.DEFAULT);
    }

    @Test
    public void roamingTypeDisabledMarkerDisablesRoamingOnSynthesizedState() {
        StateComponentInfo info = Objects.requireNonNull(StateComponentInfo.build(new NonRoamingLegacy(), null));

        assertThat(info.getState().storages()[0].roamingType()).isEqualTo(RoamingType.DISABLED);
    }

    @Test
    public void jdomExternalizableWithAnnotationKeepsIt() {
        StateComponentInfo info = Objects.requireNonNull(StateComponentInfo.build(new AnnotatedLegacy(), null));

        assertThat(info.getName()).isEqualTo("AnnotatedLegacy");
        assertThat(info.getComponent()).isInstanceOf(JDOMExternalizableWrapper.class);
    }

    @Test
    public void infosWrappingDifferentComponentsAreNotEqual() {
        StateComponentInfo first = Objects.requireNonNull(StateComponentInfo.build(new Annotated(), null));
        StateComponentInfo second = Objects.requireNonNull(StateComponentInfo.build(new AnotherAnnotated(), null));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    public void infosWrappingSameComponentAreEqual() {
        Annotated component = new Annotated();
        StateComponentInfo first = Objects.requireNonNull(StateComponentInfo.build(component, null));
        StateComponentInfo second = Objects.requireNonNull(StateComponentInfo.build(component, null));

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    public void infosWrappingSameJdomExternalizableAreEqual() {
        Legacy component = new Legacy();
        StateComponentInfo first = Objects.requireNonNull(StateComponentInfo.build(component, null));
        StateComponentInfo second = Objects.requireNonNull(StateComponentInfo.build(component, null));

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    public static class Bean {
        public String name = "default";
    }

    @State(name = "Annotated", storages = @Storage("annotated.xml"))
    public static class Annotated implements PersistentStateComponent<Bean> {
        @Override
        public @Nullable Bean getState() {
            return null;
        }

        @Override
        public void loadState(Bean state) {
        }
    }

    @State(name = "AnotherAnnotated", storages = @Storage("another.xml"))
    public static class AnotherAnnotated implements PersistentStateComponent<Bean> {
        @Override
        public @Nullable Bean getState() {
            return null;
        }

        @Override
        public void loadState(Bean state) {
        }
    }

    public static class InheritsAnnotation extends Annotated {
    }

    public static class NoAnnotation implements PersistentStateComponent<Bean> {
        @Override
        public @Nullable Bean getState() {
            return null;
        }

        @Override
        public void loadState(Bean state) {
        }
    }

    @State(name = "NoStorages", storages = {})
    public static class NoStorages implements PersistentStateComponent<Bean> {
        @Override
        public @Nullable Bean getState() {
            return null;
        }

        @Override
        public void loadState(Bean state) {
        }
    }

    public static class Legacy implements JDOMExternalizable {
        @Override
        public void readExternal(Element element) {
        }

        @Override
        public void writeExternal(Element element) {
        }
    }

    public static class NonRoamingLegacy extends Legacy implements RoamingTypeDisabled {
    }

    @State(name = "AnnotatedLegacy", storages = @Storage("annotated-legacy.xml"))
    public static class AnnotatedLegacy extends Legacy {
    }
}
