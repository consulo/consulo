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
package consulo.desktop.awt.internal.diff;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.FileContent;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorProviderManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.diff.DiffContext;
import consulo.diff.internal.DiffImplUtil;
import consulo.virtualFileSystem.fileType.UIBasedFileType;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.ex.awt.UIUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.FocusListener;

public class BinaryEditorHolder extends EditorHolder {
  @Nonnull
  protected final FileEditor myEditor;
  @Nonnull
  protected final FileEditorProvider myEditorProvider;

  public BinaryEditorHolder(@Nonnull FileEditor editor, @Nonnull FileEditorProvider editorProvider) {
    myEditor = editor;
    myEditorProvider = editorProvider;
  }

  @Nonnull
  public FileEditor getEditor() {
    return myEditor;
  }

  @Override
  public void dispose() {
    myEditorProvider.disposeEditor(myEditor);
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myEditor.getComponent();
  }

  @Override
  public void installFocusListener(@Nonnull FocusListener listener) {
    myEditor.getComponent().addFocusListener(listener);
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myEditor.getPreferredFocusedComponent();
  }

  //
  // Build
  //

  public static class BinaryEditorHolderFactory extends EditorHolderFactory<BinaryEditorHolder> {
    public static BinaryEditorHolderFactory INSTANCE = new BinaryEditorHolderFactory();

    @Override
    @Nonnull
    public BinaryEditorHolder create(@Nonnull DiffContent content, @Nonnull DiffContext context) {
      Project project = context.getProject();
      if (content instanceof FileContent) {
        if (project == null) project = ProjectManager.getInstance().getDefaultProject();
        VirtualFile file = ((FileContent)content).getFile();

        FileEditorProvider[] providers = FileEditorProviderManager.getInstance().getProviders(project, file);
        if (providers.length == 0) throw new IllegalStateException("Can't find FileEditorProvider: " + file.getFileType());

        FileEditorProvider provider = providers[0];
        FileEditor editor = provider.createEditor(project, file);

        UIUtil.removeScrollBorder(editor.getComponent());

        return new BinaryEditorHolder(editor, provider);
      }
      if (content instanceof DocumentContent) {
        Document document = ((DocumentContent)content).getDocument();
        final Editor editor = DiffImplUtil.createEditor(document, project, true);

        TextEditorProvider provider = TextEditorProvider.getInstance();
        TextEditor fileEditor = provider.getTextEditor(editor);

        Disposer.register(fileEditor, new Disposable() {
          @Override
          public void dispose() {
            EditorFactory.getInstance().releaseEditor(editor);
          }
        });

        return new BinaryEditorHolder(fileEditor, provider);
      }

      throw new IllegalArgumentException(content.getClass() + " - " + content.toString());
    }

    @Override
    public boolean canShowContent(@Nonnull DiffContent content, @Nonnull DiffContext context) {
      if (content instanceof DocumentContent) return true;
      if (content instanceof FileContent) {
        Project project = context.getProject();
        if (project == null) project = ProjectManager.getInstance().getDefaultProject();
        VirtualFile file = ((FileContent)content).getFile();

        return FileEditorProviderManager.getInstance().getProviders(project, file).length != 0;
      }
      return false;
    }

    @Override
    public boolean wantShowContent(@Nonnull DiffContent content, @Nonnull DiffContext context) {
      if (content instanceof FileContent) {
        if (content.getContentType() == null) return false;
        if (content.getContentType().isBinary()) return true;
        if (content.getContentType() instanceof UIBasedFileType) return true;
        return false;
      }
      return false;
    }
  }
}
