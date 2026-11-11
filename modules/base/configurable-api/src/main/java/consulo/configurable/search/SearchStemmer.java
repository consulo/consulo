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
package consulo.configurable.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.util.Locale;

/**
 * Brings the words of a language to their stems, so settings search finds "spaces" by "space" - the forms of a word
 * matching each other. A localization plugin brings the stemmer of its language; English is the platform's.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SearchStemmer {
    /**
     * The language of the stemmer - matched by {@link Locale#getLanguage()}.
     */
    Locale getLocale();

    /**
     * The stem of a lower cased word of the language - the word itself when it has no ending to cut.
     */
    String stem(String word);
}
