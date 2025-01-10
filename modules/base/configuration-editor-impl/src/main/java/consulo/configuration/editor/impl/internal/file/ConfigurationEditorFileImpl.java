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
package consulo.configuration.editor.impl.internal.file;

import consulo.configuration.editor.ConfigurationFileEditorProvider;
import consulo.configuration.editor.internal.ConfigurationEditorVirtualFile;
import consulo.ui.ex.action.Presentation;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
public class ConfigurationEditorFileImpl extends LightVirtualFileBase implements VirtualFileWithoutContent, ConfigurationEditorVirtualFile {
    private final String myPath;
    private final ConfigurationEditorFileSystemImpl myFileSystem;
    private final Map<String, String> myRequestedParams;

    public ConfigurationEditorFileImpl(String path,
                                       ConfigurationEditorFileType type,
                                       ConfigurationEditorFileSystemImpl fileSystem,
                                       Map<String, String> requestedParams) {
        super(type.getId(), type, 0);
        myPath = path;
        myFileSystem = fileSystem;
        myRequestedParams = requestedParams;
    }

    @Nonnull
    @Override
    public Map<String, String> getRequestedParams() {
        return myRequestedParams;
    }

    @Nonnull
    @Override
    public ConfigurationEditorFileSystemImpl getFileSystem() {
        return myFileSystem;
    }

    @Nonnull
    @Override
    public String getPath() {
        return myPath;
    }

    @Nonnull
    @Override
    public String getName() {
        return getProvider().getName().map(Presentation.NO_MNEMONIC).get();
    }

    @Override
    @Nonnull
    public ConfigurationFileEditorProvider getProvider() {
        return ((ConfigurationEditorFileType) getFileType()).getProvider();
    }

    @Nonnull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        return ArrayUtil.EMPTY_BYTE_ARRAY;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ConfigurationEditorFileImpl cef) {
            return Objects.equals(getProvider().getId(), cef.getProvider().getId());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getProvider().getId().hashCode();
    }
}
