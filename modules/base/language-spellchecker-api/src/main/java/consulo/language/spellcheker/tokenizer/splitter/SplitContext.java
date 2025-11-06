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

import consulo.document.util.TextRange;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author UNV
 * @since 2025-11-06
 */
public sealed interface SplitContext permits BaseSplitContext {
    String getText();

    default String substring(@Nonnull TextRange range) {
        return range.substring(getText());
    }

    default boolean isEmpty() {
        return StringUtil.isEmpty(getText());
    }

    void addWord(@Nonnull TextRange range);

    void checkCanceled();

    static SplitContext of(@Nonnull String text, @Nonnull Consumer<TextRange> consumer) {
        return new BaseSplitContext(text, consumer);
    }
}
