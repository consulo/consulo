// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public final class FrameworkPresentation {
    private final String myQueryTag;
    private final String myTitle;
    private final @Nullable Image myIcon;

    public FrameworkPresentation(String queryTag, String title, @Nullable Image icon) {
        myQueryTag = queryTag;
        myTitle = title;
        myIcon = icon;
    }

    /**
     * Provider id for search field of Endpoints View: prefer Title-Case-With-Dashes format.
     */
    public String getQueryTag() {
        return myQueryTag;
    }

    public String getTitle() {
        return myTitle;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }
}
