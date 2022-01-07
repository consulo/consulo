// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EmptyEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.impl.AbstractFileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.messages.MessageBusConnection;
import consulo.disposer.Disposable;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    connection.subscribe(FileTypeManager.TOPIC, new MyFileTypeListener());
    connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
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
            .finishOnUiThread(ModalityState.any(), myEditor::setHighlighter)
            .submit(NonUrgentExecutor.getInstance());
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
  public static void completeAsyncTasks() {
    NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
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
