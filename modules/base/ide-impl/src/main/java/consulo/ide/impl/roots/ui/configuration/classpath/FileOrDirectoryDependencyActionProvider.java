/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.roots.ui.configuration.classpath;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.setting.module.AddModuleDependencyActionProvider;
import consulo.ide.setting.module.ClasspathPanel;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ide.setting.module.ModulesConfigurator;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 27.09.14
 */
@ExtensionImpl
public class FileOrDirectoryDependencyActionProvider implements AddModuleDependencyActionProvider<VirtualFile[], FileOrDirectoryDependencyContext> {
  @Override
  public boolean isAvailable(@Nonnull FileOrDirectoryDependencyContext context) {
    return true;
  }

  @Override
  public FileOrDirectoryDependencyContext createContext(@Nonnull ClasspathPanel classpathPanel,
                                                        @Nonnull ModulesConfigurator modulesConfigurator,
                                                        @Nonnull LibrariesConfigurator librariesConfigurator) {
    return new FileOrDirectoryDependencyContext(classpathPanel, modulesConfigurator, librariesConfigurator);
  }

  @Nonnull
  @Override
  public LocalizeValue getActionName(@Nonnull ModuleRootLayer layer) {
    return LocalizeValue.localizeTODO("File or Directory");
  }

  @Nonnull
  @Override
  public Image getIcon(@Nonnull ModuleRootLayer layer) {
    return AllIcons.Nodes.Folder;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<VirtualFile[]> invoke(@Nonnull FileOrDirectoryDependencyContext context) {
    FileChooserDescriptor descriptor = context.getFileChooserDescriptor();
    ClasspathPanel classpathPanel = context.getClasspathPanel();
    return FileChooser.chooseFiles(descriptor, classpathPanel.getComponent(), context.getProject(), classpathPanel.getRootModel().getModule().getModuleDir());
  }
}
