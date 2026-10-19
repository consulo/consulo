/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.diff.DiffContext;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.FileContent;
import consulo.diff.internal.DiffImplUtil;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.UIBasedFileType;
import org.jspecify.annotations.Nullable;

public class BinaryEditorHolder extends EditorHolder {
    protected final FileEditor myEditor;
    protected final FileEditorProvider myEditorProvider;

    public BinaryEditorHolder(FileEditor editor, FileEditorProvider editorProvider) {
        myEditor = editor;
        myEditorProvider = editorProvider;
    }

    public FileEditor getEditor() {
        return myEditor;
    }

    @Override
    public void dispose() {
        myEditorProvider.disposeEditor(myEditor);
    }

    @Override
    public Component getComponent() {
        return myEditor.getUIComponent();
    }

    @Override
    public void installFocusListener(Runnable onFocusGained) {
        if (myEditor.getUIComponent() instanceof HasFocus focusable) {
            focusable.addFocusListener(event -> onFocusGained.run());
        }
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myEditor.getPreferredFocusedUIComponent();
    }

    //
    // Build
    //

    public static class BinaryEditorHolderFactory extends EditorHolderFactory<BinaryEditorHolder> {
        public static BinaryEditorHolderFactory INSTANCE = new BinaryEditorHolderFactory();

        @Override
        @RequiredUIAccess
        public BinaryEditorHolder create(DiffContent content, DiffContext context) {
            Project project = context.getProject();
            if (content instanceof FileContent fileContent) {
                if (project == null) {
                    project = ProjectManager.getInstance().getDefaultProject();
                }
                VirtualFile file = fileContent.getFile();

                FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(project, file);
                if (providers.length == 0) {
                    throw new IllegalStateException("Can't find FileEditorProvider: " + file.getFileType());
                }

                FileEditorProvider provider = providers[0];
                FileEditor editor = provider.createEditor(project, file);

                return new BinaryEditorHolder(editor, provider);
            }
            if (content instanceof DocumentContent documentContent) {
                Document document = documentContent.getDocument();
                Editor editor = DiffImplUtil.createEditor(document, project, true);

                TextEditorProvider provider = TextEditorProvider.getInstance();
                TextEditor fileEditor = provider.getTextEditor(editor);

                Disposer.register(fileEditor, () -> EditorFactory.getInstance().releaseEditor(editor));

                return new BinaryEditorHolder(fileEditor, provider);
            }

            throw new IllegalArgumentException(content.getClass() + " - " + content.toString());
        }

        @Override
        public boolean canShowContent(DiffContent content, DiffContext context) {
            if (content instanceof DocumentContent) {
                return true;
            }
            if (content instanceof FileContent fileContent) {
                Project project = context.getProject();
                if (project == null) {
                    project = ProjectManager.getInstance().getDefaultProject();
                }
                VirtualFile file = fileContent.getFile();

                return FileEditorProviderManager.getInstance().getProviders(project, file).length != 0;
            }
            return false;
        }

        @Override
        public boolean wantShowContent(DiffContent content, DiffContext context) {
            if (content instanceof FileContent) {
                if (content.getContentType() == null) {
                    return false;
                }
                if (content.getContentType().isBinary()) {
                    return true;
                }
                if (content.getContentType() instanceof UIBasedFileType) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }
}
