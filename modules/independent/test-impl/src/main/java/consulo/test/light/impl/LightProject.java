/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.psi.util.PsiModificationTracker;
import consulo.injecting.InjectingContainerBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightProject extends ComponentManagerImpl implements Project {
  @Nonnull
  private final String myName;

  public LightProject(@Nullable ComponentManager parent, @Nonnull String name) {
    super(parent, name, ExtensionAreas.PROJECT);
    myName = name;
  }

  @Override
  protected void registerExtensionPointsAndExtensions(ExtensionsAreaImpl area) {
  }

  @Override
  protected void registerServices(InjectingContainerBuilder builder) {
    builder.bind(PsiFileFactory.class).to(PsiFileFactoryImpl.class);
    builder.bind(PsiManager.class).to(PsiManagerImpl.class);
    builder.bind(FileIndexFacade.class).to(LightFileIndexFacade.class);
    builder.bind(PsiModificationTracker.class).to(PsiModificationTrackerImpl.class);
  }

  @Override
  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
    super.bootstrapInjectingContainer(builder);

    builder.bind(Project.class).to(this);
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nullable
  @Override
  public VirtualFile getBaseDir() {
    return null;
  }

  @Override
  public String getBasePath() {
    return null;
  }

  @Nullable
  @Override
  public VirtualFile getProjectFile() {
    return null;
  }

  @Nonnull
  @Override
  public String getProjectFilePath() {
    return null;
  }

  @Nullable
  @Override
  public String getPresentableUrl() {
    return null;
  }

  @Nullable
  @Override
  public VirtualFile getWorkspaceFile() {
    return null;
  }

  @Nonnull
  @Override
  public String getLocationHash() {
    return null;
  }

  @Override
  public void save() {

  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public boolean isInitialized() {
    return true;
  }

  @Override
  public boolean isDefault() {
    return false;
  }
}
