// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision.ui.model;

import consulo.codeEditor.Inlay;
import consulo.language.editor.codeVision.CodeVisionAnchorKind;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.util.dataholder.Key;

import java.util.ArrayList;
import java.util.List;

public class CodeVisionListData {
    public static final Key<CodeVisionListData> KEY = Key.create("CodeVisionListData");

    public final RangeCodeVisionModel rangeCodeVisionModel;
    public final Inlay<?> inlay;
    public final List<CodeVisionEntry> anchoredLens;
    public final CodeVisionAnchorKind anchor;

    private boolean _isPainted = false;

    public final ArrayList<CodeVisionEntry> visibleLens = new ArrayList<>();

    public CodeVisionListData(
        RangeCodeVisionModel rangeCodeVisionModel,
        Inlay<?> inlay,
        List<CodeVisionEntry> anchoredLens,
        CodeVisionAnchorKind anchor
    ) {
        this.rangeCodeVisionModel = rangeCodeVisionModel;
        this.inlay = inlay;
        this.anchoredLens = anchoredLens;
        this.anchor = anchor;
        updateVisible();
    }

    private void updateVisible() {
        visibleLens.clear();
        // In Consulo (no ProjectCodeVisionModel), all anchored lens are visible
        visibleLens.addAll(anchoredLens);
    }

    public boolean isPainted() {
        return _isPainted;
    }

    public void setPainted(boolean value) {
        if (_isPainted != value) {
            _isPainted = value;
            inlay.update();
        }
    }

    public RangeCodeVisionModel.InlayState state() {
        return rangeCodeVisionModel.state();
    }

    /**
     * In Consulo there is no ProjectCodeVisionModel, so the "more" inlay is not active.
     */
    public boolean isMoreLensActive() {
        return false;
    }
}
