/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.spellchecker.editor.impl.state;

import consulo.component.persist.StateJsonSerializer;
import consulo.component.persist.StateSplitterEx;
import consulo.util.lang.Pair;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Each user dictionary is stored in its own {@code <user>.json} file: {@code {"name": "<user>", "words": [...]}}.
 *
 * @author shkate@jetbrains.com
 */
public class ProjectDictionarySplitter extends StateSplitterEx {
    private static final String DICTIONARY_TAG = "dictionary";
    private static final String WORDS_TAG = "words";
    private static final String WORD_TAG = "w";

    @Override
    public List<Pair<Element, String>> splitState(Element state) {
        return splitState(state, DictionaryState.NAME_ATTRIBUTE);
    }

    @Override
    public byte @Nullable [] writeJson(Object component, Element subState, StateJsonSerializer serializer) {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put(DictionaryState.NAME_ATTRIBUTE, subState.getAttributeValue(DictionaryState.NAME_ATTRIBUTE));

        TreeSet<String> words = new TreeSet<>();
        Element wordsElement = subState.getChild(WORDS_TAG);
        if (wordsElement != null) {
            for (Element word : wordsElement.getChildren(WORD_TAG)) {
                words.add(word.getText());
            }
        }
        tree.put(WORDS_TAG, new ArrayList<>(words));

        try {
            return serializer.writeTree(tree);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public @Nullable Element readJson(Object component, byte[] content, StateJsonSerializer serializer) {
        Object tree;
        try {
            tree = serializer.readTree(content);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!(tree instanceof Map<?, ?> map) || !(map.get(DictionaryState.NAME_ATTRIBUTE) instanceof String name)) {
            return null;
        }

        Element dictionary = new Element(DICTIONARY_TAG).setAttribute(DictionaryState.NAME_ATTRIBUTE, name);
        Element wordsElement = new Element(WORDS_TAG);
        if (map.get(WORDS_TAG) instanceof List<?> words) {
            for (Object word : words) {
                if (word instanceof String text) {
                    wordsElement.addContent(new Element(WORD_TAG).setText(text));
                }
            }
        }
        dictionary.addContent(wordsElement);
        return dictionary;
    }
}
