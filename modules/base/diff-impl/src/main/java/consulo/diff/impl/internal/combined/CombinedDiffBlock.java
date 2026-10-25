// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

public interface CombinedDiffBlock<ID extends CombinedBlockId> extends Disposable {
    ID getId();

    Component getHeader();

    Component getStickyHeaderComponent();

    Component getPreferredFocusComponent();

    Component getBody();

    Component getComponent();

    @RequiredUIAccess
    default void updateBlockContent(CombinedDiffBlockContent newContent) {
    }
}
