// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.spellchecker.editor.impl.dictionary;

import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayer;
import consulo.language.spellchecker.editor.dictionary.EditableDictionary;
import consulo.language.spellchecker.editor.impl.SpellCheckerManagerImpl;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

public class ProjectDictionaryLayer implements DictionaryLayer {
    private final Project myProject;

    public ProjectDictionaryLayer(Project project) {
        myProject = project;
    }

    @Override
    public LocalizeValue getName() {
        return SpellCheckerLocalize.dictionaryNameProjectLevel();
    }

    @Override
    public EditableDictionary getDictionary() {
        return ((SpellCheckerManagerImpl) SpellCheckerManager.getInstance(myProject)).getProjectDictionary();
    }
}
