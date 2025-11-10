/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.spellcheker.tokenizer.splitter;

import consulo.application.progress.ProgressIndicatorProvider;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2025-11-06
 */
public final class BaseSplitContext implements SplitContext {
    @Nonnull
    private final String myText;
    @Nonnull
    private final Consumer<TextRange> myConsumer;
    @Nonnull
    private final ProgressIndicatorProvider myProgressIndicatorProvider;

    public BaseSplitContext(@Nonnull String text, @Nonnull Consumer<TextRange> consumer) {
        this(text, consumer, ProgressIndicatorProvider.getInstance());
    }

    public BaseSplitContext(
        @Nonnull String text,
        @Nonnull Consumer<TextRange> consumer,
        @Nonnull ProgressIndicatorProvider progressIndicatorProvider
    ) {
        myText = text;
        myConsumer = consumer;
        myProgressIndicatorProvider = progressIndicatorProvider;
    }

    @Nonnull
    @Override
    public String getText() {
        return myText;
    }

    @Override
    public String substring(@Nonnull TextRange range) {
        return range.substring(myText);
    }

    @Override
    public void addWord(@Nonnull TextRange range) {
        boolean tooShort = (range.getEndOffset() - range.getStartOffset()) <= BaseTokenSplitter.MIN_RANGE_LENGTH;
        if (tooShort) {
            return;
        }
        myConsumer.accept(range);
    }


    @Override
    public void checkCanceled() {
        myProgressIndicatorProvider.checkForCanceled();
    }
}
