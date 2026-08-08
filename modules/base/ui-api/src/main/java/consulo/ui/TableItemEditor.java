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
import org.jspecify.annotations.Nullable;

/**
 * Contract of an editable column - which widget edits the cell and where the value goes. The editor
 * lifecycle stays with the backend, since that is where desktop and web genuinely differ.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public interface TableItemEditor<Item, Value> {
    @RequiredUIAccess
    ValueComponent<Value> createComponent(Item item);

    @RequiredUIAccess
    void commit(Item item, @Nullable Value value);

    default boolean isEditable(Item item) {
        return true;
    }
}
