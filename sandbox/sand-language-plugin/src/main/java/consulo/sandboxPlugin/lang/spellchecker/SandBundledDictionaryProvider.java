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
package consulo.sandboxPlugin.lang.spellchecker;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.dictionary.BundledDictionaryProvider;

import java.io.InputStream;
import java.util.function.BiConsumer;

/**
 * Reference {@link BundledDictionaryProvider}: words accepted by every spellchecker engine.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
@ExtensionImpl
public class SandBundledDictionaryProvider implements BundledDictionaryProvider {
    private static final String DICTIONARY = "/dictionary/sand.dic";

    @Override
    public void loadDictionaries(BiConsumer<String, InputStream> consumer) {
        InputStream stream = getClass().getResourceAsStream(DICTIONARY);
        if (stream != null) {
            consumer.accept(DICTIONARY, stream);
        }
    }
}
