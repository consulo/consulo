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
package consulo.ui.impl.font;

import consulo.ui.font.Typeface;

/**
 * What every frontend answers with - the family and its pitch are all a {@link Typeface} carries, so the
 * record lives here rather than once per toolkit.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public record TypefaceImpl(String name, boolean monospaced) implements Typeface {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMonospaced() {
        return monospaced;
    }

    @Override
    public String toString() {
        return name;
    }
}
