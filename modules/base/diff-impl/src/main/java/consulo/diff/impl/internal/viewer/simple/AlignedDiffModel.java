// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.viewer.simple;

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;

public interface AlignedDiffModel extends Disposable {
    List<? extends AlignableChange> getDiffChanges();

    boolean needAlignChanges();

    @RequiredUIAccess
    void realignChanges();

    @RequiredUIAccess
    void clear();
}
