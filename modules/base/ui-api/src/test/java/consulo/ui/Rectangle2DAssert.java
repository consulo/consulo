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

import org.assertj.core.api.AbstractAssert;

/**
 * @author UNV
 * @since 2025-06-13
 */
public class Rectangle2DAssert extends AbstractAssert<Rectangle2DAssert, Rectangle2D> {
    private Rectangle2DAssert(Rectangle2D rectangle2D) {
        super(rectangle2D, Rectangle2DAssert.class);
    }

    public void isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected %s to be empty", actual);
        }
    }

    public Rectangle2DAssert is(int x, int y, int width, int height) {
        return isEqualTo(new Rectangle2D(x, y, width, height));
    }

    public static Rectangle2DAssert assertThat(Rectangle2D rect) {
        return new Rectangle2DAssert(rect);
    }
}
