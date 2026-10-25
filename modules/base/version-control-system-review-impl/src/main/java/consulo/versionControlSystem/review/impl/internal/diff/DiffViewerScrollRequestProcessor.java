// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.diff.FrameDiffTool;
import consulo.diff.impl.internal.fragment.UnifiedDiffViewer;
import consulo.diff.impl.internal.viewer.side.OnesideTextDiffViewer;
import consulo.diff.impl.internal.viewer.simple.SimpleDiffViewer;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import consulo.versionControlSystem.review.diff.model.DiffViewerChangeScrollRequest;
import consulo.versionControlSystem.review.diff.model.DiffViewerLineScrollRequest;
import consulo.versionControlSystem.review.diff.model.DiffViewerScrollRequest;

public final class DiffViewerScrollRequestProcessor {
    private DiffViewerScrollRequestProcessor() {
    }

    @RequiredUIAccess
    public static void scroll(FrameDiffTool.DiffViewer viewer, DiffViewerScrollRequest request) {
        switch (request) {
            case DiffViewerLineScrollRequest lineRequest -> scroll(viewer, lineRequest.location());
            case DiffViewerChangeScrollRequest changeRequest -> scroll(viewer, changeRequest.policy());
        }
    }

    @RequiredUIAccess
    public static void scroll(FrameDiffTool.DiffViewer viewer, DiffLineLocation location) {
        if (viewer instanceof OnesideTextDiffViewer oneside) {
            oneside.scrollToLine(location.line());
        }
        else if (viewer instanceof SimpleDiffViewer simple) {
            simple.scrollToLine(location.side(), location.line());
        }
        else if (viewer instanceof UnifiedDiffViewer unified) {
            unified.scrollToLine(location.side(), location.line());
        }
    }

    @RequiredUIAccess
    private static void scroll(FrameDiffTool.DiffViewer viewer, ScrollToPolicy policy) {
        if (viewer instanceof SimpleDiffViewer simple) {
            simple.scrollToChange(policy);
        }
        else if (viewer instanceof UnifiedDiffViewer unified) {
            unified.scrollToChange(policy);
        }
    }
}
