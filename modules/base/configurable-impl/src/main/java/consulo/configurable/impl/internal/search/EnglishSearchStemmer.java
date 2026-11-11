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
package consulo.configurable.impl.internal.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.search.SearchStemmer;

import java.util.Locale;

/**
 * English, with the Porter stemmer.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionImpl
public class EnglishSearchStemmer implements SearchStemmer {
    @Override
    public Locale getLocale() {
        return Locale.ENGLISH;
    }

    @Override
    public String stem(String word) {
        String stem = PorterStemmerUtil.stem(word);
        return stem == null ? word : stem;
    }
}
