// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.create;

import consulo.util.concurrent.coroutine.ObservableValue;

public interface CodeReviewTitleDescriptionViewModel {
    ObservableValue<String> getTitleText();

    ObservableValue<String> getDescriptionText();

    ObservableValue<Boolean> isTemplateLoading();

    void setTitle(String text);

    void setDescription(String text);
}
