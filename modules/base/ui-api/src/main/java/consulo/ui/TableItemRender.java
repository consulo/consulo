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

import consulo.ui.annotation.RequiredUIAccess;

/**
 * Render twin of {@link TableItemEditor} - a cell drawn from the row it belongs to, not from the column
 * value alone. A column whose look follows something the value does not carry (the kind of row it is, an
 * error the row holds) cannot answer from {@link TextItemRender}, which only ever sees the value.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
@FunctionalInterface
public interface TableItemRender<Item, Value> {
    static <Item, Value> TableItemRender<Item, Value> of(TextItemRender<Value> render) {
        return (presentation, value, item) -> render.render(presentation, value);
    }

    @RequiredUIAccess
    void render(TextItemPresentation presentation, RenderItem<Value> value, Item item);
}
