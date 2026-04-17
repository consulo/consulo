// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.list;

import org.jspecify.annotations.Nullable;

import javax.swing.*;

public interface UserPresentation {
    String getUsername();

    @Nullable String getFullName();

    Icon getAvatarIcon();

    default String getPresentableName() {
        String fullName = getFullName();
        return fullName != null ? fullName : getUsername();
    }
}
