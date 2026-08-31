// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.daemon;

import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.internal.SilentChangeVetoer;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoManager;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * Sometimes we need to know if we can silently change the file, without user's explicit permission.
 * By convention, permission is required for:<pre>
 * - never touched files,
 * - files under explicit write permission version control (such as Perforce, which asks "do you want to edit this file"),
 * - files in the middle of cut-n-paste operation.
 * </pre>
 * <p/>
 * To determine this, several things are computed in two stages.
 * Some things require the EDT for computation, e.g. {@link #thisFile(Project, VirtualFile)}, to query this file "undo" status.
 * Some things, on the other hand, are quite expensive to compute in EDT and thus require a background thread, e.g.
 * {@link SilentChangeVetoer#extensionsAllowToChangeFileSilently(Project, VirtualFile)} (which queries the VCS dirty scope).
 * The complete algorithm is the following:<pre>
 * (in BGT) {@code ThreeState extensionsAllowToChangeFileSilently = SilentChangeVetoer.extensionsAllowToChangeFileSilently(project, virtualFile);}
 * (in BGT) {@code boolean isFileInContent = ModuleUtilCore.projectContainsFile(project, virtualFile, false);}
 * (in EDT) {@code Result result = CanISilentlyChange.thisFile(project, virtualFile);}
 * (in any thread) {@code boolean canSilentlyChange = result.canIReally(isFileInContent, extensionsAllowToChangeFileSilently);}
 * </pre>
 */
public final class CanISilentlyChange {
    public enum Result {
        UH_HUH,             // yes
        UH_UH,              // no
        ONLY_WHEN_IN_CONTENT;

        // can call from any thread
        public boolean canIReally(boolean isInContent, ThreeState extensionsAllowToChangeFileSilently) {
            return switch (this) {
                case UH_HUH -> extensionsAllowToChangeFileSilently != ThreeState.NO;
                case UH_UH -> false;
                case ONLY_WHEN_IN_CONTENT -> extensionsAllowToChangeFileSilently != ThreeState.NO && isInContent;
            };
        }
    }

    @RequiredUIAccess
    private static boolean canUndo(Project project, VirtualFile virtualFile) {
        FileEditor[] editors = FileEditorManager.getInstance(project).getEditors(virtualFile);
        if (editors.length == 0) {
            return false;
        }
        UndoManager undoManager = ProjectUndoManager.getInstance(project);
        for (FileEditor editor : editors) {
            if (undoManager.isUndoAvailable(editor)) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    public static Result thisFile(Project project, @Nullable VirtualFile virtualFile) {
        if (virtualFile == null) {
            return Result.UH_UH;
        }
        if (ScratchUtil.isScratch(virtualFile)) {
            return canUndo(project, virtualFile) ? Result.UH_HUH : Result.UH_UH;
        }
        return canUndo(project, virtualFile) ? Result.ONLY_WHEN_IN_CONTENT : Result.UH_UH;
    }

    private CanISilentlyChange() {
    }
}
