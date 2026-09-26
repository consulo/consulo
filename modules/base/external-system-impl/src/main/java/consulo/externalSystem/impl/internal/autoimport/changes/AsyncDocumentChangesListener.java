// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.externalSystem.impl.internal.observable.AtomicOperationTrace;
import consulo.virtualFileSystem.VirtualFile;

import java.util.function.Predicate;

import static consulo.externalSystem.autoimport.ExternalSystemModificationType.INTERNAL;

public class AsyncDocumentChangesListener implements DocumentListener {
    private final boolean myIgnoreExternalChanges;
    private final AsyncFileChangesListener myListener;
    private final Predicate<Document> myExternalModification;

    private final AtomicOperationTrace myBulkUpdateOperation = new AtomicOperationTrace("Bulk document update operation");

    public AsyncDocumentChangesListener(
        boolean isIgnoreExternalChanges,
        AsyncFileChangesListener listener,
        Predicate<Document> externalModification
    ) {
        myIgnoreExternalChanges = isIgnoreExternalChanges;
        myListener = listener;
        myExternalModification = externalModification;

        myBulkUpdateOperation.whenOperationStarted(listener::init);
        myBulkUpdateOperation.whenOperationFinished(listener::apply);
    }

    @Override
    public void documentChanged(DocumentEvent event) {
        Document document = event.getDocument();
        if (myIgnoreExternalChanges && myExternalModification.test(document)) {
            return;
        }
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        VirtualFile file = fileDocumentManager.getFile(document);
        if (file == null) {
            return;
        }
        if (myBulkUpdateOperation.isOperationInProgress()) {
            myListener.onFileChange(file.getPath(), document.getModificationStamp(), INTERNAL);
        }
        else {
            myListener.init();
            myListener.onFileChange(file.getPath(), document.getModificationStamp(), INTERNAL);
            myListener.apply();
        }
    }

    @Override
    public void bulkUpdateStarting(Document document) {
        myBulkUpdateOperation.traceStart();
    }

    @Override
    public void bulkUpdateFinished(Document document) {
        myBulkUpdateOperation.traceFinish();
    }
}
