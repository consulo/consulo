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
package consulo.language.editor.ui;

import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An {@link EditorBox} with a drop-down list of items. Choosing an item replaces the editor text with it.
 */
public interface EditorComboBox extends EditorBox {
    List<String> getItems();

    @RequiredUIAccess
    void setItems(List<String> items);

    /**
     * @return the item equal to the current text, or {@code null} when the text matches no item
     */
    @Nullable String getSelectedItem();

    /**
     * Replaces the editor text with the given item, {@code null} clears the text
     */
    @RequiredUIAccess
    void setSelectedItem(@Nullable String item);
}
