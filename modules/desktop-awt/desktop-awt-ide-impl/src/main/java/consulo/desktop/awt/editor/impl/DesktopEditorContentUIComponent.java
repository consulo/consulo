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
package consulo.desktop.awt.editor.impl;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.action.EditorActionManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.PasteProvider;
import consulo.undoRedo.CommandProcessor;
import org.jspecify.annotations.Nullable;

/**
 * The editor content area as a real ui component - it owns the swing peer rather than wrapping one,
 * and carries the editor-level logic that needs no swing at all. The peer stays a paint and awt
 * protocol surface.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopEditorContentUIComponent extends SwingComponentDelegate<EditorComponentImpl> {
    private final DesktopEditorImpl myEditor;

    public DesktopEditorContentUIComponent(DesktopEditorImpl editor) {
        myEditor = editor;
    }

    @Override
    protected EditorComponentImpl createComponent() {
        return new EditorComponentImpl(myEditor, this);
    }

    @Override
    protected void init(EditorComponentImpl component) {
        super.init(component);

        // the peer answers this component via FromSwingComponentWrapper, and the data manager reads the key from here
        putUserData(UiDataProvider.KEY, this::uiDataSnapshot);
    }

    public DesktopEditorImpl getEditor() {
        return myEditor;
    }

    private void uiDataSnapshot(DataSink sink) {
        if (myEditor.isDisposed() || myEditor.isRendererMode()) {
            return;
        }

        sink.set(Editor.KEY, myEditor);
        sink.lazy(Caret.KEY, () -> myEditor.getCaretModel().getCurrentCaret());
        sink.lazy(DeleteProvider.KEY, () -> myEditor.getDeleteProvider());
        sink.lazy(CutProvider.KEY, () -> myEditor.getCutProvider());
        sink.lazy(CopyProvider.KEY, () -> myEditor.getCopyProvider());
        sink.lazy(PasteProvider.KEY, () -> myEditor.getPasteProvider());
        sink.lazy(EditorKeys.EDITOR_VIRTUAL_SPACE, () -> {
            LogicalPosition location = myEditor.myLastMousePressedLocation;
            if (location == null) {
                location = myEditor.getCaretModel().getLogicalPosition();
            }
            return EditorUtil.inVirtualSpace(myEditor, location);
        });
    }

    static void notSupported() {
        throw new RuntimeException("Not supported for this text implementation");
    }

    /**
     * Inserts, removes or replaces the given text at the given offset
     */
    @RequiredUIAccess
    void editDocumentSafely(int offset, int length, @Nullable String text) {
        Project project = myEditor.getProject();
        Document document = myEditor.getDocument();
        if (!FileDocumentManager.getInstance().requestWriting(document, project)) {
            return;
        }
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .document(document)
            .groupId(document)
            .inWriteAction()
            .run(() -> {
                document.startGuardedBlockChecking();
                try {
                    if (text == null) {
                        // remove
                        document.deleteString(offset, offset + length);
                    }
                    else if (length == 0) {
                        // insert
                        document.insertString(offset, text);
                    }
                    else {
                        document.replaceString(offset, offset + length, text);
                    }
                }
                catch (ReadOnlyFragmentModificationException e) {
                    EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(document).handle(e);
                }
                finally {
                    document.stopGuardedBlockChecking();
                }
            });
    }
}
