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

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2025-11-06
 */
public final class BaseSplitContext implements SplitContext {
    
    private final String myText;
    
    private final Consumer<TextRange> myConsumer;
    
    private final ProgressIndicatorProvider myProgressIndicatorProvider;

    public BaseSplitContext(String text, Consumer<TextRange> consumer) {
        this(text, consumer, ProgressIndicatorProvider.getInstance());
    }

    public BaseSplitContext(
        String text,
        Consumer<TextRange> consumer,
        ProgressIndicatorProvider progressIndicatorProvider
    ) {
        myText = text;
        myConsumer = consumer;
        myProgressIndicatorProvider = progressIndicatorProvider;
    }

    
    @Override
    public String getText() {
        return myText;
    }

    @Override
    public String substring(TextRange range) {
        return range.substring(myText);
    }

    @Override
    public void addWord(TextRange range) {
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
