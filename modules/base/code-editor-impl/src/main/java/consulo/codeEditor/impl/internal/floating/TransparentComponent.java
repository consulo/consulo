// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl.internal.floating;

import consulo.ui.annotation.RequiredUIAccess;

public interface TransparentComponent {
    @RequiredUIAccess
    boolean isComponentOnHold();

    @RequiredUIAccess
    void setOpacity(float opacity);

    @RequiredUIAccess
    void showComponent();

    @RequiredUIAccess
    void hideComponent();

    @RequiredUIAccess
    void repaintComponent();
}
