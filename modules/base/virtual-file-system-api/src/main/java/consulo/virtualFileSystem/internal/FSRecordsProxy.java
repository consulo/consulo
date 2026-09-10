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
package consulo.virtualFileSystem.internal;

import com.uber.nullaway.annotations.Contract;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.virtualFileSystem.FileAttribute;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 2025-06-21
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface FSRecordsProxy {
    static FSRecordsProxy getInstance() {
        return Application.get().getInstance(FSRecordsProxy.class);
    }

    @Contract("_->fail")
    void handleError(Throwable e) throws RuntimeException, Error;

    DataOutputStream writeAttribute(int fileId, FileAttribute att);

    @Nullable DataInputStream readAttributeWithLock(int fileId, FileAttribute att);

    long getCreationTimestamp();

    int getMaxId();

    /** Adds an object which must be closed during VFS close process */
    void addCloseable(Closeable closeable);

    /**
     * Registers a storage keeping some data by fileId.
     * Since we reuse fileId of removed files, we need to be sure all data attached to the re-used fileId was
     * cleaned before re-use -- hence a storage that keeps such data should implement {@link FileIdIndexedStorage}
     * interface, and should be registered with that method (or invent own method to keep track of removed files)
     */
    void addFileIdIndexedStorage(FileIdIndexedStorage storage);

    /**
     * Any storage keeping some data by fileId.
     * Since we reuse fileId of removed files, we need to be sure all data attached to the re-used fileId was
     * cleaned before re-use -- hence every storage that keeps such data should implement this interface, and
     * should be registered {@link #addFileIdIndexedStorage(FileIdIndexedStorage)}
     */
    interface FileIdIndexedStorage {
        void clear(int fileId) throws IOException;
    }
}
