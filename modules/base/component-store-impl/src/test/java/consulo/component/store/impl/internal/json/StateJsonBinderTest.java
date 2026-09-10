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

import consulo.util.lang.ThreeState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class StateJsonBinderTest {
    private static class PlainState {
        public String name;
    }

    @Test
    public void freshStateSerializesToNothing() throws IOException {
        byte[] content = StateJsonBinder.serialize(new TestJsonState());

        assertThat(new String(content, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("{}");
        assertThat(StateJsonBinder.isEmptyState(content)).isTrue();
    }

    @Test
    public void onlyNonDefaultValuesAreWritten() throws IOException {
        TestJsonState state = new TestJsonState();
        state.limit = 9;

        byte[] content = StateJsonBinder.serialize(state);

        assertThat(new String(content, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("{\"limit\":9}");
        assertThat(StateJsonBinder.isEmptyState(content)).isFalse();
    }

    @Test
    public void javaDefaultDifferingFromFieldDefaultIsWritten() throws IOException {
        TestJsonState state = new TestJsonState();
        state.enabled = false;
        state.limit = 0;
        state.name = null;

        byte[] content = StateJsonBinder.serialize(state);
        TestJsonState back = StateJsonBinder.deserialize(TestJsonState.class, content);

        assertThat(back.enabled).isFalse();
        assertThat(back.limit).isZero();
        assertThat(back.name).isNull();
    }

    @Test
    public void noFalseOrNullStubsAreWritten() throws IOException {
        assertThat(new String(StateJsonBinder.serialize(new TestPlainState()), java.nio.charset.StandardCharsets.UTF_8))
            .isEqualTo("{}");

        TestPlainState state = new TestPlainState();
        state.flag = true;

        assertThat(new String(StateJsonBinder.serialize(state), java.nio.charset.StandardCharsets.UTF_8))
            .isEqualTo("{\"flag\":true}");

        TestPlainState withText = new TestPlainState();
        withText.text = "x";

        assertThat(new String(StateJsonBinder.serialize(withText), java.nio.charset.StandardCharsets.UTF_8))
            .isEqualTo("{\"text\":\"x\"}");
    }

    @Test
    public void annotatedStateIsJsonCapable() {
        assertThat(StateJsonBinder.isJsonCapable(ThreeState.class)).isTrue();
    }

    @Test
    public void plainStateIsNotJsonCapable() {
        assertThat(StateJsonBinder.isJsonCapable(PlainState.class)).isFalse();
        assertThat(StateJsonBinder.isJsonCapable(String.class)).isFalse();
    }

    @Test
    public void envelopeIsSortedAndRoundTrips() throws IOException {
        java.util.Map<String, byte[]> states = new java.util.LinkedHashMap<>();
        states.put("Zeta", "{\"a\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        states.put("Alpha", "{\"b\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        byte[] envelope = StateJsonBinder.writeEnvelope(states);
        assertThat(new String(envelope, java.nio.charset.StandardCharsets.UTF_8))
            .isEqualTo("{\"Alpha\":{\"b\":true},\"Zeta\":{\"a\":1}}");

        java.util.Map<String, byte[]> back = StateJsonBinder.readEnvelope(envelope);
        assertThat(back.keySet()).containsExactlyInAnyOrder("Alpha", "Zeta");
        assertThat(new String(back.get("Zeta"), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
    }

    @Test
    public void envelopeIsPrettyPrinted() throws IOException {
        java.util.Map<String, byte[]> states = new java.util.LinkedHashMap<>();
        states.put("Comp", "{\"a\":1,\"b\":{\"c\":[1,2]}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String pretty = new String(StateJsonBinder.prettyPrint(StateJsonBinder.writeEnvelope(states)), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(pretty).contains("\n");
        assertThat(StateJsonBinder.readEnvelope(pretty.getBytes(java.nio.charset.StandardCharsets.UTF_8)).keySet()).containsExactly("Comp");
        System.out.println(pretty);
    }

    @Test
    public void emptyEnvelope() throws IOException {
        assertThat(StateJsonBinder.readEnvelope(new byte[0])).isEmpty();
        assertThat(new String(StateJsonBinder.writeEnvelope(java.util.Map.of()), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("{}");
    }

    @Test
    public void substituteTouchesOnlyStringValues() throws IOException {
        byte[] envelope = ("{\"Comp\":{\"path\":\"/home/u/proj/a\",\"count\":42,\"flag\":true,\"none\":null,"
            + "\"nested\":{\"p\":\"/home/u/proj/b\"},\"list\":[\"/home/u/proj/c\",7]}}")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] collapsed = StateJsonBinder.substituteStrings(envelope, it -> it.replace("/home/u/proj", "$PROJECT_DIR$"));

        assertThat(new String(collapsed, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(
            "{\"Comp\":{\"path\":\"$PROJECT_DIR$/a\",\"count\":42,\"flag\":true,\"none\":null,"
                + "\"nested\":{\"p\":\"$PROJECT_DIR$/b\"},\"list\":[\"$PROJECT_DIR$/c\",7]}}");

        byte[] expanded = StateJsonBinder.substituteStrings(collapsed, it -> it.replace("$PROJECT_DIR$", "/home/u/proj"));
        assertThat(expanded).isEqualTo(envelope);
    }

    @Test
    public void substituteKeepsComponentNames() throws IOException {
        byte[] envelope = "{\"$PROJECT_DIR$\":{\"v\":\"x\"}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = StateJsonBinder.substituteStrings(envelope, it -> "REPLACED");
        assertThat(new String(result, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("{\"$PROJECT_DIR$\":{\"REPLACED\":\"REPLACED\"}}");
    }

    @Test
    public void substituteReachesMapKeysLikeXmlAttributes() throws IOException {
        byte[] envelope = "{\"Comp\":{\"paths\":{\"/home/u/proj/a\":\"v\"}}}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        byte[] collapsed = StateJsonBinder.substituteStrings(envelope, it -> it.replace("/home/u/proj", "$PROJECT_DIR$"));

        assertThat(new String(collapsed, java.nio.charset.StandardCharsets.UTF_8))
            .isEqualTo("{\"Comp\":{\"paths\":{\"$PROJECT_DIR$/a\":\"v\"}}}");
    }

    @Test
    public void annotatedStateRoundTrips() throws IOException {
        byte[] content = StateJsonBinder.serialize(ThreeState.UNSURE);

        assertThat(new String(content, StandardCharsets.UTF_8)).isEqualTo("\"UNSURE\"");
        assertThat(StateJsonBinder.deserialize(ThreeState.class, content)).isEqualTo(ThreeState.UNSURE);
    }
}
