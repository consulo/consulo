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
package consulo.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2025-06-10
 */
public class Size2DTest {
    @Test
    void testCreation() {
        Size2D point = new Size2D(10, 20);
        assertThat(point.width()).isEqualTo(10);
        assertThat(point.height()).isEqualTo(20);

        assertThat(new Size2D(10)).isEqualTo(new Size2D(10, 10));
    }

    @Test
    void testIsEmpty() {
        assertThat(new Size2D(10, 20).isEmpty()).isFalse();
        assertThat(new Size2D(0, 20).isEmpty()).isTrue();
        assertThat(new Size2D(-1, 20).isEmpty()).isTrue();
        assertThat(new Size2D(10, 0).isEmpty()).isTrue();
        assertThat(new Size2D(10, -1).isEmpty()).isTrue();
        assertThat(new Size2D(0, 0).isEmpty()).isTrue();
        assertThat(new Size2D(-1, -1).isEmpty()).isTrue();
    }
}
