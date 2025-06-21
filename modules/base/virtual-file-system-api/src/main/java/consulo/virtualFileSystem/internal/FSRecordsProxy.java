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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.virtualFileSystem.FileAttribute;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import java.io.DataInputStream;
import java.io.DataOutputStream;

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

    @Nonnull
    DataOutputStream writeAttribute(int fileId, @Nonnull FileAttribute att);

    @Nullable
    DataInputStream readAttributeWithLock(int fileId, @Nonnull FileAttribute att);
}
