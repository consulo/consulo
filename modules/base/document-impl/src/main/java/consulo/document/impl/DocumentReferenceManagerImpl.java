// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.document.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.document.Document;
import consulo.document.DocumentReference;
import consulo.document.DocumentReferenceManager;
import consulo.document.FileDocumentManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Maps;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SoftReference;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
@ServiceImpl
public final class DocumentReferenceManagerImpl extends DocumentReferenceManager {
    private static final Key<List<VirtualFile>> DELETED_FILES = Key.create(DocumentReferenceManagerImpl.class.getName() + ".DELETED_FILES");

    private final Map<Document, DocumentReference> myDocToRef = ContainerUtil.createWeakKeyWeakValueMap();

    private static final Key<Reference<DocumentReference>> FILE_TO_REF_KEY = Key.create("FILE_TO_REF_KEY");
    private static final Key<DocumentReference> FILE_TO_STRONG_REF_KEY = Key.create("FILE_TO_STRONG_REF_KEY");
    private final Map<DocumentFilePath, DocumentReference> myDeletedFilePathToRef = Maps.newWeakValueHashMap();

    @Nonnull
    private final Application myApplication;

    @Inject
    DocumentReferenceManagerImpl(@Nonnull Application application) {
        myApplication = application;
        application.getMessageBus().connect().subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void before(@Nonnull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event instanceof VFileDeleteEvent) {
                        beforeFileDeletion((VFileDeleteEvent) event);
                    }
                }
            }

            @Override
            public void after(@Nonnull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event instanceof VFileCreateEvent) {
                        fileCreated((VFileCreateEvent) event);
                    }
                    else if (event instanceof VFileDeleteEvent) {
                        fileDeleted((VFileDeleteEvent) event);
                    }
                }
            }

            private void beforeFileDeletion(@Nonnull VFileDeleteEvent event) {
                VirtualFile f = event.getFile();
                f.putUserData(DELETED_FILES, collectDeletedFiles(f, new ArrayList<>()));
            }

            private void fileDeleted(@Nonnull VFileDeleteEvent event) {
                VirtualFile f = event.getFile();
                List<VirtualFile> files = f.getUserData(DELETED_FILES);
                f.putUserData(DELETED_FILES, null);

                assert files != null : f;
                for (VirtualFile each : files) {
                    DocumentReference ref = SoftReference.dereference(each.getUserData(FILE_TO_REF_KEY));
                    each.putUserData(FILE_TO_REF_KEY, null);
                    if (ref != null) {
                        myDeletedFilePathToRef.put(new DocumentFilePath(each.getUrl()), ref);
                    }
                }
            }

            private void fileCreated(@Nonnull VFileCreateEvent event) {
                VirtualFile f = event.getFile();
                DocumentReference ref = f == null ? null : myDeletedFilePathToRef.remove(new DocumentFilePath(f.getUrl()));
                if (ref != null) {
                    f.putUserData(FILE_TO_REF_KEY, new WeakReference<>(ref));
                    ((DocumentReferenceByVirtualFile) ref).update(f);
                }
            }
        });
    }

    @Nonnull
    private static List<VirtualFile> collectDeletedFiles(@Nonnull VirtualFile f, @Nonnull List<VirtualFile> files) {
        if (!(f instanceof NewVirtualFile)) {
            return files;
        }

        if (!f.isDirectory()) {
            files.add(f);
        }
        else {
            for (VirtualFile each : ((NewVirtualFile) f).iterInDbChildren()) {
                collectDeletedFiles(each, files);
            }
        }
        return files;
    }

    @Nonnull
    @Override
    public DocumentReference create(@Nonnull Document document) {
        myApplication.assertIsWriteThread();

        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return file == null ? createFromDocument(document) : create(file);
    }

    @Nonnull
    private DocumentReference createFromDocument(@Nonnull Document document) {
        DocumentReference result = myDocToRef.get(document);
        if (result == null) {
            result = new DocumentReferenceByDocument(document);
            myDocToRef.put(document, result);
        }
        return result;
    }

    @Nonnull
    @Override
    public DocumentReference create(@Nonnull VirtualFile file) {
        myApplication.assertIsWriteThread();

        if (!file.isInLocalFileSystem()) { // we treat local files differently from non local because we can undo their deletion
            DocumentReference reference = file.getUserData(FILE_TO_STRONG_REF_KEY);
            if (reference == null) {
                file.putUserData(FILE_TO_STRONG_REF_KEY, reference = new DocumentReferenceByNonlocalVirtualFile(file));
            }
            return reference;
        }

        assert file.isValid() : "file is invalid: " + file;

        DocumentReference result = SoftReference.dereference(file.getUserData(FILE_TO_REF_KEY));
        if (result == null) {
            result = new DocumentReferenceByVirtualFile(file);
            file.putUserData(FILE_TO_REF_KEY, new WeakReference<>(result));
        }
        return result;
    }

    @TestOnly
    public void cleanupForNextTest() {
        myDeletedFilePathToRef.clear();
        myDocToRef.clear();
    }
}
