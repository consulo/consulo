// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.codeVision;

import consulo.language.editor.codeVision.PlatformCodeVisionIds;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.localize.LocalizeValue;

public abstract class ReferencesCodeVisionProvider extends CodeVisionProviderBase {
    @Override
    public LocalizeValue getName() {
        return CodeInsightLocalize.codeVisionUsagesName();
    }

    @Override
    public String getGroupId() {
        return PlatformCodeVisionIds.USAGES.getKey();
    }
}
