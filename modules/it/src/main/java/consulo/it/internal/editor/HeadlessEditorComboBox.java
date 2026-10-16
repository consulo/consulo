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
package consulo.it.internal.editor;

import consulo.document.Document;
import consulo.language.editor.ui.EditorComboBox;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class HeadlessEditorComboBox extends HeadlessEditorBox implements EditorComboBox {
    private List<String> myItems = List.of();

    public HeadlessEditorComboBox(@Nullable Project project, Document document, FileType fileType, LocalizeValue placeholder) {
        super(project, document, fileType, placeholder);
    }

    @Override
    public List<String> getItems() {
        return myItems;
    }

    @RequiredUIAccess
    @Override
    public void setItems(List<String> items) {
        myItems = List.copyOf(items);
    }

    @Override
    public @Nullable String getSelectedItem() {
        String value = getValue();
        return myItems.contains(value) ? value : null;
    }

    @RequiredUIAccess
    @Override
    public void setSelectedItem(@Nullable String item) {
        setValue(item);
    }
}
