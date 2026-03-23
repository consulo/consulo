// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * @param displayText Text that will be displayed in the UI
 * @param actionId    Action ID passed to provider when this action is invoked. null for non-clickable line
 */
public record CodeVisionEntryExtraActionModel(LocalizeValue displayText, @Nullable String actionId) {
}
