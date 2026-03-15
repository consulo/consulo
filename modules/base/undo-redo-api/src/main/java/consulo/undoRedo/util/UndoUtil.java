// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.undoRedo.util;

import consulo.application.Application;
import consulo.document.Document;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Objects;

public final class UndoUtil {
    private UndoUtil() {
    }

    @RequiredUIAccess
    public static void writeInRunUndoTransparentAction(Runnable runnable) {
        CommandProcessor.getInstance().runUndoTransparentAction(() -> Application.get().runWriteAction(runnable));
    }

    public static void disableUndoFor(Document document) {
        document.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
    }

    public static void disableUndoIn(Document document, Runnable runnable) {
        document.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
        try {
            runnable.run();
        }
        finally {
            document.putUserData(UndoConstants.DONT_RECORD_UNDO, null);
        }
    }

    public static void disableUndoFor(VirtualFile file) {
        file.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
    }

    public static void enableUndoFor(Document document) {
        document.putUserData(UndoConstants.DONT_RECORD_UNDO, null);
    }

    public static boolean isUndoDisabledFor(Document document) {
        return Boolean.TRUE.equals(document.getUserData(UndoConstants.DONT_RECORD_UNDO));
    }

    public static boolean isUndoDisabledFor(VirtualFile file) {
        return Boolean.TRUE.equals(file.getUserData(UndoConstants.DONT_RECORD_UNDO));
    }

    public static void forceUndoIn(VirtualFile file, Runnable runnable) {
        file.putUserData(UndoConstants.FORCE_RECORD_UNDO, Boolean.TRUE);
        try {
            runnable.run();
        }
        finally {
            file.putUserData(UndoConstants.FORCE_RECORD_UNDO, null);
        }
    }

    public static void setForceUndoFlag(VirtualFile file, boolean flag) {
        file.putUserData(UndoConstants.FORCE_RECORD_UNDO, flag ? Boolean.TRUE : null);
    }

    public static boolean isForceUndoFlagSet(VirtualFile file) {
        return Objects.equals(file.getUserData(UndoConstants.FORCE_RECORD_UNDO), Boolean.TRUE);
    }
}
