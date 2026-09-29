/*
 * Copyright 2000-2023 JetBrains s.r.o. and contributors.
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
package consulo.ide.impl.idea.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.language.editor.ui.awt.TextCompletionProvider;
import consulo.language.editor.ui.awt.TextCompletionUtil;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import java.util.Objects;

@ExtensionImpl(id = "textCompletion", order = "first, before commitCompletion")
public class TextCompletionContributor extends CompletionContributor implements DumbAware {
    @RequiredReadAction
    @Override
    public void fillCompletionVariants(CompletionParameters parameters, CompletionResultSet result) {
        PsiFile file = parameters.getOriginalFile();

        TextCompletionProvider provider = TextCompletionUtil.getProvider(file);
        if (provider == null) {
            return;
        }

        if (parameters.getInvocationCount() == 0 && !Boolean.TRUE.equals(file.getUserData(TextCompletionUtil.AUTO_POPUP_KEY))) {
            return;
        }

        String advertisement = provider.getAdvertisement();
        if (advertisement != null) {
            result.addLookupAdvertisement(advertisement);
        }

        String text = file.getText();
        int offset = Math.min(text.length(), parameters.getOffset());
        String prefix = provider.getPrefix(text, offset);
        if (prefix == null) {
            return;
        }

        CompletionResultSet activeResult = provider.applyPrefixMatcher(result, prefix);

        provider.fillCompletionVariants(parameters, prefix, activeResult);
    }

    @Override
    public boolean invokeAutoPopup(PsiElement position, char typeChar) {
        PsiFile file = position.getContainingFile();
        TextCompletionProvider provider = TextCompletionUtil.getProvider(file);
        if (provider != null && Boolean.TRUE.equals(file.getUserData(TextCompletionUtil.AUTO_POPUP_KEY))) {
            return Objects.equals(CharFilter.Result.ADD_TO_PREFIX, provider.acceptChar(typeChar));
        }
        return super.invokeAutoPopup(position, typeChar);
    }

    @Override
    public Language getLanguage() {
        return PlainTextLanguage.INSTANCE;
    }
}
