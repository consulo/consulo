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
package consulo.desktop.awt.ui;


import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author UNV
 * @since 2025-06-10
 */
public class ImmutableInsetsTest {
    @Test
    @SuppressWarnings("UseDPIAwareInsets")
    void testCreation() {
        ImmutableInsets insets = new ImmutableInsets(1, 2, 3, 4);
        assertThat(insets.top()).isEqualTo(1);
        assertThat(insets.right()).isEqualTo(2);
        assertThat(insets.bottom()).isEqualTo(3);
        assertThat(insets.left()).isEqualTo(4);

        assertThat(new ImmutableInsets(1, 2)).isEqualTo(new ImmutableInsets(1, 2, 1, 2));
        assertThat(ImmutableInsets.of(1, 2)).isEqualTo(new ImmutableInsets(1, 2, 1, 2));

        assertThat(new ImmutableInsets(1)).isEqualTo(new ImmutableInsets(1, 1, 1, 1));
        assertThat(ImmutableInsets.of(1)).isEqualTo(new ImmutableInsets(1, 1, 1, 1));

        assertThat(ImmutableInsets.top(1)).isEqualTo(new ImmutableInsets(1, 0, 0, 0));
        assertThat(ImmutableInsets.right(1)).isEqualTo(new ImmutableInsets(0, 1, 0, 0));
        assertThat(ImmutableInsets.bottom(1)).isEqualTo(new ImmutableInsets(0, 0, 1, 0));
        assertThat(ImmutableInsets.left(1)).isEqualTo(new ImmutableInsets(0, 0, 0, 1));

        assertThat(ImmutableInsets.of(new Insets(1, 2, 3, 4))).isEqualTo(new ImmutableInsets(1, 4, 3, 2));
    }

    @Test
    void testAddToSize() {
        assertThat(new ImmutableInsets(1, 2, 3, 4).addTo(new Size2D(10, 20)))
            .isEqualTo(new Size2D(16, 24));
    }

    @Test
    void testRemoveFromSize() {
        assertThat(new ImmutableInsets(1, 2, 3, 4).removeFrom(new Size2D(10, 20)))
            .isEqualTo(new Size2D(4, 16));
    }

    @Test
    void testAddToRect() {
        assertThat(new ImmutableInsets(1, 2, 3, 4).addTo(new Rectangle2D(10, 20, 30, 40)))
            .isEqualTo(new Rectangle2D(6, 19, 36, 44));
    }

    @Test
    void testRemoveFromRect() {
        assertThat(new ImmutableInsets(1, 2, 3, 4).removeFrom(new Rectangle2D(10, 20, 30, 40)))
            .isEqualTo(new Rectangle2D(14, 21, 24, 36));
    }
}
