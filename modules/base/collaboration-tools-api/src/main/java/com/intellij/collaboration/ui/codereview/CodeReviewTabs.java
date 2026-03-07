// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.awt.tab.TabInfo;
import icons.CollaborationToolsIcons;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import org.jetbrains.annotations.Nls;

import java.util.function.Supplier;

@Internal
public final class CodeReviewTabs {
    private CodeReviewTabs() {
    }

    public static @Nonnull Job bindTabText(
        @Nonnull CoroutineScope cs,
        @Nonnull TabInfo tab,
        @Nonnull Supplier<@Nls String> text,
        @Nonnull Flow<Integer> countFlow
    ) {
        // Note: This method binds tab text using coroutines flow collection.
        // The actual implementation requires Kotlin coroutine infrastructure.
        // The flow onEach + launchIn pattern needs to be called from Kotlin interop.
        return FlowKt.launchIn(
            FlowKt.onEach(
                countFlow,
                (count, continuation) -> {
                    setTabText(tab, text);
                    appendCount(tab, count, true);
                    return null;
                }
            ),
            cs
        );
    }

    private static void setTabText(@Nonnull TabInfo tab, @Nonnull Supplier<@Nls String> text) {
        tab.clearText(false).append(text.get(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    private static void appendCount(@Nonnull TabInfo tab, @Nullable Integer count, boolean smallGap) {
        if (count != null) {
            tab.append(smallGap ? "  " + count : "   " + count, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
    }

    @SuppressWarnings("unused")
    private static void appendUnreadIcon(@Nonnull TabInfo tab, @Nonnull TabLabel tabLabel, @Nullable Integer unread) {
        if (unread == null || unread <= 0) {
            tab.stopAlerting();
        }
        else {
            tab.setAlertIcon(new AlertIcon(
                CollaborationToolsIcons.Review.FileUnread,
                0,
                tabLabel.getLabelComponent().getPreferredSize().width + JBUIScale.scale(3)
            ));
            tab.fireAlert();
            tab.resetAlertRequest();
        }
    }

    @SuppressWarnings("unused")
    private static void setUnreadTooltip(@Nonnull TabInfo tab, @Nullable Integer unread) {
        tab.setTooltipText(
            unread != null && unread > 0
                ? CollaborationToolsLocalize.tooltipCodeReviewFilesNotViewed(unread).get()
                : null
        );
    }
}
