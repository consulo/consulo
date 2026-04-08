// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.notification;

import com.intellij.notification.impl.NotificationIdsHolder;
import jakarta.annotation.Nonnull;

import java.util.List;

public final class CollaborationToolsNotificationIdsHolder implements NotificationIdsHolder {
    public static final String REVIEW_BRANCH_CHECKOUT_FAILED = "review.branch.checkout.failed";

    CollaborationToolsNotificationIdsHolder() {
    }

    @Override
    public @Nonnull List<String> getNotificationIds() {
        return List.of(REVIEW_BRANCH_CHECKOUT_FAILED);
    }
}
