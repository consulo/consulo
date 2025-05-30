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
package consulo.application.io;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2025-04-02
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface SafeOutputStreamFactory {
    @Nonnull
    default SafeOutputStream create(@Nonnull File target) {
        return create(target.toPath());
    }

    @Nonnull
    default SafeOutputStream create(@Nonnull File target, @Nonnull String backupExt) {
        return create(target.toPath(), backupExt);
    }

    @Nonnull
    SafeOutputStream create(@Nonnull Path target);

    @Nonnull
    SafeOutputStream create(@Nonnull Path target, @Nonnull String backupExt);
}
