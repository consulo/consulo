// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.impl.image;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public record RepaintRequest(
    @Nullable Component component,
    int x,
    int y,
    @Nullable Component target,
    @Nullable Component paintingParent,
    @Nullable Rectangle paintingParentRec
) {
    public @Nullable Component getActualTarget() {
        if (target == null) {
            return null;
        }
        if (SwingUtilities.getWindowAncestor(target) != null) {
            return target;
        }
        return paintingParent != null && SwingUtilities.getWindowAncestor(paintingParent) != null ? paintingParent : null;
    }
}
