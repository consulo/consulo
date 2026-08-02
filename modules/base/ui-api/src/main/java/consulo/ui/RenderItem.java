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

import org.jspecify.annotations.Nullable;

/**
 * Item handed to a render, together with the per row state it needs. Never {@code null}, even when
 * the value is, so state is always reachable.
 * <p/>
 * Extra per row state is added here rather than as another render parameter, which would break the
 * lambda form of every render.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface RenderItem<E> {
    static <E> RenderItem<E> of(@Nullable E value, boolean selected) {
        return new RenderItem<>() {
            @Override
            public @Nullable E getValue() {
                return value;
            }

            @Override
            public boolean isSelected() {
                return selected;
            }
        };
    }

    @Nullable
    E getValue();

    boolean isSelected();
}
