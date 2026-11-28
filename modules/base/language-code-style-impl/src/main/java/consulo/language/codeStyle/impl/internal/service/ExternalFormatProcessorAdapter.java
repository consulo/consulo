// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.language.codeStyle.impl.internal.service;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.ExternalFormatProcessor;
import consulo.language.codeStyle.FormattingRangesInfo;
import consulo.language.codeStyle.internal.CoreCodeStyleUtil;
import consulo.language.codeStyle.service.FormattingService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * To support legacy API
 */
@ExtensionImpl(id = "externalFormatProcessorAdapter", order = "first")
public final class ExternalFormatProcessorAdapter implements FormattingService {
    private static final Set<Feature> FEATURES = EnumSet.of(Feature.AD_HOC_FORMATTING, Feature.FORMAT_FRAGMENTS);

    @Override
    @RequiredReadAction
    public boolean canFormat(PsiFile file) {
        return ExternalFormatProcessor.useExternalFormatter(file);
    }

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public PsiElement formatElement(PsiElement element, boolean canChangeWhiteSpacesOnly) {
        return ExternalFormatProcessor.formatElement(element, element.getTextRange(), canChangeWhiteSpacesOnly);
    }

    @Override
    public PsiElement formatElement(PsiElement element, TextRange range, boolean canChangeWhiteSpacesOnly) {
        return ExternalFormatProcessor.formatElement(element, range, canChangeWhiteSpacesOnly);
    }

    @Override
    public void formatRanges(PsiFile file, FormattingRangesInfo rangesInfo, boolean canChangeWhiteSpaceOnly, boolean quickFormat) {
        List<CoreCodeStyleUtil.RangeFormatInfo> infos = CoreCodeStyleUtil.getRangeFormatInfoList(file, rangesInfo);
        // IMPORTANT: Don't use canChangeWhiteSpaceOnly from parameters because we always want it to be 'false' for formatRanges called here.
        CoreCodeStyleUtil.postProcessRanges(infos, range -> ExternalFormatProcessor.formatRangeInFile(file, range, false));
    }
}
