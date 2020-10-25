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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.MultiMap;
import consulo.annotation.access.RequiredWriteAction;
import consulo.container.plugin.PluginListenerDescriptor;
import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightProject extends ComponentManagerImpl implements Project {
  @Nonnull
  private final Application myApplication;
  @Nonnull
  private final String myName;
  @Nonnull
  private final LightExtensionRegistrator myRegistrator;

  public LightProject(@Nonnull Application application, @Nonnull String name, @Nonnull LightExtensionRegistrator registrator) {
    super(application, name, ExtensionAreaId.PROJECT, false);
    myApplication = application;
    myName = name;
    myRegistrator = registrator;

    buildInjectingContainer();
  }

  @Nonnull
  @Override
  public Application getApplication() {
    return myApplication;
  }

  @Override
  protected void fillListenerDescriptors(MultiMap<String, PluginListenerDescriptor> mapByTopic) {
  }

  @Nonnull
  @Override
  protected InjectingContainer findRootContainer() {
    return InjectingContainer.root(getClass().getClassLoader());
  }

  @Override
  protected void registerExtensionPointsAndExtensions(ExtensionsAreaImpl area) {
    myRegistrator.registerExtensionPointsAndExtensions(area);
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

  @RequiredWriteAction
  @Nonnull
  @Override
  public AsyncResult<Void> saveAsync(UIAccess uiAccess) {
    return AsyncResult.resolved();
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
