// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.intention;

import consulo.annotation.DeprecationInfo;
import consulo.document.util.TextRange;
import consulo.language.editor.internal.InspectionCache;
import consulo.language.editor.internal.InspectionCacheService;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Alexey Kudravtsev
 */
@Deprecated
@DeprecationInfo("Use QuickFix registration via HighlightInfo.Builder")
public final class QuickFixAction {
    private QuickFixAction() {
    }

    public static void registerQuickFixAction(@Nullable HighlightInfo info, @Nullable IntentionAction action, @Nullable HighlightDisplayKey key) {
        registerQuickFixAction(info, null, action, key);
    }

    public static void registerQuickFixAction(@Nullable HighlightInfo info, @Nullable IntentionAction action) {
        registerQuickFixAction(info, null, action);
    }

    /**
     * @deprecated This is used by TeamCity plugin
     */
    @Deprecated
    public static void registerQuickFixAction(@Nullable HighlightInfo info, @Nullable IntentionAction action, @Nullable List<IntentionAction> options, @Nonnull LocalizeValue displayName) {
        if (info == null) {
            return;
        }
        info.registerFix(action, options, displayName, null, null);
    }

    public static void registerQuickFixAction(@Nullable HighlightInfo info, @Nullable TextRange fixRange, @Nullable IntentionAction action, @Nullable HighlightDisplayKey key) {
        if (info == null) {
            return;
        }

        InspectionCache cache = InspectionCacheService.getInstance().get();

        info.registerFix(action, null, cache.getDisplayNameByKey(key), fixRange, key);
    }

    public static void registerQuickFixAction(@Nullable HighlightInfo info, @Nullable TextRange fixRange, @Nullable IntentionAction action) {
        if (info == null) {
            return;
        }
        info.registerFix(action, null, LocalizeValue.empty(), fixRange, null);
    }

    public static void registerQuickFixActions(@Nullable HighlightInfo info, @Nullable TextRange fixRange, @Nonnull Iterable<? extends IntentionAction> actions) {
        for (IntentionAction action : actions) {
            registerQuickFixAction(info, fixRange, action);
        }
    }
}
