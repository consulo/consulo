// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.localize.LocalizeValue;

import java.util.Set;

public record InlayProviderInfo(
    DeclarativeInlayHintsProvider provider,
    String providerId,
    Set<DeclarativeInlayOptionInfo> options,
    boolean isEnabledByDefault,
    LocalizeValue providerName
) {
}
