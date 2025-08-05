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
package consulo.document.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.DocumentFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2025-08-05
 */
@ServiceImpl
@Singleton
public class DocumentFactoryImpl implements DocumentFactory {
    @Nonnull
    @Override
    public DocumentEx createDocument(@Nonnull CharSequence chars, boolean acceptSlashR, boolean forUseInNonAWTThread) {
        return new DocumentImpl(chars, acceptSlashR, forUseInNonAWTThread);
    }
}
