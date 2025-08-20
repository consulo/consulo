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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ComponentScope;
import consulo.application.Application;
import consulo.component.bind.InjectingBinding;
import consulo.component.impl.internal.BaseComponentManager;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightProject extends BaseComponentManager implements Project {
  @Nonnull
  private final Application myApplication;
  @Nonnull
  private final String myName;
  @Nonnull
  private final LightExtensionRegistrator myRegistrator;

  public LightProject(@Nonnull Application application,
                      @Nonnull String name,
                      ComponentBinding componentBinding,
                      @Nonnull LightExtensionRegistrator registrator) {
    super(application, name, ComponentScope.PROJECT, componentBinding, false);
    myApplication = application;
    myName = name;
    myRegistrator = registrator;

    buildInjectingContainer();
  }

  @Override
  public int getProfiles() {
    return ComponentProfiles.LIGHT_TEST;
  }

  @Nonnull
  @Override
  public Application getApplication() {
    return myApplication;
  }

  @Override
  protected void fillListenerDescriptors(MultiMap<String, InjectingBinding> mapByTopic) {
  }

  @Nonnull
  @Override
  protected InjectingContainer findRootContainer() {
    return InjectingContainer.root(getClass().getClassLoader());
  }

  @Override
  protected void registerServices(InjectingContainerBuilder builder) {
    myRegistrator.registerServices(builder);
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
