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
package consulo.language.spellchecker.editor.impl.dictionary;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.dictionary.BundledDictionaryProvider;

import java.io.InputStream;
import java.util.function.BiConsumer;

/**
 * Programming vocabulary accepted by every spellchecker engine.
 * <p>
 * Stored in a directory which is not a package name, so it is not encapsulated by the module system.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
@ExtensionImpl
public class ProgrammingBundledDictionaryProvider implements BundledDictionaryProvider {
    private static final String[] DICTIONARIES = {
        "/spellchecker-dictionary/programming.dic",      // Programming-related English words
        "/spellchecker-dictionary/programming-misc.dic", // Programming-related English acronyms and abbreviations
        "/spellchecker-dictionary/programming-names.dic" // Programming-related English names
    };

    @Override
    public void loadDictionaries(BiConsumer<String, InputStream> consumer) {
        for (String dictionary : DICTIONARIES) {
            InputStream stream = getClass().getResourceAsStream(dictionary);
            if (stream != null) {
                consumer.accept(dictionary, stream);
            }
        }
    }
}
