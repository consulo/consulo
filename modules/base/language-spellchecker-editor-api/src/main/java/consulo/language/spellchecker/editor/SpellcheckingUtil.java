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
package consulo.language.spellchecker.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.SpellcheckingStrategy;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-01
 */
public final class SpellcheckingUtil {
    public static final String SPELL_CHECKING_INSPECTION_TOOL_NAME = "SpellCheckingInspection";

    private SpellcheckingUtil() {
    }

    @RequiredReadAction
    public static @Nullable SpellcheckingStrategy getSpellcheckingStrategy(PsiElement element) {
        for (SpellcheckingStrategy strategy : SpellcheckingStrategy.forLanguage(element.getLanguage())) {
            if (strategy.isMyContext(element)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * Feeds tokens of the element into the consumer, using the {@link SpellcheckingStrategy} of the element language.
     */
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static void tokenize(PsiElement element, TokenConsumer consumer) {
        SpellcheckingStrategy strategy = getSpellcheckingStrategy(element);
        if (strategy == null) {
            return;
        }
        Tokenizer tokenizer = strategy.getTokenizer(element);
        tokenizer.tokenize(element, consumer);
    }
}
