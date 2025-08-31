/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.structureView.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.structureView.StructureView;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewWrapper;
import consulo.ide.impl.idea.ide.impl.StructureViewWrapperImpl;
import consulo.ide.impl.idea.ide.structureView.newStructureView.StructureViewComponent;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.language.editor.structureView.StructureViewExtension;
import consulo.language.editor.structureView.StructureViewFactoryEx;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.util.collection.MultiValuesMap;
import consulo.util.lang.StringUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author Eugene Belyaev
 */

@Singleton
@State(name = "StructureViewFactory", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceImpl
public final class StructureViewFactoryImpl extends StructureViewFactoryEx implements PersistentStateComponent<StructureViewFactoryImpl.State> {
  public static class State {
    @SuppressWarnings({"WeakerAccess"})
    public boolean AUTOSCROLL_MODE = true;
    @SuppressWarnings({"WeakerAccess"})
    public boolean AUTOSCROLL_FROM_SOURCE = false;
    @SuppressWarnings({"WeakerAccess"})
    public String ACTIVE_ACTIONS = "";
    public boolean SHOW_TOOLBAR = false;
  }

  private final Project myProject;
  private StructureViewWrapperImpl myStructureViewWrapperImpl;
  private State myState = new State();
  private Runnable myRunWhenInitialized = null;

  private static final ExtensionPointCacheKey<StructureViewExtension, MultiMap<Class<? extends PsiElement>, StructureViewExtension>>
    CACHE_KEY =
    ExtensionPointCacheKey.create("StructureViewExtension", walker -> {
      MultiMap<Class<? extends PsiElement>, StructureViewExtension> map = new MultiMap<>();
      walker.walk(extension -> map.putValue(extension.getType(), extension));
      return map;
    });


  private final MultiValuesMap<Class<? extends PsiElement>, StructureViewExtension> myImplExtensions = new MultiValuesMap<>();

  @Inject
  public StructureViewFactoryImpl(Project project) {
    myProject = project;
  }

  @Override
  public StructureViewWrapper getStructureViewWrapper() {
    return myStructureViewWrapperImpl;
  }

  @Override
  @Nonnull
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  public void initToolWindow(ToolWindowEx toolWindow) {
    myStructureViewWrapperImpl = new StructureViewWrapperImpl(myProject, toolWindow);
    if (myRunWhenInitialized != null) {
      myRunWhenInitialized.run();
      myRunWhenInitialized = null;
    }
  }

  @Override
  public Collection<StructureViewExtension> getAllExtensions(Class<? extends PsiElement> type) {
    Collection<StructureViewExtension> result = myImplExtensions.get(type);
    if (result == null) {
      MultiMap<Class<? extends PsiElement>, StructureViewExtension> map =
        myProject.getApplication().getExtensionPoint(StructureViewExtension.class).getOrBuildCache(CACHE_KEY);
      for (Class<? extends PsiElement> registeredType : map.keySet()) {
        if (ReflectionUtil.isAssignable(registeredType, type)) {
          Collection<StructureViewExtension> extensions = map.get(registeredType);
          for (StructureViewExtension extension : extensions) {
            myImplExtensions.put(type, extension);
          }
        }
      }
      result = myImplExtensions.get(type);
      if (result == null) return Collections.emptyList();
    }
    return result;
  }

  @Override
  public void setActiveAction(String name, boolean state) {
    Collection<String> activeActions = collectActiveActions();

    if (state) {
      activeActions.add(name);
    }
    else {
      activeActions.remove(name);
    }

    myState.ACTIVE_ACTIONS = toString(activeActions);
  }

  private static String toString(Collection<String> activeActions) {
    return StringUtil.join(activeActions, ",");
  }

  private Collection<String> collectActiveActions() {
    String[] strings = myState.ACTIVE_ACTIONS.split(",");
    return new HashSet<>(Arrays.asList(strings));
  }

  @Override
  public boolean isActionActive(String name) {
    return collectActiveActions().contains(name);
  }

  @Override
  public void runWhenInitialized(Runnable runnable) {
    if (myStructureViewWrapperImpl != null) {
      runnable.run();
    }
    else {
      myRunWhenInitialized = runnable;
    }
  }

  @Override
  public StructureView createStructureView(FileEditor fileEditor, StructureViewModel treeModel, Project project) {
    return createStructureView(fileEditor, treeModel, project, true);
  }

  @Override
  public StructureView createStructureView(FileEditor fileEditor,
                                           StructureViewModel treeModel,
                                           Project project,
                                           boolean showRootNode) {
    return new StructureViewComponent(fileEditor, treeModel, project, showRootNode);
  }
}
