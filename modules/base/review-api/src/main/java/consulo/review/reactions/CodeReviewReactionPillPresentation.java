// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.reactions;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import java.util.List;

public interface CodeReviewReactionPillPresentation {
    LocalizeValue getReactionName();

    List<LocalizeValue> getReactors();

    boolean isOwnReaction();

    Image getIcon(int size);
}
