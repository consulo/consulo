// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.list;

import org.jspecify.annotations.Nullable;

import java.awt.*;

public interface TagPresentation {
    String getName();

    @Nullable Color getColor();
}
