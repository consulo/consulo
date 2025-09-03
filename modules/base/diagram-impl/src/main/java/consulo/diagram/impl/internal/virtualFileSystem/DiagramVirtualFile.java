/*
 * Copyright 2013-2025 consulo.io
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
package consulo.diagram.impl.internal.virtualFileSystem;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.extension.ExtensionPoint;
import consulo.diagram.GraphProvider;
import consulo.project.Project;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-09-02
 */
public class DiagramVirtualFile extends LightVirtualFileBase {
    private final String myPath;
    private final VirtualFileSystem myFileSystem;

    public DiagramVirtualFile(String name, String path, VirtualFileSystem fileSystem) {
        super(name, DiagramFileType.INSTANCE, System.currentTimeMillis());
        myPath = path;
        myFileSystem = fileSystem;
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public <V> Map.Entry<GraphProvider<V>, V> resolve(@Nonnull Project project) {
        String path = getPath();

        int i = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        if (i == -1) {
            return null;
        }

        String providerId = path.substring(0, i);
        if (providerId.charAt(0) == '/') {
            providerId = providerId.substring(1, providerId.length());
        }

        String elsePart = path.substring(i + URLUtil.ARCHIVE_SEPARATOR.length(), path.length());

        int j = elsePart.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        if (j == -1) {
            return null;
        }

        String providerPath = elsePart.substring(j + URLUtil.ARCHIVE_SEPARATOR.length(), elsePart.length());

        ExtensionPoint<GraphProvider> point = project.getApplication().getExtensionPoint(GraphProvider.class);

        final String finalProviderId = providerId;
        GraphProvider provider = point.findFirstSafe(graphProvider -> Objects.equals(finalProviderId, graphProvider.getId()));
        if (provider == null) {
            return null;
        }

        Object restored = provider.restoreFromURL(project, providerPath);
        if (restored == null) {
            return null;
        }
        return Map.entry(provider, (V) restored);
    }

    @Nonnull
    @Override
    public String getPath() {
        return myPath;
    }

    @Nonnull
    @Override
    public VirtualFileSystem getFileSystem() {
        return myFileSystem;
    }

    @Nonnull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }
}
