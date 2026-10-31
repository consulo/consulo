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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.structureView.StructureView;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewWrapper;
import consulo.language.editor.structureView.StructureViewExtension;
import consulo.language.editor.structureView.StructureViewFactoryEx;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The production {@code StructureViewFactoryImpl} lives in {@code consulo.ide.impl}. Structure view models
 * ({@code PsiTreeElementBase.getChildren()}) ask the factory for the {@link StructureViewExtension}s; there is no
 * structure view tool window headless, so the UI parts are unsupported.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessStructureViewFactory extends StructureViewFactoryEx {
    private static final ExtensionPointCacheKey<StructureViewExtension, MultiMap<Class<? extends PsiElement>, StructureViewExtension>> CACHE_KEY =
        ExtensionPointCacheKey.create("HeadlessStructureViewExtension", walker -> {
            MultiMap<Class<? extends PsiElement>, StructureViewExtension> map = new MultiMap<>();
            walker.walk(extension -> map.putValue(extension.getType(), extension));
            return map;
        });

    private final Project myProject;
    private final Map<Class<? extends PsiElement>, List<StructureViewExtension>> myExtensions = new ConcurrentHashMap<>();
    private final Set<String> myActiveActions = ConcurrentHashMap.newKeySet();

    @Inject
    public HeadlessStructureViewFactory(Project project) {
        myProject = project;
    }

    @Override
    public @Nullable StructureViewWrapper getStructureViewWrapper() {
        return null;
    }

    @Override
    public Collection<StructureViewExtension> getAllExtensions(Class<? extends PsiElement> type) {
        return myExtensions.computeIfAbsent(type, it -> {
            MultiMap<Class<? extends PsiElement>, StructureViewExtension> map =
                myProject.getApplication().getExtensionPoint(StructureViewExtension.class).getOrBuildCache(CACHE_KEY);
            List<StructureViewExtension> result = new ArrayList<>();
            for (Class<? extends PsiElement> registeredType : map.keySet()) {
                if (ReflectionUtil.isAssignable(registeredType, it)) {
                    result.addAll(map.get(registeredType));
                }
            }
            return result;
        });
    }

    @Override
    public void setActiveAction(String name, boolean state) {
        if (state) {
            myActiveActions.add(name);
        }
        else {
            myActiveActions.remove(name);
        }
    }

    @Override
    public boolean isActionActive(String name) {
        return myActiveActions.contains(name);
    }

    @Override
    public void runWhenInitialized(Runnable runnable) {
        runnable.run();
    }

    @Override
    public StructureView createStructureView(FileEditor fileEditor, StructureViewModel treeModel, Project project) {
        throw new UnsupportedOperationException("no structure view component headless");
    }

    @Override
    public StructureView createStructureView(FileEditor fileEditor, StructureViewModel treeModel, Project project, boolean showRootNode) {
        throw new UnsupportedOperationException("no structure view component headless");
    }
}
