/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.fileEditor.impl.text;

import consulo.annotation.component.ServiceImpl;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.TextEditorWithPreview;
import consulo.fileEditor.TextEditorWithPreviewFactory;
import consulo.ui.ex.action.ActionToolbar;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2024-09-13
 */
@ServiceImpl
@Singleton
public class DesktopAWTTextEditorWithPreviewFactory implements TextEditorWithPreviewFactory {
    
    @Override
    public TextEditorWithPreview create(TextEditor editor,
                                        FileEditor preview,
                                        @Nullable ActionToolbar leftToolbarActionToolbar,
                                        String editorName) {
        return new TextEditorWithPreviewImpl(editor, preview, leftToolbarActionToolbar, editorName);
    }
}
