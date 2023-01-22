/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.remoteServer.impl.configuration.deploySource.impl;

import consulo.application.AllIcons;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.virtualFileSystem.VirtualFile;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.remoteServer.configuration.deployment.ModuleDeploymentSource;
import consulo.ide.impl.idea.remoteServer.impl.configuration.deploySource.ModuleDeploymentSourceType;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ui.image.Image;
import consulo.component.util.pointer.NamedPointer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author nik
 */
public class ModuleDeploymentSourceImpl implements ModuleDeploymentSource {
  private final NamedPointer<Module> myPointer;

  public ModuleDeploymentSourceImpl(@Nonnull NamedPointer<Module> pointer) {
    myPointer = pointer;
  }

  @Nonnull
  public NamedPointer<Module> getModulePointer() {
    return myPointer;
  }

  @Nullable
  public Module getModule() {
    return myPointer.get();
  }

  @Override
  @Nullable
  public VirtualFile getContentRoot() {
    Module module = myPointer.get();
    if (module == null) {
      return null;
    }
    return ArrayUtil.getFirstElement(ModuleRootManager.getInstance(module).getContentRoots());
  }

  @javax.annotation.Nullable
  @Override
  public File getFile() {
    VirtualFile contentRoot = getContentRoot();
    if (contentRoot == null) {
      return null;
    }
    return VfsUtilCore.virtualToIoFile(contentRoot);
  }

  @Nullable
  @Override
  public String getFilePath() {
    File file = getFile();
    if (file == null) {
      return null;
    }
    return file.getAbsolutePath();
  }

  @Nonnull
  @Override
  public String getPresentableName() {
    return myPointer.getName();
  }

  @Nullable
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Module;
  }

  @Override
  public boolean isValid() {
    return getModule() != null;
  }

  @Override
  public boolean isArchive() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModuleDeploymentSource)) return false;

    return myPointer.equals(((ModuleDeploymentSource)o).getModulePointer());
  }

  @Override
  public int hashCode() {
    return myPointer.hashCode();
  }

  @Nonnull
  @Override
  public DeploymentSourceType<?> getType() {
    return DeploymentSourceType.EP_NAME.findExtension(ModuleDeploymentSourceType.class);
  }
}
