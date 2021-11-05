// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.command.undo;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import javax.annotation.Nonnull;

public final class UndoUtil {
  private UndoUtil() {
  }

  /**
   * make undoable action in current document in order to Undo action work from current file
   *
   * @param file to make editors of to respond to undo action.
   */
  public static void markPsiFileForUndo(@Nonnull final PsiFile file) {
    Project project = file.getProject();
    final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    if (document == null) return;
    CommandProcessor.getInstance().addAffectedDocuments(project, document);
  }

  public static void disableUndoFor(@Nonnull Document document) {
    document.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
  }

  public static void disableUndoIn(@Nonnull Document document, @Nonnull Runnable runnable) {
    document.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
    try {
      runnable.run();
    }
    finally {
      document.putUserData(UndoConstants.DONT_RECORD_UNDO, null);
    }
  }

  public static void disableUndoFor(@Nonnull VirtualFile file) {
    file.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
  }

  public static void enableUndoFor(@Nonnull Document document) {
    document.putUserData(UndoConstants.DONT_RECORD_UNDO, null);
  }

  public static boolean isUndoDisabledFor(@Nonnull Document document) {
    return Boolean.TRUE.equals(document.getUserData(UndoConstants.DONT_RECORD_UNDO));
  }

  public static boolean isUndoDisabledFor(@Nonnull VirtualFile file) {
    return Boolean.TRUE.equals(file.getUserData(UndoConstants.DONT_RECORD_UNDO));
  }

  public static void forceUndoIn(@Nonnull VirtualFile file, @Nonnull Runnable runnable) {
    file.putUserData(UndoConstants.FORCE_RECORD_UNDO, Boolean.TRUE);
    try {
      runnable.run();
    }
    finally {
      file.putUserData(UndoConstants.FORCE_RECORD_UNDO, null);
    }
  }

  public static void setForceUndoFlag(@Nonnull VirtualFile file, boolean flag) {
    file.putUserData(UndoConstants.FORCE_RECORD_UNDO, flag ? Boolean.TRUE : null);
  }

  public static boolean isForceUndoFlagSet(@Nonnull VirtualFile file) {
    return file.getUserData(UndoConstants.FORCE_RECORD_UNDO) == Boolean.TRUE;
  }
}
