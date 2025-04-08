/*
 * Copyright 2013-2025 consulo.io
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
package consulo.component.extension;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2025-04-08
 */
public class ExtensionPointTest {
    @Test
    void emptyExtensionStreamTest() {
        ExtensionStream<Object> emptyStream = ExtensionStream.empty();
        Consumer<Object> doNothing = obj -> {
        };

        assertThat(emptyStream.map(Function.identity())).isSameAs(emptyStream);
        assertThat(emptyStream.mapNonnull(Function.identity())).isSameAs(emptyStream);
        assertThat(emptyStream.filter(obj -> true)).isSameAs(emptyStream);
        assertThat(emptyStream.distinct()).isSameAs(emptyStream);
        assertThat(emptyStream.peek(doNothing)).isSameAs(emptyStream);

        assertThat(emptyStream.anyMatch(obj -> true)).isFalse();
        assertThat(emptyStream.findFirst()).isEmpty();
        assertThat(emptyStream.toList()).isEmpty();

        emptyStream.forEach(doNothing);
    }

    @Test
    void extensionStreamTest() {
        List<Integer> values = List.of(1);
        Consumer<Integer> doNothing = obj -> {
        };

        assertThat(of(values).map(Function.identity()).toList()).isEqualTo(values);
        assertThat(of(values).mapNonnull(Function.identity()).toList()).isEqualTo(values);
        assertThat(of(values).filter(obj -> true).toList()).isEqualTo(values);
        assertThat(of(values).peek(doNothing).toList()).isEqualTo(values);

        assertThat(of(values).anyMatch(obj -> true)).isTrue();
        assertThat(of(values).findFirst()).hasValue(1);
    }

    private <T> ExtensionStream<T> of(List<T> values) {
        return ExtensionStream.of(values.stream());
    }
}
