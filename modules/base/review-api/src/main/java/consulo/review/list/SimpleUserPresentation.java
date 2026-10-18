// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public record SimpleUserPresentation(String username, @Nullable String fullName, Image avatarIcon) implements UserPresentation {
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public @Nullable String getFullName() {
        return fullName;
    }

    @Override
    public Image getAvatarIcon() {
        return avatarIcon;
    }
}
