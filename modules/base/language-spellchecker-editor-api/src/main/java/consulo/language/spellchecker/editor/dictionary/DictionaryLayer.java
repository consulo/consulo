// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.spellchecker.editor.dictionary;

import consulo.localize.LocalizeValue;

/**
 * A user dictionary words can be saved to, e.g. the project or the application dictionary.
 */
public interface DictionaryLayer {
    EditableDictionary getDictionary();

    LocalizeValue getName();
}
