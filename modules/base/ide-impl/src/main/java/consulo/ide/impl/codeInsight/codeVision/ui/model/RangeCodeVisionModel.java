// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision.ui.model;

import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.document.util.TextRange;
import consulo.language.editor.codeVision.CodeVisionAnchorKind;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.language.editor.codeVision.CodeVisionPredefinedActionEntry;
import consulo.project.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RangeCodeVisionModel {
    public enum InlayState {
        NORMAL,
        ACTIVE
    }

    public final Project project;
    public final Editor editor;
    public final TextRange anchoringRange;
    public final String name;
    public final ArrayList<Inlay<?>> inlays = new ArrayList<>();

    private final List<CodeVisionEntry> lensForRange;

    public RangeCodeVisionModel(Project project,
                                Editor editor,
                                Map<CodeVisionAnchorKind, List<CodeVisionEntry>> lensMap,
                                TextRange anchoringRange,
                                String name) {
        this.project = project;
        this.editor = editor;
        this.anchoringRange = anchoringRange;
        this.name = name;
        this.lensForRange = lensMap.values().stream().flatMap(List::stream).toList();
    }

    public void handleLensClick(CodeVisionEntry entry, Inlay<?> anchorInlay) {
        if (entry instanceof CodeVisionPredefinedActionEntry actionEntry) {
            actionEntry.onClick(editor);
        }
    }

    public void handleLensRightClick(CodeVisionEntry clickedEntry, Inlay<?> anchorInlay) {
        // no-op without ProjectCodeVisionModel
    }

    public InlayState state() {
        return InlayState.NORMAL;
    }
}
