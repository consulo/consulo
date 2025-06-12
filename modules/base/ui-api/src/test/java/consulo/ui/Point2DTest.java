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
public class Point2DTest {
    @Test
    void testCreation() {
        Point2D point = new Point2D(1, 2);
        assertThat(point.x()).isEqualTo(1);
        assertThat(point.y()).isEqualTo(2);

        assertThat(new Point2D()).isEqualTo(new Point2D(0, 0));

        assertThat(point.withX(10)).isEqualTo(new Point2D(10, 2));
        assertThat(point.withY(20)).isEqualTo(new Point2D(1, 20));
    }

    @Test
    void testAdd() {
        assertThat(new Point2D(10, 20).add(1, 2)).isEqualTo(new Point2D(11, 22));
    }
}
