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
package consulo.repository.ui.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.repository.ui.PackageManagementServiceProvider;
import consulo.virtualFileSystem.BaseVirtualFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@ExtensionImpl
public class PackagesVirtualFileSystem extends BaseVirtualFileSystem {
    public static final String PROTOCOL = "packages";

    private final Application myApplication;
    // one file per target, so the target is opened in the same editor tab
    private final Map<String, PackagesVirtualFile> myFiles = new ConcurrentHashMap<>();

    @Inject
    public PackagesVirtualFileSystem(Application application) {
        myApplication = application;
    }

    public static PackagesVirtualFileSystem getInstance() {
        return (PackagesVirtualFileSystem) Objects.requireNonNull(VirtualFileManager.getInstance().getFileSystem(PROTOCOL));
    }

    public PackagesVirtualFile findFile(PackageManagementServiceProvider provider, String target) {
        return myFiles.computeIfAbsent(PackagesVirtualFile.buildPath(provider.getId(), target),
            path -> new PackagesVirtualFile(provider.getId(), target, provider.getTitle(target).get(), this));
    }

    public @Nullable PackageManagementServiceProvider findProvider(String providerId) {
        return myApplication.getExtensionPoint(PackageManagementServiceProvider.class).findFirstSafe(it -> it.getId().equals(providerId));
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(String path) {
        int separator = path.indexOf('/');
        if (separator <= 0) {
            return null;
        }
        PackageManagementServiceProvider provider = findProvider(path.substring(0, separator));
        if (provider == null) {
            return null;
        }
        return findFile(provider, URLDecoder.decode(path.substring(separator + 1), StandardCharsets.UTF_8));
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(String path) {
        return findFileByPath(path);
    }

    @Override
    public void refresh(boolean asynchronous) {
    }
}
