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
import consulo.diagram.DiagramElementManager;
import consulo.diagram.DiagramProvider;
import consulo.project.Project;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public <T> @Nullable DiagramTarget<T> resolve(Project project) {
        String path = getPath();
        int i = path.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        if (i == -1) {
            return null;
        }

        String providerId = path.substring(0, i);
        if (providerId.charAt(0) == '/') {
            providerId = providerId.substring(1);
        }

        String elsePart = path.substring(i + URLUtil.ARCHIVE_SEPARATOR.length());
        int j = elsePart.indexOf(URLUtil.ARCHIVE_SEPARATOR);
        if (j == -1) {
            return null;
        }

        String fqn = elsePart.substring(j + URLUtil.ARCHIVE_SEPARATOR.length());

        DiagramProvider<T> provider = (DiagramProvider<T>) DiagramProvider.findByID(providerId);
        if (provider == null) {
            return null;
        }

        T element = provider.getVfsResolver().resolveElementByFQN(fqn, project);
        if (element == null) {
            return null;
        }
        return new DiagramTarget<>(provider, element);
    }

    public static <T> String buildPath(DiagramProvider<T> provider, T element) {
        DiagramElementManager<T> elementManager = provider.getElementManager();
        String title = elementManager.getElementTitle(element);
        return provider.getID() + URLUtil.ARCHIVE_SEPARATOR + (title == null ? provider.getID() : title) + URLUtil.ARCHIVE_SEPARATOR
            + provider.getVfsResolver().getQualifiedName(element);
    }

    @Override
    public String getPath() {
        return myPath;
    }

    
    @Override
    public VirtualFileSystem getFileSystem() {
        return myFileSystem;
    }

    
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public byte[] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
    }
}
