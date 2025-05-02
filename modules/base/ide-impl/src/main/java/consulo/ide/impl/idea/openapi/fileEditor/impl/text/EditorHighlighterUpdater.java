// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileEditor.impl.text;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.application.impl.NonBlockingReadActionImpl;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.project.Project;
import consulo.project.event.DumbModeListener;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * @author peter
 */
public class EditorHighlighterUpdater {
  @Nonnull
  protected final Project myProject;
  @Nonnull
  private final EditorEx myEditor;
  @Nullable
  private final VirtualFile myFile;

  public EditorHighlighterUpdater(@Nonnull Project project, @Nonnull Disposable parentDisposable, @Nonnull EditorEx editor, @Nullable VirtualFile file) {
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

  @Nonnull
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

  @TestOnly
  public static void completeAsyncTasks(Application application) {
    NonBlockingReadActionImpl.waitForAsyncTaskCompletion(application);
  }

  /**
   * Listen changes of file types. When type of the file changes we need
   * to also change highlighter.
   */
  private final class MyFileTypeListener implements FileTypeListener {
    @Override
    public void fileTypesChanged(@Nonnull final FileTypeEvent event) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      // File can be invalid after file type changing. The editor should be removed
      // by the FileEditorManager if it's invalid.
      FileType type = event.getRemovedFileType();
      if (type != null && !(type instanceof AbstractFileType)) {
        // Plugin is being unloaded, so we need to release plugin classes immediately
        updateHighlightersSynchronously();
      }
      else {
        updateHighlighters();
      }
    }
  }
}
