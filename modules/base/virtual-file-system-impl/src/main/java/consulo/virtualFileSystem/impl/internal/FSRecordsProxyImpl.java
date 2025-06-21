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
package consulo.virtualFileSystem.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.virtualFileSystem.FileAttribute;
import consulo.virtualFileSystem.internal.FSRecordsProxy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * @author VISTALL
 * @since 2025-06-21
 */
@ServiceImpl
@Singleton
public class FSRecordsProxyImpl implements FSRecordsProxy {
    @Override
    public void handleError(Throwable e) throws RuntimeException, Error {
        FSRecords.handleError(e);
    }

    @Nonnull
    @Override
    public DataOutputStream writeAttribute(int fileId, @Nonnull FileAttribute att) {
        return FSRecords.writeAttribute(fileId, att);
    }

    @Nullable
    @Override
    public DataInputStream readAttributeWithLock(int fileId, @Nonnull FileAttribute att) {
        return FSRecords.readAttributeWithLock(fileId, att);
    }
}
