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
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.FormatTextRanges;
import consulo.language.codeStyle.FormattingRangesInfo;
import consulo.language.codeStyle.impl.internal.CodeFormatterFacade;
import consulo.language.codeStyle.internal.CoreCodeStyleUtil;
import consulo.language.codeStyle.service.FormattingService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl(id = "core", order = "last")
public final class CoreFormattingService implements FormattingService {
    private static final Set<Feature> FEATURES = EnumSet.of(Feature.AD_HOC_FORMATTING, Feature.FORMAT_FRAGMENTS);

    @Override
    @RequiredReadAction
    public boolean canFormat(PsiFile file) {
        return true;
    }

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public PsiElement formatElement(PsiElement element, boolean canChangeWhiteSpacesOnly) {
        ASTNode treeElement = element.getNode();
        PsiFile file = element.getContainingFile();
        PsiElement formatted =
            new CodeFormatterFacade(getSettings(file), element.getLanguage(), canChangeWhiteSpacesOnly)
                .processElement(treeElement).getPsi();
        return CoreCodeStyleUtil.postProcessElement(file, formatted, canChangeWhiteSpacesOnly);
    }

    @Override
    public PsiElement formatElement(PsiElement element, TextRange range, boolean canChangeWhiteSpacesOnly) {
        ASTNode treeElement = element.getNode();
        PsiFile file = element.getContainingFile();
        CodeFormatterFacade codeFormatter = new CodeFormatterFacade(getSettings(file), element.getLanguage());
        PsiElement formatted = codeFormatter.processRange(treeElement, range.getStartOffset(), range.getEndOffset()).getPsi();
        return CoreCodeStyleUtil.postProcessElement(file, formatted, canChangeWhiteSpacesOnly);
    }

    @Override
    public void formatRanges(PsiFile file, FormattingRangesInfo rangesInfo, boolean canChangeWhiteSpaceOnly, boolean quickFormat) {
        List<CoreCodeStyleUtil.RangeFormatInfo> infos = CoreCodeStyleUtil.getRangeFormatInfoList(file, rangesInfo);
        CodeFormatterFacade codeFormatter = new CodeFormatterFacade(getSettings(file), file.getLanguage());
        codeFormatter.processText(file, (FormatTextRanges) rangesInfo, !canChangeWhiteSpaceOnly);
        CoreCodeStyleUtil.postProcessRanges(infos, range -> CoreCodeStyleUtil.postProcessText(file, range, canChangeWhiteSpaceOnly));
    }

    private static CodeStyleSettings getSettings(PsiFile file) {
        return CodeStyle.getSettings(file);
    }
}
