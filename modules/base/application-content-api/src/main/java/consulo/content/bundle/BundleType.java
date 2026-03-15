/*
 * Copyright 2013-2023 consulo.io
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
package consulo.content.bundle;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public abstract class BundleType extends SdkType {
    public BundleType(String id, LocalizeValue displayName, Image icon) {
        super(id, displayName, icon);
    }

    public abstract void collectHomePaths(Platform platform, Consumer<Path> pathConsumer);

    public boolean canCreatePredefinedSdks(Platform platform) {
        return false;
    }

    public boolean isValidSdkHome(Platform platform, Path path) {
        return getVersionString(platform, path) != null;
    }

    
    public String suggestSdkName(Platform platform, @Nullable String currentSdkName, Path path) {
        return getDisplayName().get() + " " + getVersionString(platform, path);
    }

    @Nullable
    public abstract String getVersionString(Platform platform, Path path);

    /**
     * If a path selected in the file chooser is not a valid SDK home path, returns an adjusted version of the path that is again
     * checked for validity.
     *
     * @param homePath the path selected in the file chooser.
     * @return the path to be used as the SDK home.
     */
    
    public Path adjustSelectedSdkHome(Platform platform, Path homePath) {
        return homePath;
    }

    
    public FileChooserDescriptor getHomeChooserDescriptor(Platform platform) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
            @Override
            public void validateSelectedFiles(VirtualFile[] files) throws Exception {
                if (files.length != 0) {
                    Path selectedPath = files[0].toNioPath();
                    boolean valid = isValidSdkHome(platform, selectedPath);
                    if (!valid) {
                        valid = isValidSdkHome(platform, adjustSelectedSdkHome(platform, selectedPath));
                        if (!valid) {
                            LocalizeValue message = files[0].isDirectory()
                                ? ProjectLocalize.sdkConfigureHomeInvalidError(getDisplayName())
                                : ProjectLocalize.sdkConfigureHomeFileInvalidError(getDisplayName());
                            throw new Exception(message.get());
                        }
                    }
                }
            }
        };
        descriptor.withTitleValue(ProjectLocalize.sdkConfigureHomeTitle(getDisplayName()));
        return descriptor;
    }

    // region obsolete stuff

    
    @Override
    @Deprecated
    public final FileChooserDescriptor getHomeChooserDescriptor() {
        throw new UnsupportedOperationException();
    }

    
    @Override
    @Deprecated
    public final String suggestSdkName(String currentSdkName, String sdkHome) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final String adjustSelectedSdkHome(String homePath) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    @Deprecated
    public final String getVersionString(String sdkHome) {
        throw new UnsupportedOperationException();
    }

    
    @Override
    @Deprecated
    public final Collection<String> suggestHomePaths() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final boolean isValidSdkHome(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final boolean canCreatePredefinedSdks() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final String sdkPath(VirtualFile homePath) {
        throw new UnsupportedOperationException();
    }
    // endregion
}
