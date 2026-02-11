/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.awt.platform.impl;

import consulo.desktop.startup.DesktopPlatformUserImpl;
import consulo.platform.PlatformFeature;
import consulo.platform.PlatformFileSystem;
import consulo.platform.PlatformUser;
import consulo.platform.impl.PlatformBase;
import consulo.ui.UIAccess;
import consulo.webBrowser.BrowserUtil;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
class DesktopAWTPlatformImpl extends PlatformBase {
    public DesktopAWTPlatformImpl() {
        super(LOCAL, LOCAL, getSystemJvmProperties());
    }

    @Override
    public void openInBrowser(@Nonnull URL url) {
        BrowserUtil.browse(url);
    }

    @Nonnull
    @Override
    public String fileManagerName() {
        return FileManagerProxy.getFileManagerName();
    }

    @Override
    public boolean supportsFeature(@Nonnull PlatformFeature feature) {
        switch (feature) {
            case OPEN_DIRECTORY_IN_FILE_MANAGER:
            case OPEN_FILE_IN_FILE_MANAGER:
                return FileManagerProxy.isSupported();
        }
        return false;
    }

    @Override
    public void openFileInFileManager(@Nonnull File file, @Nonnull UIAccess uiAccess) {
        FileManagerProxy.openFile(file, uiAccess);
    }

    @Override
    public void openDirectoryInFileManager(@Nonnull File file, @Nonnull UIAccess uiAccess) {
        FileManagerProxy.openDirectory(file, uiAccess);
    }

    @Nonnull
    @Override
    protected PlatformUser createUser(Map<String, String> jvmProperties) {
        return new DesktopPlatformUserImpl(this, jvmProperties);
    }

    @Nonnull
    @Override
    protected PlatformFileSystem createFS(Map<String, String> jvmProperties) {
        return new DesktopAWTFileSystemImpl(this, jvmProperties);
    }
}
