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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TristateBooleanTest {
    private static String json(Object state) throws Exception {
        return new String(StateJsonBinder.serialize(state), StandardCharsets.UTF_8);
    }

    private static TestBooleanState roundtrip(TestBooleanState state) throws Exception {
        return StateJsonBinder.deserialize(TestBooleanState.class, StateJsonBinder.serialize(state));
    }

    @Test
    public void untouchedTristateWritesNothing() throws Exception {
        assertThat(json(new TestBooleanState())).isEqualTo("{}");
    }

    @Test
    public void falseIsNeverConfusedWithUnset() throws Exception {
        TestBooleanState state = new TestBooleanState();
        state.tristate = Boolean.FALSE;

        assertThat(json(state)).isEqualTo("{\"tristate\":false}");
        assertThat(roundtrip(state).tristate).isFalse();
    }

    @Test
    public void trueIsWritten() throws Exception {
        TestBooleanState state = new TestBooleanState();
        state.tristate = Boolean.TRUE;

        assertThat(json(state)).isEqualTo("{\"tristate\":true}");
        assertThat(roundtrip(state).tristate).isTrue();
    }

    @Test
    public void nullIsWrittenOnlyWhenItDiffersFromTheDefault() throws Exception {
        TestBooleanState state = new TestBooleanState();
        state.enabledByDefault = null;

        assertThat(json(state))
            .describedAs("a null that differs from the field default carries information and must survive")
            .isEqualTo("{\"enabledByDefault\":null}");
        assertThat(roundtrip(state).enabledByDefault).isNull();
    }

    @Test
    public void defaultTrueIsNotWritten() throws Exception {
        TestBooleanState state = new TestBooleanState();
        state.enabledByDefault = Boolean.TRUE;

        assertThat(json(state)).isEqualTo("{}");
        assertThat(roundtrip(state).enabledByDefault).isTrue();
    }

    @Test
    public void primitiveFalseDefaultIsNotWritten() throws Exception {
        TestBooleanState state = new TestBooleanState();
        state.primitive = false;

        assertThat(json(state)).isEqualTo("{}");

        state.primitive = true;
        assertThat(json(state)).isEqualTo("{\"primitive\":true}");
        assertThat(roundtrip(state).primitive).isTrue();
    }

    @Test
    public void nullSurvivesTheEnvelopeAndMacroSubstitution() throws Exception {
        TestBooleanState state = new TestBooleanState();
        state.enabledByDefault = null;
        state.tristate = Boolean.FALSE;

        Map<String, byte[]> states = new LinkedHashMap<>();
        states.put("Boolish", StateJsonBinder.serialize(state));

        byte[] envelope = StateJsonBinder.substituteStrings(StateJsonBinder.writeEnvelope(states), value -> value);
        byte[] back = StateJsonBinder.readEnvelope(envelope).get("Boolish");

        TestBooleanState result = StateJsonBinder.deserialize(TestBooleanState.class, back);
        assertThat(result.enabledByDefault).isNull();
        assertThat(result.tristate).isFalse();
    }
}
