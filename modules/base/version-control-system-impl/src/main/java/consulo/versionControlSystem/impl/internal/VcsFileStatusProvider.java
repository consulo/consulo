/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.rollback.RollbackEnvironment;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.ReadonlyStatusHandlerInternal;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.status.internal.FileStatusFacade;
import consulo.virtualFileSystem.status.internal.FileStatusManagerInternal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
@Singleton
public class VcsFileStatusProvider implements FileStatusFacade, Disposable {
    private static final Logger LOG = Logger.getInstance(VcsFileStatusProvider.class);

    private final Project myProject;
    private final FileStatusManagerInternal myFileStatusManager;
    private final Provider<ProjectLevelVcsManager> myVcsManager;
    private final Provider<ChangeListManager> myChangeListManager;
    private final Provider<VcsDirtyScopeManager> myDirtyScopeManager;
    private final Provider<VcsConfiguration> myConfiguration;

    private boolean myHaveEmptyContentRevisions;

    private MergingUpdateQueue myMergingUpdateQueue;

    private final Map<VirtualFile, Boolean> myRefreshedFileMap = new HashMap<>();

    @Inject
    public VcsFileStatusProvider(
        Project project,
        FileStatusManager fileStatusManager,
        Provider<ProjectLevelVcsManager> vcsManager,
        Provider<VcsDirtyScopeManager> dirtyScopeManager,
        Provider<VcsConfiguration> configuration,
        Provider<ChangeListManager> changeListManager
    ) {
        myProject = project;
        myChangeListManager = changeListManager;
        myFileStatusManager = (FileStatusManagerInternal) fileStatusManager;
        myVcsManager = vcsManager;
        myDirtyScopeManager = dirtyScopeManager;
        myConfiguration = configuration;
        myHaveEmptyContentRevisions = true;
        myFileStatusManager.setFileStatusProvider(this);

        myMergingUpdateQueue = new MergingUpdateQueue("FileStatusFacade", 100, true, null, this, null, false);

        myProject.getMessageBus().connect().subscribe(ChangeListListener.class, new ChangeListListener() {
            @Override
            public void changeListAdded(ChangeList list) {
                fileStatusesChanged();
            }

            @Override
            public void changeListRemoved(ChangeList list) {
                fileStatusesChanged();
            }

            @Override
            public void changeListChanged(ChangeList list) {
                fileStatusesChanged();
            }

            @Override
            public void changeListUpdateDone() {
                if (myHaveEmptyContentRevisions) {
                    myHaveEmptyContentRevisions = false;
                    fileStatusesChanged();
                }
            }

            @Override
            public void unchangedFileStatusChanged() {
                fileStatusesChanged();
            }
        });
    }

    @Override
    public void dispose() {
        myRefreshedFileMap.clear();
    }

    private void fileStatusesChanged() {
        myFileStatusManager.fileStatusesChanged();
    }

    @Override
    @Nonnull
    public FileStatus getFileStatus(@Nonnull VirtualFile virtualFile) {
        AbstractVcs vcs = myVcsManager.get().getVcsFor(virtualFile);
        if (vcs == null) {
            if (ScratchUtil.isScratch(virtualFile)) {
                return FileStatus.SUPPRESSED;
            }
            return myFileStatusManager.getDefaultStatus(virtualFile);
        }

        FileStatus status = myChangeListManager.get().getStatus(virtualFile);
        if (status == FileStatus.NOT_CHANGED && isDocumentModified(virtualFile)) {
            return FileStatus.MODIFIED;
        }
        if (status == FileStatus.NOT_CHANGED) {
            return myFileStatusManager.getDefaultStatus(virtualFile);
        }
        return status;
    }

    private static boolean isDocumentModified(VirtualFile virtualFile) {
        return !virtualFile.isDirectory() && FileDocumentManager.getInstance().isFileModified(virtualFile);
    }

    @Override
    public void refreshFileStatusFromDocument(@Nonnull VirtualFile virtualFile, @Nonnull Document doc) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("refreshFileStatusFromDocument: file.getModificationStamp()=" + virtualFile.getModificationStamp() + ", document.getModificationStamp()=" + doc.getModificationStamp());
        }
        FileStatus cachedStatus = myFileStatusManager.getCachedStatus(virtualFile);
        if (cachedStatus == null || cachedStatus == FileStatus.NOT_CHANGED || !isDocumentModified(virtualFile)) {
            synchronized (myRefreshedFileMap) {
                myRefreshedFileMap.put(virtualFile, cachedStatus == FileStatus.MODIFIED);
            }

            myMergingUpdateQueue.queue(Update.create("refreshFileStatusFromDocument", this::processModifiedDocuments));
        }
    }

    private void processModifiedDocuments() {
        Map<VirtualFile, Boolean> data;

        synchronized (myRefreshedFileMap) {
            data = new HashMap<>(myRefreshedFileMap);

            myRefreshedFileMap.clear();
        }

        if (data.isEmpty()) {
            return;
        }

        for (Map.Entry<VirtualFile, Boolean> entry : data.entrySet()) {
            processModifiedDocument(entry.getKey(), entry.getValue());
        }
    }

    private void processModifiedDocument(VirtualFile virtualFile, boolean modified) {
        AbstractVcs vcs = myVcsManager.get().getVcsFor(virtualFile);
        if (vcs == null) {
            return;
        }

        if (modified && !isDocumentModified(virtualFile)) {
            ReadonlyStatusHandlerInternal statusHandler = (ReadonlyStatusHandlerInternal) ReadonlyStatusHandler.getInstance(myProject);

            if (!statusHandler.isShowDialog()) {
                RollbackEnvironment rollbackEnvironment = vcs.getRollbackEnvironment();
                if (rollbackEnvironment != null) {
                    rollbackEnvironment.rollbackIfUnchanged(virtualFile);
                }
            }
        }
        myFileStatusManager.fileStatusChanged(virtualFile);
        ChangeProvider cp = vcs.getChangeProvider();
        if (cp != null && cp.isModifiedDocumentTrackingRequired()) {
            myDirtyScopeManager.get().fileDirty(virtualFile);
        }
    }

    @Override
    @Nonnull
    public ThreeState getNotChangedDirectoryParentingStatus(@Nonnull VirtualFile virtualFile) {
        return myConfiguration.get().SHOW_DIRTY_RECURSIVELY ? myChangeListManager.get().haveChangesUnder(virtualFile) : ThreeState.NO;
    }

    @Nullable
    public VcsBaseContentProvider.BaseContent getBaseRevision(@Nonnull VirtualFile file) {
        if (!isHandledByVcs(file)) {
            VcsBaseContentProvider provider = findProviderFor(file);
            return provider == null ? null : provider.getBaseRevision(file);
        }
        Change change = ChangeListManager.getInstance(myProject).getChange(file);
        if (change == null) {
            return null;
        }
        ContentRevision beforeRevision = change.getBeforeRevision();
        if (beforeRevision == null) {
            return null;
        }
        if (beforeRevision instanceof BinaryContentRevision) {
            return null;
        }
        return new BaseContentImpl(beforeRevision);
    }

    @Nullable
    private VcsBaseContentProvider findProviderFor(@Nonnull VirtualFile file) {
        return myProject.getExtensionPoint(VcsBaseContentProvider.class).findFirstSafe(p -> p.isSupported(file));
    }

    public boolean isSupported(@Nonnull VirtualFile file) {
        return isHandledByVcs(file) || findProviderFor(file) != null;
    }

    private boolean isHandledByVcs(@Nonnull VirtualFile file) {
        return file.isInLocalFileSystem() && myVcsManager.get().getVcsFor(file) != null;
    }

    private class BaseContentImpl implements VcsBaseContentProvider.BaseContent {
        @Nonnull
        private final ContentRevision myContentRevision;

        public BaseContentImpl(@Nonnull ContentRevision contentRevision) {
            myContentRevision = contentRevision;
        }

        @Nonnull
        @Override
        public VcsRevisionNumber getRevisionNumber() {
            return myContentRevision.getRevisionNumber();
        }

        @Nullable
        @Override
        public String loadContent() {
            String content;
            try {
                content = myContentRevision.getContent();
            }
            catch (VcsException ex) {
                content = null;
            }
            if (content == null) {
                myHaveEmptyContentRevisions = true;
                return null;
            }
            return content;
        }
    }
}
