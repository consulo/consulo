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
package consulo.language.index.impl.internal.moduleAware;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * A roots change may move the options of any file — module flags are part of them — so the providers get a full
 * analysis pass; the meta storage is flushed first so a crash mid-pass loses nothing already recorded.
 */
@TopicImpl(ComponentScope.PROJECT)
public final class ModuleAwareIndexRootChangeListener implements ModuleRootListener {
    private final Project myProject;
    private final Provider<ModuleAwareIndexMetaStorage> myStorage;

    @Inject
    public ModuleAwareIndexRootChangeListener(Project project,
                                              Provider<ModuleAwareIndexMetaStorage> storage) {
        myProject = project;
        myStorage = storage;
    }

    @Override
    public void rootsChanged(ModuleRootEvent event) {
        myStorage.get().flush();
        ModuleAwareIndexOptionsAnalyzer.getInstance(myProject).schedule(null);
    }
}
