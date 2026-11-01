// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.spellchecker.editor.dictionary;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface DictionaryLayersProvider {
    List<DictionaryLayer> getLayers(Project project);

    static List<DictionaryLayer> getAllLayers(Project project) {
        List<DictionaryLayer> layers = new ArrayList<>();
        project.getApplication().getExtensionPoint(DictionaryLayersProvider.class).forEach(it -> layers.addAll(it.getLayers(project)));
        return layers;
    }

    static @Nullable DictionaryLayer getLayer(Project project, String layerName) {
        for (DictionaryLayer layer : getAllLayers(project)) {
            if (layer.getName().get().equals(layerName)) {
                return layer;
            }
        }
        return null;
    }
}
