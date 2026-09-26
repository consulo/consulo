/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalSystem.impl.internal.autoimport.changes;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.FileDocumentManagerListener;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ExternalDocumentChangeTracker implements FileDocumentManagerListener {
    private final Set<Document> myReloadingDocuments = ConcurrentHashMap.newKeySet();

    private ExternalDocumentChangeTracker() {
    }

    public static ExternalDocumentChangeTracker install(Disposable parentDisposable) {
        ExternalDocumentChangeTracker tracker = new ExternalDocumentChangeTracker();
        Application.get().getMessageBus().connect(parentDisposable).subscribe(FileDocumentManagerListener.class, tracker);
        return tracker;
    }

    @Override
    public void beforeFileContentReload(VirtualFile file, Document document) {
        myReloadingDocuments.add(document);
    }

    @Override
    public void fileContentReloaded(VirtualFile file, Document document) {
        myReloadingDocuments.remove(document);
    }

    public boolean isExternalChangeInProgress(Document document) {
        return myReloadingDocuments.contains(document);
    }
}
