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
package consulo.language.editor.ui.internal;

import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public record EditorBoxOptions(
    @Nullable Project project,
    Document document,
    FileType fileType,
    boolean oneLine,
    boolean viewer,
    boolean editorFont,
    LocalizeValue placeholder,
    List<Consumer<EditorEx>> customizations
) {
}
