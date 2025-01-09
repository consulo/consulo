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
import consulo.ui.ex.action.Presentation;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFileWithoutContent;
import consulo.virtualFileSystem.light.LightVirtualFileBase;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
public class ConfigurationEditorFile extends LightVirtualFileBase implements VirtualFileWithoutContent {
    private final ConfigurationEditorFileSystemImpl myFileSystem;

    public ConfigurationEditorFile(ConfigurationEditorFileType type, ConfigurationEditorFileSystemImpl fileSystem) {
        super(type.getId(), type, 0);
        myFileSystem = fileSystem;
    }

    @Nonnull
    @Override
    public ConfigurationEditorFileSystemImpl getFileSystem() {
        return myFileSystem;
    }

    @Nonnull
    @Override
    public String getPath() {
        return "/" + getProvider().getId();
    }

    @Nonnull
    @Override
    public String getName() {
        return getProvider().getName().map(Presentation.NO_MNEMONIC).get();
    }

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
        if (obj instanceof ConfigurationEditorFile cef) {
            return Objects.equals(getUrl(), cef.getUrl());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getUrl().hashCode();
    }
}
