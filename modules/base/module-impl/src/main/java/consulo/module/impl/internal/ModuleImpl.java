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

package consulo.module.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.application.impl.internal.BaseApplication;
import consulo.application.impl.internal.PlatformComponentManagerImpl;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.component.store.impl.internal.IComponentStore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.extension.ModuleExtension;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class ModuleImpl extends PlatformComponentManagerImpl implements ModuleEx {
  @Nonnull
  private String myName;

  @Nullable
  private final VirtualFilePointer myDirVirtualFilePointer;

  public ModuleImpl(@Nonnull String name, @Nullable String dirUrl, @Nonnull Project project, @Nonnull ComponentBinding componentBinding) {
    super(project, "Module " + name, ComponentScope.MODULE, componentBinding);
    myName = name;
    myDirVirtualFilePointer = dirUrl == null ? null : VirtualFilePointerManager.getInstance().create(dirUrl, this, null);
  }

  @Override
  public void executeNonCancelableSection(@Nonnull Runnable runnable) {
    PlatformComponentManagerImpl application = (BaseApplication)getApplication();
    application.executeNonCancelableSection(runnable);
  }

  @Nullable
  @Override
  protected IComponentStore getStateStore() {
    return null;
  }

  @Override
  public int getProfiles() {
    return myParent.getProfiles();
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(Module.class).to(this);
  }

  @Override
  public void rename(String newName) {
    myName = newName;
  }

  @Nullable
  @Override
  public VirtualFile getModuleDir() {
    return myDirVirtualFilePointer == null ? null : myDirVirtualFilePointer.getFile();
  }

  @Nullable
  @Override
  public String getModuleDirPath() {
    return myDirVirtualFilePointer == null ? null : VirtualFileManager.extractPath(myDirVirtualFilePointer.getUrl());
  }

  @Nullable
  @Override
  public String getModuleDirUrl() {
    return myDirVirtualFilePointer == null ? null : myDirVirtualFilePointer.getUrl();
  }

  @Override
  @Nonnull
  public Project getProject() {
    return (Project)myParent;
  }

  @Override
  @Nonnull
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public <T extends ModuleExtension<T>> T getExtension(@Nonnull String key) {
    ModuleRootManagerImpl manager = (ModuleRootManagerImpl)ModuleRootManager.getInstance(this);
    return manager.getExtension(key);
  }

  @Nullable
  @Override
  public <T extends ModuleExtension<T>> T getExtension(@Nonnull Class<T> clazz) {
    ModuleRootManagerImpl manager = (ModuleRootManagerImpl)ModuleRootManager.getInstance(this);
    return manager.getExtension(clazz);
  }

  @Override
  public void moduleAdded() {
    ModuleRootManagerImpl manager = (ModuleRootManagerImpl)ModuleRootManager.getInstance(this);

    manager.moduleAdded();
  }

  @Override
  public String toString() {
    return "Module: '" + myName + "'";
  }
}
