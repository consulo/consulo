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
package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.component.util.pointer.NamedPointer;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.remoteServer.configuration.deployment.ModuleDeploymentSource;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * @author nik
 */
public class ModuleDeploymentSourceImpl implements ModuleDeploymentSource {
    private final NamedPointer<Module> myPointer;

    public ModuleDeploymentSourceImpl(@Nonnull NamedPointer<Module> pointer) {
        myPointer = pointer;
    }

    @Override
    @Nonnull
    public NamedPointer<Module> getModulePointer() {
        return myPointer;
    }

    @Override
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

    @Nullable
    @Override
    public File getFile() {
        VirtualFile contentRoot = getContentRoot();
        if (contentRoot == null) {
            return null;
        }
        return VirtualFileUtil.virtualToIoFile(contentRoot);
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
    public LocalizeValue getPresentableName() {
        return LocalizeValue.of(myPointer.getName());
    }

    @Nullable
    @Override
    public Image getIcon() {
        return PlatformIconGroup.nodesModule();
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
        return o == this
            || o instanceof ModuleDeploymentSource source
            && myPointer.equals(source.getModulePointer());
    }

    @Override
    public int hashCode() {
        return myPointer.hashCode();
    }

    @Nonnull
    @Override
    public DeploymentSourceType<?> getType() {
        return DeploymentSourceType.EP_NAME.findExtensionOrFail(ModuleDeploymentSourceType.class);
    }
}
