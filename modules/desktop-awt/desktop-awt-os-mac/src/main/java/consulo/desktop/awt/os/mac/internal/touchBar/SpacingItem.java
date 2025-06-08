// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.application.util.mac.foundation.ID;

final class SpacingItem extends TBItem {
    SpacingItem() {
        super("space", null);
    }

    @Override
    protected ID _createNativePeer() {
        return ID.NIL;
    } // mustn't be called
}