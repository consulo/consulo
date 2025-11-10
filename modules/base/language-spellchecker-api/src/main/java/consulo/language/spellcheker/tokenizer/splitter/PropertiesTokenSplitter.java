/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class PropertiesTokenSplitter extends BaseTokenSplitter {
    private static final PropertiesTokenSplitter INSTANCE = new PropertiesTokenSplitter();

    public static PropertiesTokenSplitter getInstance() {
        return INSTANCE;
    }

    private static final Pattern WORD = Pattern.compile("\\p{L}*");

    @Override
    public void split(@Nonnull SplitContext context, @Nonnull TextRange range) {
        if (context.isEmpty()) {
            return;
        }
        IdentifierTokenSplitter splitter = IdentifierTokenSplitter.getInstance();
        Matcher matcher = WORD.matcher(context.substring(range));
        while (matcher.find()) {
            if (matcher.end() - matcher.start() < MIN_RANGE_LENGTH) {
                continue;
            }
            TextRange found = matcherRange(range, matcher);
            splitter.split(context, found);
        }
    }
}