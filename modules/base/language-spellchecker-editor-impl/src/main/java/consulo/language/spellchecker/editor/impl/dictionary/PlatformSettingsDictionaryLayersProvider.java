// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.spellchecker.editor.impl.dictionary;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayer;
import consulo.language.spellchecker.editor.dictionary.DictionaryLayersProvider;
import consulo.project.Project;

import java.util.List;

@ExtensionImpl
public class PlatformSettingsDictionaryLayersProvider implements DictionaryLayersProvider {
    @Override
    public List<DictionaryLayer> getLayers(Project project) {
        return List.of(new ApplicationDictionaryLayer(project.getApplication()), new ProjectDictionaryLayer(project));
    }
}
