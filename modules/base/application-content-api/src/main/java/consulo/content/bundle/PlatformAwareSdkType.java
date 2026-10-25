/*
 * Copyright 2013-2026 consulo.io
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

import consulo.application.Application;
import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-05-17
 */
public abstract class PlatformAwareSdkType extends SdkType {
    public PlatformAwareSdkType(String id, LocalizeValue displayName, Image icon) {
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

    public abstract @Nullable String getVersionString(Platform platform, Path path);

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
            public void validateSelectedPaths(List<Path> paths) throws Exception {
                if (!paths.isEmpty()) {
                    Path selectedPath = paths.get(0);
                    Platform selectedPlatform = platformOf(selectedPath, platform);
                    boolean valid = isValidSdkHome(selectedPlatform, selectedPath);
                    if (!valid) {
                        valid = isValidSdkHome(selectedPlatform, adjustSelectedSdkHome(selectedPlatform, selectedPath));
                        if (!valid) {
                            LocalizeValue message = Files.isDirectory(selectedPath)
                                ? ProjectLocalize.sdkConfigureHomeInvalidError(getDisplayName())
                                : ProjectLocalize.sdkConfigureHomeFileInvalidError(getDisplayName());
                            throw new Exception(message.get());
                        }
                    }
                }
            }
        };
        descriptor.withTitle(ProjectLocalize.sdkConfigureHomeTitle(getDisplayName()));
        descriptor.withPlatform(platform);
        return descriptor;
    }

    /**
     * The machine a chosen path is on - the chooser may offer several of them.
     */
    static Platform platformOf(Path path, Platform fallback) {
        PlatformTarget target = Application.get().getInstance(PlatformTargetManager.class).findTargetOf(path);
        Platform platform = target == null ? null : target.getPlatform();
        return platform == null ? fallback : platform;
    }

    // region obsolete stuff

    @Override
    @Deprecated
    public final FileChooserDescriptor getHomeChooserDescriptor() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final String suggestSdkName(@Nullable String currentSdkName, String sdkHome) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final String adjustSelectedSdkHome(String homePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final @Nullable String getVersionString(String sdkHome) {
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
