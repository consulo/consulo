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
package consulo.desktop.qt.editor.impl;

import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorFoldingModelBase;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtCodeEditorFoldingModelImpl extends CodeEditorFoldingModelBase {
    public DesktopQtCodeEditorFoldingModelImpl(CodeEditorBase editor) {
        super(editor);
    }

    @Override
    protected void notifyBatchFoldingProcessingDoneToEditor() {
        DesktopQtEditorImpl editor = (DesktopQtEditorImpl) myEditor;

        DesktopQtEditorWidget widget = editor.getSurface();
        if (widget == null) {
            return;
        }

        // folding moves which row a mark is shown on, and the gutter keyed its icons by row
        ((DesktopQtEditorGutterComponentImpl) editor.getGutterComponentEx()).dropRenderersCache();

        widget.updateSideAreas();
        widget.updateScrollRanges();
        widget.viewport().update();
        widget.getGutter().update();
    }
}
