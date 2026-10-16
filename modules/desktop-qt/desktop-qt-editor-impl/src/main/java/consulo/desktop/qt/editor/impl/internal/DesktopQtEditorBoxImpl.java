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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.EditorFactory;
import consulo.language.editor.ui.internal.EditorBoxOptions;
import consulo.project.ProjectManager;
import io.qt.widgets.QWidget;

class DesktopQtEditorBoxImpl extends DesktopQtEditorBoxBase<DesktopQtEditorBoxFrame> {
    DesktopQtEditorBoxImpl(EditorBoxOptions options, EditorFactory editorFactory, ProjectManager projectManager) {
        super(options, editorFactory, projectManager);
    }

    @Override
    protected DesktopQtEditorBoxFrame createHost(QWidget parent) {
        return new DesktopQtEditorBoxFrame(parent);
    }
}
