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
public class Rectangle2DTest {
    @Test
    void testCreation() {
        Rectangle2D rect = new Rectangle2D(1, 2, 10, 20);
        assertThat(rect.minX()).isEqualTo(1);
        assertThat(rect.minY()).isEqualTo(2);
        assertThat(rect.width()).isEqualTo(10);
        assertThat(rect.height()).isEqualTo(20);
        assertThat(rect.maxX()).isEqualTo(11);
        assertThat(rect.maxY()).isEqualTo(22);
        assertThat(rect.minPoint()).isEqualTo(new Point2D(1, 2));
        assertThat(rect.maxPoint()).isEqualTo(new Point2D(11, 22));

        assertThat(new Rectangle2D(10, 20)).isEqualTo(new Rectangle2D(0, 0, 10, 20));

        assertThat(new Rectangle2D(new Size2D(10, 20))).isEqualTo(new Rectangle2D(0, 0, 10, 20));

        assertThat(new Rectangle2D(new Point2D(1, 2), new Size2D(10, 20))).isEqualTo(new Rectangle2D(1, 2, 10, 20));
    }

    @Test
    void testIsEmpty() {
        assertThat(new Rectangle2D(1, 2, 10, 20).isEmpty()).isFalse();
        assertThat(new Rectangle2D(1, 2, 0, 20).isEmpty()).isTrue();
        assertThat(new Rectangle2D(1, 2, -1, 20).isEmpty()).isTrue();
        assertThat(new Rectangle2D(1, 2, 10, 0).isEmpty()).isTrue();
        assertThat(new Rectangle2D(1, 2, 10, -1).isEmpty()).isTrue();
        assertThat(new Rectangle2D(1, 2, 0, 0).isEmpty()).isTrue();
        assertThat(new Rectangle2D(1, 2, -1, -1).isEmpty()).isTrue();
    }

    @Test
    void testContainsPoint() {
        Rectangle2D rect = new Rectangle2D(1, 2, 10, 20);
        assertThat(rect.contains(new Point2D(5, 5))).isTrue();

        assertThat(rect.contains(new Point2D(1, 2))).isTrue();
        assertThat(rect.contains(new Point2D(1, 11))).isTrue();
        assertThat(rect.contains(new Point2D(11, 2))).isTrue();
        assertThat(rect.contains(new Point2D(11, 22))).isTrue();

        assertThat(rect.contains(new Point2D(1, 1))).isFalse();
        assertThat(rect.contains(new Point2D(0, 2))).isFalse();
        assertThat(rect.contains(new Point2D(12, 2))).isFalse();
        assertThat(rect.contains(new Point2D(1, 23))).isFalse();

        assertThat(rect.contains(new Point2D(0, 0))).isFalse();
        assertThat(rect.contains(new Point2D(12, 23))).isFalse();
    }

    @Test
    void testContainsRect() {
        assertThat(new Rectangle2D(1, 2, 10, -1).contains(new Rectangle2D(1, 2, 10, -1))).isFalse();
        assertThat(new Rectangle2D(1, 2, -1, 20).contains(new Rectangle2D(1, 2, -1, 20))).isFalse();

        Rectangle2D rect = new Rectangle2D(1, 2, 10, 20);
        assertThat(rect.contains(rect)).isTrue();

        assertThat(rect.contains(new Rectangle2D(5, 5, 0, 0))).isTrue();
        assertThat(rect.contains(new Rectangle2D(5, 5, 10, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(5, 5, 0, 20))).isFalse();
        assertThat(rect.contains(new Rectangle2D(5, 5, 10, 20))).isFalse();
        assertThat(rect.contains(new Rectangle2D(5, 5, -1, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(5, 5, 0, -1))).isFalse();

        assertThat(rect.contains(new Rectangle2D(1, 2, 0, 0))).isTrue();
        assertThat(rect.contains(new Rectangle2D(1, 11, 0, 0))).isTrue();
        assertThat(rect.contains(new Rectangle2D(11, 2, 0, 0))).isTrue();
        assertThat(rect.contains(new Rectangle2D(11, 22, 0, 0))).isTrue();

        assertThat(rect.contains(new Rectangle2D(1, 1, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(0, 2, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(12, 2, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(1, 23, 0, 0))).isFalse();

        assertThat(rect.contains(new Rectangle2D(0, 0, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(12, 23, 0, 0))).isFalse();
    }
}
