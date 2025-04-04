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
package consulo.application.impl.internal.io;

import consulo.annotation.component.ServiceImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.io.SafeOutputStream;
import consulo.application.io.SafeOutputStreamFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2025-04-02
 */
@Singleton
@ServiceImpl
public class SafeOutputStreamFactoryImpl implements SafeOutputStreamFactory {
    private final ApplicationConcurrency myApplicationConcurrency;

    @Inject
    public SafeOutputStreamFactoryImpl(ApplicationConcurrency applicationConcurrency) {
        myApplicationConcurrency = applicationConcurrency;
    }

    @Nonnull
    @Override
    public SafeOutputStream create(@Nonnull Path target) {
        return new SafeFileOutputStreamImpl(myApplicationConcurrency, target);
    }

    @Nonnull
    @Override
    public SafeOutputStream create(@Nonnull Path target, @Nonnull String backupExt) {
        return new SafeFileOutputStreamImpl(myApplicationConcurrency, target, backupExt);
    }
}
