// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

public final class InlayTags {
    private InlayTags() {
    }

    public static final byte LIST_TAG = 0;
    public static final byte TEXT_TAG = 1;
    public static final byte ICON_TAG = 2;
    public static final byte COLLAPSE_BUTTON_TAG = 3;
    public static final byte COLLAPSIBLE_LIST_EXPANDED_BRANCH_TAG = 4;
    public static final byte COLLAPSIBLE_LIST_COLLAPSED_BRANCH_TAG = 5;
    public static final byte COLLAPSIBLE_LIST_EXPLICITLY_EXPANDED_TAG = 6;
    public static final byte COLLAPSIBLE_LIST_IMPLICITLY_EXPANDED_TAG = 7;
    public static final byte COLLAPSIBLE_LIST_EXPLICITLY_COLLAPSED_TAG = 8;
    public static final byte COLLAPSIBLE_LIST_IMPLICITLY_COLLAPSED_TAG = 9;
    public static final byte CLICK_HANDLER_SCOPE_TAG = 10;
}