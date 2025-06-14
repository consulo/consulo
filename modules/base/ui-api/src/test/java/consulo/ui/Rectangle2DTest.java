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
import static consulo.ui.Rectangle2DAssert.assertThat;

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
        assertThat(rect.contains(new Rectangle2D(1, 22, 0, 0))).isTrue();
        assertThat(rect.contains(new Rectangle2D(11, 2, 0, 0))).isTrue();
        assertThat(rect.contains(new Rectangle2D(11, 22, 0, 0))).isTrue();

        assertThat(rect.contains(new Rectangle2D(1, 1, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(0, 2, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(12, 2, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(1, 23, 0, 0))).isFalse();

        assertThat(rect.contains(new Rectangle2D(0, 0, 0, 0))).isFalse();
        assertThat(rect.contains(new Rectangle2D(12, 23, 0, 0))).isFalse();
    }

    @Test
    void testIntersects() {
        assertThat(new Rectangle2D(1, 2, 10, -1).intersects(new Rectangle2D(1, 2, 10, -1))).isFalse();
        assertThat(new Rectangle2D(1, 2, 10, 0).intersects(new Rectangle2D(1, 2, 10, 0))).isFalse();
        assertThat(new Rectangle2D(1, 2, -1, 20).intersects(new Rectangle2D(1, 2, -1, 20))).isFalse();
        assertThat(new Rectangle2D(1, 2, 0, 20).intersects(new Rectangle2D(1, 2, 0, 20))).isFalse();

        Rectangle2D rect = new Rectangle2D(1, 2, 10, 20);
        assertThat(rect.intersects(rect)).isTrue();

        assertThat(rect.intersects(new Rectangle2D(5, 5, 0, 0))).isFalse();
        assertThat(rect.intersects(new Rectangle2D(5, 5, 10, 0))).isFalse();
        assertThat(rect.intersects(new Rectangle2D(5, 5, 0, 20))).isFalse();
        assertThat(rect.intersects(new Rectangle2D(5, 5, 10, 20))).isTrue();
        assertThat(rect.intersects(new Rectangle2D(5, 5, -1, 0))).isFalse();
        assertThat(rect.intersects(new Rectangle2D(5, 5, 0, -1))).isFalse();

        assertThat(rect.intersects(new Rectangle2D(0, 1, 1, 1))).isFalse();
        assertThat(rect.intersects(new Rectangle2D(0, 22, 1, 1))).isFalse();
        assertThat(rect.intersects(new Rectangle2D(11, 1, 1, 1))).isFalse();
        assertThat(rect.intersects(new Rectangle2D(11, 22, 1, 1))).isFalse();

        assertThat(rect.intersects(new Rectangle2D(0, 1, 2, 2))).isTrue();
        assertThat(rect.intersects(new Rectangle2D(0, 21, 2, 2))).isTrue();
        assertThat(rect.intersects(new Rectangle2D(10, 1, 2, 2))).isTrue();
        assertThat(rect.intersects(new Rectangle2D(10, 21, 2, 2))).isTrue();
    }

    @Test
    void testIntersection() {
        assertThat(new Rectangle2D(1, 2, 10, -1).intersection(new Rectangle2D(1, 2, 10, -1))).isEmpty();
        assertThat(new Rectangle2D(1, 2, 10, 0).intersection(new Rectangle2D(1, 2, 10, 0))).isEmpty();
        assertThat(new Rectangle2D(1, 2, -1, 20).intersection(new Rectangle2D(1, 2, -1, 20))).isEmpty();
        assertThat(new Rectangle2D(1, 2, 0, 20).intersection(new Rectangle2D(1, 2, 0, 20))).isEmpty();

        Rectangle2D rect = new Rectangle2D(1, 2, 10, 20);
        assertThat(rect.intersection(rect)).isEqualTo(rect);

        assertThat(rect.intersection(new Rectangle2D(5, 5, 0, 0))).isEmpty();
        assertThat(rect.intersection(new Rectangle2D(5, 5, 10, 0))).isEmpty();
        assertThat(rect.intersection(new Rectangle2D(5, 5, 0, 20))).isEmpty();
        assertThat(rect.intersection(new Rectangle2D(5, 5, 10, 20))).is(5, 5, 10 - (5 - 1), 20 - (5 - 2));
        assertThat(rect.intersection(new Rectangle2D(5, 5, -1, 0))).isEmpty();
        assertThat(rect.intersection(new Rectangle2D(5, 5, 0, -1))).isEmpty();

        assertThat(rect.intersection(new Rectangle2D(0, 1, 1, 1))).isEmpty();
        assertThat(rect.intersection(new Rectangle2D(0, 22, 1, 1))).isEmpty();
        assertThat(rect.intersection(new Rectangle2D(11, 1, 1, 1))).isEmpty();
        assertThat(rect.intersection(new Rectangle2D(11, 22, 1, 1))).isEmpty();

        assertThat(rect.intersection(new Rectangle2D(0, 1, 2, 2))).is(1, 2, 1, 1);
        assertThat(rect.intersection(new Rectangle2D(0, 21, 2, 2))).is(1, 21, 1, 1);
        assertThat(rect.intersection(new Rectangle2D(10, 1, 2, 2))).is(10, 2, 1, 1);
        assertThat(rect.intersection(new Rectangle2D(10, 21, 2, 2))).is(10, 21, 1, 1);
    }

    @Test
    void testTranslate() {
        assertThat(new Rectangle2D(1, 2, 10, 20).translate(-1, -2)).is(0, 0, 10, 20);
    }

    @Test
    void testTranslateToFit() {
        Rectangle2D rect = new Rectangle2D(1, 2, 10, 20);
        assertThat(rect.translateToFit(rect)).isEqualTo(rect);

        assertThat(new Rectangle2D(0, 0, 10, 20).translateToFit(rect)).isEqualTo(rect);
        assertThat(new Rectangle2D(12, 23, 10, 20).translateToFit(rect)).isEqualTo(rect);

        assertThat(new Rectangle2D(0, 0, 5, 5).translateToFit(rect)).is(1, 2, 5, 5);
        assertThat(new Rectangle2D(12, 23, 5, 5).translateToFit(rect)).is(6, 17, 5, 5);

        assertThat(new Rectangle2D(0, 0, 20, 30).translateToFit(rect)).is(1, 2, 20, 30);
        assertThat(new Rectangle2D(12, 23, 20, 30).translateToFit(rect)).is(1, 2, 20, 30);
    }
}
