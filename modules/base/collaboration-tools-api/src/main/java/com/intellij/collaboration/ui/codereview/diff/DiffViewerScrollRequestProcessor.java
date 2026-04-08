// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerChangeScrollRequest;
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerLineScrollRequest;
import com.intellij.collaboration.ui.codereview.diff.model.DiffViewerScrollRequest;
import consulo.diff.FrameDiffTool;
import consulo.diff.util.Side;
import jakarta.annotation.Nonnull;

final class DiffViewerScrollRequestProcessor {
    private DiffViewerScrollRequestProcessor() {
    }

    @RequiresEdt
    static void scroll(@Nonnull FrameDiffTool.DiffViewer viewer, @Nonnull DiffViewerScrollRequest request) {
        if (request instanceof DiffViewerLineScrollRequest lineRequest) {
            scroll(viewer, lineRequest.getLocation());
        }
        else if (request instanceof DiffViewerChangeScrollRequest changeRequest) {
            scrollToChange(viewer, changeRequest.getPolicy());
        }
    }

    @RequiresEdt
    static void scroll(@Nonnull FrameDiffTool.DiffViewer viewer, @Nonnull DiffLineLocation location) {
        Side side = location.getSide();
        int line = location.getLine();
        if (viewer instanceof OnesideTextDiffViewer onesideViewer) {
            onesideViewer.scrollToLine(line);
        }
        else if (viewer instanceof SimpleDiffViewer simpleViewer) {
            simpleViewer.scrollToLine(side, line);
        }
        else if (viewer instanceof UnifiedDiffViewer unifiedViewer) {
            unifiedViewer.scrollToLine(side, line);
        }
    }

    private static void scrollToChange(@Nonnull FrameDiffTool.DiffViewer viewer, @Nonnull ScrollToPolicy policy) {
        if (viewer instanceof SimpleDiffViewer simpleViewer) {
            simpleViewer.scrollToChange(policy);
        }
        else if (viewer instanceof UnifiedDiffViewer unifiedViewer) {
            unifiedViewer.scrollToChange(policy);
        }
    }
}
