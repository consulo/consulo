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

package com.intellij.openapi.module.impl;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.components.impl.ModulePathMacroManager;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;

import java.util.*;

/**
 * @author max
 */
@Logger
public class ModuleImpl extends ComponentManagerImpl implements ModuleEx {
  @NotNull
  private final Project myProject;
  private boolean isModuleAdded;

  @NonNls
  private static final String OPTION_WORKSPACE = "workspace";

  private String myName;

  private final ModuleScopeProvider myModuleScopeProvider;
  private VirtualFilePointer myDirVirtualFilePointer;

  private Map<String, String> myOptions = new LinkedHashMap<String, String>();

  public ModuleImpl(@NotNull String name, @NotNull String dirUrl, @NotNull Project project) {
    super(project, "Module " + name);

    getPicoContainer().registerComponentInstance(Module.class, this);

    myName = name;
    myProject = project;
    myModuleScopeProvider = new ModuleScopeProviderImpl(this);

    myDirVirtualFilePointer = VirtualFilePointerManager.getInstance().create(dirUrl, project, null);
  }

  @Override
  protected void bootstrapPicoContainer(@NotNull String name) {
    Extensions.instantiateArea(ExtensionAreas.IDEA_MODULE, this, (AreaInstance)getParentComponentManager());
    super.bootstrapPicoContainer(name);

    getPicoContainer().registerComponentImplementation(ModulePathMacroManager.class);
  }


 @Override
  public void loadModuleComponents() {
    final IdeaPluginDescriptor[] plugins = PluginManager.getPlugins();
    for (IdeaPluginDescriptor plugin : plugins) {
      if (PluginManager.shouldSkipPlugin(plugin)) continue;
      loadComponentsConfiguration(plugin.getModuleComponents(), plugin, false);
    }
  }

  @Override
  protected boolean isComponentSuitable(Map<String, String> options) {
    if (!super.isComponentSuitable(options)) return false;
    if (options == null) return true;

    Set<String> optionNames = options.keySet();
    for (String optionName : optionNames) {
      if (Comparing.equal(OPTION_WORKSPACE, optionName)) continue;
      if (!parseOptionValue(options.get(optionName)).contains(getOptionValue(optionName))) return false;
    }

    return true;
  }

  private static List<String> parseOptionValue(String optionValue) {
    if (optionValue == null) return new ArrayList<String>(0);
    return Arrays.asList(optionValue.split(";"));
  }

  @Override
  @Nullable
  public VirtualFile getModuleFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void rename(String newName) {
    myName = newName;
  }

  @Override
  @NotNull
  public String getModuleFilePath() {
    return getModuleDirPath();
  }

  @Nullable
  @Override
  public VirtualFile getModuleDir() {
    return myDirVirtualFilePointer.getFile();
  }

  @NotNull
  @Override
  public String getModuleDirPath() {
    return VirtualFileManager.extractPath(getModuleDirUrl());
  }

  @NotNull
  @Override
  public String getModuleDirUrl() {
    return myDirVirtualFilePointer.getUrl();
  }

  @Override
  public synchronized void dispose() {
    isModuleAdded = false;
    disposeComponents();
    Extensions.disposeArea(this);
    super.dispose();
  }

  @Override
  public void projectOpened() {
    for (ModuleComponent component : getComponents(ModuleComponent.class)) {
      try {
        component.projectOpened();
      }
      catch (Exception e) {
        LOGGER.error(e);
      }
    }
  }

  @Override
  public void projectClosed() {
    final ModuleComponent[] components = ArrayUtil.reverseArray(getComponents(ModuleComponent.class));
    for (ModuleComponent component : components) {
      try {
        component.projectClosed();
      }
      catch (Exception e) {
        LOGGER.error(e);
      }
    }
  }

  @Override
  @NotNull
  public Project getProject() {
    return myProject;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  public boolean isLoaded() {
    return isModuleAdded;
  }

  @Override
  public void moduleAdded() {
    isModuleAdded = true;
    for (ModuleComponent component : getComponents(ModuleComponent.class)) {
      component.moduleAdded();
    }
  }

  @Override
  public void setOption(@NotNull String optionName, @NotNull String optionValue) {
    myOptions.put(optionName, optionValue);
  }

  @Override
  public void clearOption(@NotNull String optionName) {
    myOptions.remove(optionName);
  }

  @Override
  public String getOptionValue(@NotNull String optionName) {
    return myOptions.get(optionName);
  }

  @Override
  public GlobalSearchScope getModuleScope() {
    return myModuleScopeProvider.getModuleScope();
  }

  @Override
  public GlobalSearchScope getModuleScope(boolean includeTests) {
    return myModuleScopeProvider.getModuleScope(includeTests);
  }

  @Override
  public GlobalSearchScope getModuleWithLibrariesScope() {
    return myModuleScopeProvider.getModuleWithLibrariesScope();
  }

  @Override
  public GlobalSearchScope getModuleWithDependenciesScope() {
    return myModuleScopeProvider.getModuleWithDependenciesScope();
  }

  @Override
  public GlobalSearchScope getModuleContentScope() {
    return myModuleScopeProvider.getModuleContentScope();
  }

  @Override
  public GlobalSearchScope getModuleContentWithDependenciesScope() {
    return myModuleScopeProvider.getModuleContentWithDependenciesScope();
  }

  @Override
  public GlobalSearchScope getModuleWithDependenciesAndLibrariesScope(boolean includeTests) {
    return myModuleScopeProvider.getModuleWithDependenciesAndLibrariesScope(includeTests);
  }

  @Override
  public GlobalSearchScope getModuleWithDependentsScope() {
    return myModuleScopeProvider.getModuleWithDependentsScope();
  }

  @Override
  public GlobalSearchScope getModuleTestsWithDependentsScope() {
    return myModuleScopeProvider.getModuleTestsWithDependentsScope();
  }

  @Override
  public GlobalSearchScope getModuleRuntimeScope(boolean includeTests) {
    return myModuleScopeProvider.getModuleRuntimeScope(includeTests);
  }

  @Override
  public void clearScopesCache() {
    myModuleScopeProvider.clearCache();
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    if (myName == null) return "Module (not initialized)";
    return "Module: '" + getName() + "'";
  }

  @Override
  public <T> T[] getExtensions(final ExtensionPointName<T> extensionPointName) {
    return Extensions.getArea(this).getExtensionPoint(extensionPointName).getExtensions();
  }

  @Override
  protected boolean logSlowComponents() {
    return super.logSlowComponents() || ApplicationInfoImpl.getShadowInstance().isEAP();
  }

  @Override
  protected MutablePicoContainer createPicoContainer() {
    return Extensions.getArea(this).getPicoContainer();
  }
}
