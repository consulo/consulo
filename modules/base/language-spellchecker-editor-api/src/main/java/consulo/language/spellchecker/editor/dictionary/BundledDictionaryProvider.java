/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.spellchecker.editor.dictionary;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.io.InputStream;
import java.util.function.BiConsumer;

/**
 * Word lists of accepted words (keywords, library and framework names) which are valid for every spellchecker engine.
 * <p>
 * Each dictionary is a plain text file, one word per line. The provider opens the streams itself, so resources of its own module
 * are accessible: {@code consumer.accept("/dictionary/my.dic", getClass().getResourceAsStream("/dictionary/my.dic"))}.
 * The consumer closes the stream. The name identifies the dictionary in settings, where it can be disabled.
 * <p>
 * A word listed here is never reported, whichever engine is active. Language dictionaries of an engine do not belong here.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BundledDictionaryProvider {
    void loadDictionaries(BiConsumer<String, InputStream> consumer);
}
