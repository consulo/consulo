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
package consulo.ui;

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * What a component shows in place of content it has none of - the grey prompt of an empty text box, and the line a
 * collection puts in the middle of itself while it holds nothing.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public interface HasPlaceholder {
    void setPlaceholder(LocalizeValue text);

    @Deprecated
    default void setPlaceholder(@Nullable String text) {
        setPlaceholder(LocalizeValue.ofNullable(text));
    }
}
