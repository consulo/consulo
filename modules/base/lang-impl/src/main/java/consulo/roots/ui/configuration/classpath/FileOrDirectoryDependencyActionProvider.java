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
package consulo.roots.ui.configuration.classpath;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.localize.LocalizeValue;
import consulo.roots.ModuleRootLayer;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.fileChooser.FileChooser;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 27.09.14
 */
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
