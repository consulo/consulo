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
package consulo.document.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-08-05
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface DocumentFactory {
    @Deprecated
    static DocumentFactory getInstance() {
        return Application.get().getInstance(DocumentFactory.class);
    }

    @Nonnull
    default DocumentEx createDocument(@Nonnull CharSequence chars) {
        return createDocument(chars, false);
    }

    /**
     * NOTE: if client sets forUseInNonAWTThread to true it's supposed that client will completely control document and its listeners.
     * The noticeable peculiarity of DocumentImpl behavior in this mode is that DocumentImpl won't suppress ProcessCancelledException
     * thrown from listeners during changedUpdate event, so the exception will be rethrown and rest of the listeners WON'T be notified.
     */
    @Nonnull
    default DocumentEx createDocument(@Nonnull CharSequence chars, boolean forUseInNonAWTThread) {
        return createDocument(chars, false, forUseInNonAWTThread);
    }

    @Nonnull
    DocumentEx createDocument(@Nonnull CharSequence chars, boolean acceptSlashR, boolean forUseInNonAWTThread);
}
