// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.spellchecker.editor.impl.dictionary;

import consulo.application.Application;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayer;
import consulo.language.spellchecker.editor.dictionary.EditableDictionary;
import consulo.language.spellchecker.editor.impl.state.AppDictionaryState;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.localize.LocalizeValue;

public class ApplicationDictionaryLayer implements DictionaryLayer {
    private final Application myApplication;

    public ApplicationDictionaryLayer(Application application) {
        myApplication = application;
    }

    @Override
    public LocalizeValue getName() {
        return SpellCheckerLocalize.dictionaryNameApplicationLevel();
    }

    @Override
    public EditableDictionary getDictionary() {
        return myApplication.getInstance(AppDictionaryState.class).getOrCreateDictionary();
    }
}
