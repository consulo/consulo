// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.viewer.simple;

import consulo.diff.util.Side;

import java.util.List;

public class SimpleAlignedDiffModel extends AlignedDiffModelBase {
    private final SimpleDiffViewer myViewer;

    public SimpleAlignedDiffModel(SimpleDiffViewer viewer) {
        super(
            viewer.getRequest(),
            viewer.getContext(),
            viewer.getEditor1(),
            viewer.getEditor2(),
            (sideIndex, line) -> viewer.transferLine(Side.fromIndex(sideIndex), line)
        );
        myViewer = viewer;
    }

    @Override
    public List<? extends AlignableChange> getDiffChanges() {
        return myViewer.getDiffChanges();
    }
}
