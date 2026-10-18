// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.list;

import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

public interface TagPresentation {
    LocalizeValue getName();

    @Nullable
    ColorValue getColor();
}
