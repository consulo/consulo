// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.highlighting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.Alarm;
import javax.annotation.Nonnull;

public class BraceHighlighter implements StartupActivity {
  private final Alarm myAlarm = new Alarm();

  @Override
  public void runActivity(@Nonnull final Project project) {
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) return; // sorry, upsource
    final EditorEventMulticaster eventMulticaster = EditorFactory.getInstance().getEventMulticaster();

    eventMulticaster.addCaretListener(new CaretListener() {
      @Override
      public void caretPositionChanged(@Nonnull CaretEvent e) {
        if (e.getCaret() != e.getEditor().getCaretModel().getPrimaryCaret()) return;
        myAlarm.cancelAllRequests();
        Editor editor = e.getEditor();
        final SelectionModel selectionModel = editor.getSelectionModel();
        // Don't update braces in case of the active selection.
        if (editor.getProject() != project || selectionModel.hasSelection()) {
          return;
        }
        updateBraces(editor, myAlarm);
      }
    }, project);

    final SelectionListener selectionListener = new SelectionListener() {
      @Override
      public void selectionChanged(@Nonnull SelectionEvent e) {
        myAlarm.cancelAllRequests();
        Editor editor = e.getEditor();
        if (editor.getProject() != project) {
          return;
        }

        final TextRange oldRange = e.getOldRange();
        final TextRange newRange = e.getNewRange();
        if (oldRange != null && newRange != null && oldRange.isEmpty() == newRange.isEmpty()) {
          // Don't perform braces update in case of active/absent selection.
          return;
        }
        updateBraces(editor, myAlarm);
      }
    };
    eventMulticaster.addSelectionListener(selectionListener, project);

    DocumentListener documentListener = new DocumentListener() {
      @Override
      public void documentChanged(@Nonnull DocumentEvent e) {
        myAlarm.cancelAllRequests();
        Editor[] editors = EditorFactory.getInstance().getEditors(e.getDocument(), project);
        for (Editor editor : editors) {
          updateBraces(editor, myAlarm);
        }
      }
    };
    eventMulticaster.addDocumentListener(documentListener, project);

    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@Nonnull FileEditorManagerEvent e) {
        myAlarm.cancelAllRequests();
        FileEditor oldEditor = e.getOldEditor();
        if (oldEditor instanceof TextEditor) {
          clearBraces(((TextEditor)oldEditor).getEditor());
        }
        FileEditor newEditor = e.getNewEditor();
        if (newEditor instanceof TextEditor) {
          updateBraces(((TextEditor)newEditor).getEditor(), myAlarm);
        }
      }
    });
  }

  static void updateBraces(@Nonnull final Editor editor, @Nonnull final Alarm alarm) {
    if (editor.getDocument().isInBulkUpdate()) return;

    BraceHighlightingHandler.lookForInjectedAndMatchBracesInOtherThread(editor, alarm, handler -> {
      handler.updateBraces();
      return false;
    });
  }

  private void clearBraces(@Nonnull final Editor editor) {
    BraceHighlightingHandler.lookForInjectedAndMatchBracesInOtherThread(editor, myAlarm, handler -> {
      handler.clearBraceHighlighters();
      return false;
    });
  }

  @Nonnull
  public static Alarm getAlarm() {
    return POST_STARTUP_ACTIVITY.findExtensionOrFail(BraceHighlighter.class).myAlarm;
  }
}
