// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.impl.internal.text;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.language.custom.CustomSyntaxTableFileType;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import org.jspecify.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * @author peter
 */
public class EditorHighlighterUpdater {
  
  protected final Project myProject;
  
  private final EditorEx myEditor;
  private final @Nullable VirtualFile myFile;

  public EditorHighlighterUpdater(Project project, Disposable parentDisposable, EditorEx editor, @Nullable VirtualFile file) {
    myProject = project;
    myEditor = editor;
    myFile = file;
    MessageBusConnection connection = project.getMessageBus().connect(parentDisposable);
    connection.subscribe(FileTypeListener.class, new MyFileTypeListener());
    connection.subscribe(DumbModeListener.class, new DumbModeListener() {
      @Override
      public void enteredDumbMode() {
        updateHighlighters();
      }

      @Override
      public void exitDumbMode() {
        updateHighlighters();
      }
    });
  }

  public void updateHighlightersAsync() {
    ReadAction.nonBlocking(this::createHighlighter)
            .expireWith(myProject)
            .expireWhen(() -> (myFile != null && !myFile.isValid()) || myEditor.isDisposed())
            .coalesceBy(EditorHighlighterUpdater.class, myEditor)
            .finishOnUiThread(Application::getAnyModalityState, myEditor::setHighlighter)
            .submitDefault();
  }

  
  protected EditorHighlighter createHighlighter() {
    EditorHighlighter highlighter = myFile != null
                                    ? EditorHighlighterFactory.getInstance().createEditorHighlighter(myProject, myFile)
                                    : new EmptyEditorHighlighter(EditorColorsManager.getInstance().getGlobalScheme().getAttributes(HighlighterColors.TEXT));
    highlighter.setText(myEditor.getDocument().getImmutableCharSequence());
    return highlighter;
  }

  /**
   * Updates editors' highlighters. This should be done when the opened file
   * changes its file type.
   */
  public void updateHighlighters() {
    if (!myProject.isDisposed() && !myEditor.isDisposed()) {
      updateHighlightersAsync();
    }
  }

  private void updateHighlightersSynchronously() {
    if (!myProject.isDisposed() && !myEditor.isDisposed()) {
      myEditor.setHighlighter(createHighlighter());
    }
  }

  /**
   * Listen changes of file types. When type of the file changes we need
   * to also change highlighter.
   */
  private final class MyFileTypeListener implements FileTypeListener {
    @Override
    @RequiredUIAccess
    public void fileTypesChanged(FileTypeEvent event) {
      UIAccess.assertIsUIThread();
      // File can be invalid after file type changing. The editor should be removed
      // by the FileEditorManager if it's invalid.
      FileType type = event.getRemovedFileType();
      if (type != null && !(type instanceof CustomSyntaxTableFileType)) {
        // Plugin is being unloaded, so we need to release plugin classes immediately
        updateHighlightersSynchronously();
      }
      else {
        updateHighlighters();
      }
    }
  }
}
