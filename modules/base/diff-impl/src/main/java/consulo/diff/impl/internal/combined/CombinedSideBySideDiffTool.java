// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ExtensionImpl;
import consulo.diff.DiffContext;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.DiffRequest;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

/**
 * This tool intended only for persistence purpose.
 * Combined diff viewer will be created by the corresponding {@link consulo.diff.combined.CombinedDiffComponentProcessor}.
 */
@ExtensionImpl(id = "combinedSideBySide", order = "after binary")
public class CombinedSideBySideDiffTool implements CombinedDiffTool {
    @Override
    public boolean canShow(DiffContext context, DiffRequest request) {
        return false;
    }

    @Override
    public String getName() {
        return DiffLocalize.combinedSideBySideViewer().get();
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.actionsSplitvertically();
    }
}
