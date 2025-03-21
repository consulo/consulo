// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ui.ex.action;

import consulo.annotation.DeprecationInfo;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class DumbAwareToggleAction extends ToggleAction implements DumbAware {
    protected DumbAwareToggleAction() {
    }

    protected DumbAwareToggleAction(@Nonnull LocalizeValue text) {
        super(text);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected DumbAwareToggleAction(@Nullable String text) {
        super(text);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected DumbAwareToggleAction(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected DumbAwareToggleAction(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }
}