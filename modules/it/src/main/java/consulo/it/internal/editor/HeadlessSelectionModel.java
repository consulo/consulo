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

import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.codeEditor.impl.CodeEditorSelectionModelBase;

/**
 * Selection model for the headless editor. The whole contract of {@link SelectionModel} is already
 * implemented by {@link CodeEditorSelectionModelBase} in terms of the caret model, so the real
 * selection state lives in the carets and nothing here is degenerate.
 *
 * @author VISTALL
 * @since 2026-09-26
 */
public class HeadlessSelectionModel extends CodeEditorSelectionModelBase implements SelectionModel {
    public HeadlessSelectionModel(CodeEditorBase editor) {
        super(editor);
    }
}
