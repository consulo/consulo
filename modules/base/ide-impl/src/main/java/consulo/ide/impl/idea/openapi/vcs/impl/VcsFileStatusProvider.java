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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.ide.impl.idea.openapi.vcs.readOnlyHandler.ReadonlyStatusHandlerImpl;
import consulo.versionControlSystem.rollback.RollbackEnvironment;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import consulo.virtualFileSystem.status.FileStatusProvider;
import consulo.virtualFileSystem.status.impl.internal.FileStatusManagerImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class VcsFileStatusProvider implements FileStatusProvider, VcsBaseContentProvider {
    private final Project myProject;
    private final FileStatusManagerImpl myFileStatusManager;
    private final ProjectLevelVcsManager myVcsManager;
    private final ChangeListManager myChangeListManager;
    private final VcsDirtyScopeManager myDirtyScopeManager;
    private final VcsConfiguration myConfiguration;
    private boolean myHaveEmptyContentRevisions;

    private static final Logger LOG = Logger.getInstance(VcsFileStatusProvider.class);

    @Inject
    public VcsFileStatusProvider(
        final Project project,
        final FileStatusManager fileStatusManager,
        final ProjectLevelVcsManager vcsManager,
        ChangeListManager changeListManager,
        VcsDirtyScopeManager dirtyScopeManager, VcsConfiguration configuration
    ) {
        myProject = project;
        myFileStatusManager = (FileStatusManagerImpl)fileStatusManager;
        myVcsManager = vcsManager;
        myChangeListManager = changeListManager;
        myDirtyScopeManager = dirtyScopeManager;
        myConfiguration = configuration;
        myHaveEmptyContentRevisions = true;
        myFileStatusManager.setFileStatusProvider(this);

        changeListManager.addChangeListListener(new ChangeListAdapter() {
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

    private void fileStatusesChanged() {
        myFileStatusManager.fileStatusesChanged();
    }

    @Override
    @Nonnull
    public FileStatus getFileStatus(@Nonnull final VirtualFile virtualFile) {
        final AbstractVcs vcs = myVcsManager.getVcsFor(virtualFile);
        if (vcs == null) {
            if (ScratchUtil.isScratch(virtualFile)) {
                return FileStatus.SUPPRESSED;
            }
            return FileStatusManagerImpl.getDefaultStatus(virtualFile);
        }

        final FileStatus status = myChangeListManager.getStatus(virtualFile);
        if (status == FileStatus.NOT_CHANGED && isDocumentModified(virtualFile)) {
            return FileStatus.MODIFIED;
        }
        if (status == FileStatus.NOT_CHANGED) {
            return FileStatusManagerImpl.getDefaultStatus(virtualFile);
        }
        return status;
    }

    private static boolean isDocumentModified(VirtualFile virtualFile) {
        if (virtualFile.isDirectory()) {
            return false;
        }
        return FileDocumentManager.getInstance().isFileModified(virtualFile);
    }

    @Override
    public void refreshFileStatusFromDocument(@Nonnull final VirtualFile virtualFile, @Nonnull final Document doc) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("refreshFileStatusFromDocument: file.getModificationStamp()=" + virtualFile.getModificationStamp() + ", document.getModificationStamp()=" + doc.getModificationStamp());
        }
        FileStatus cachedStatus = myFileStatusManager.getCachedStatus(virtualFile);
        if (cachedStatus == null || cachedStatus == FileStatus.NOT_CHANGED || !isDocumentModified(virtualFile)) {
            final AbstractVcs vcs = myVcsManager.getVcsFor(virtualFile);
            if (vcs == null) {
                return;
            }
            if (cachedStatus == FileStatus.MODIFIED && !isDocumentModified(virtualFile)) {
                if (!((ReadonlyStatusHandlerImpl)ReadonlyStatusHandlerImpl.getInstance(myProject)).getState().SHOW_DIALOG) {
                    RollbackEnvironment rollbackEnvironment = vcs.getRollbackEnvironment();
                    if (rollbackEnvironment != null) {
                        rollbackEnvironment.rollbackIfUnchanged(virtualFile);
                    }
                }
            }
            myFileStatusManager.fileStatusChanged(virtualFile);
            ChangeProvider cp = vcs.getChangeProvider();
            if (cp != null && cp.isModifiedDocumentTrackingRequired()) {
                myDirtyScopeManager.fileDirty(virtualFile);
            }
        }
    }

    @Nonnull
    @Override
    public ThreeState getNotChangedDirectoryParentingStatus(@Nonnull VirtualFile virtualFile) {
        return myConfiguration.SHOW_DIRTY_RECURSIVELY ? myChangeListManager.haveChangesUnder(virtualFile) : ThreeState.NO;
    }

    @Override
    @Nullable
    public BaseContent getBaseRevision(@Nonnull final VirtualFile file) {
        if (!isHandledByVcs(file)) {
            VcsBaseContentProvider provider = findProviderFor(file);
            return provider == null ? null : provider.getBaseRevision(file);
        }
        final Change change = ChangeListManager.getInstance(myProject).getChange(file);
        if (change == null) {
            return null;
        }
        final ContentRevision beforeRevision = change.getBeforeRevision();
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
        for (VcsBaseContentProvider support : VcsBaseContentProvider.EP_NAME.getExtensionList(myProject)) {
            if (support.isSupported(file)) {
                return support;
            }
        }
        return null;
    }

    @Override
    public boolean isSupported(@Nonnull VirtualFile file) {
        return isHandledByVcs(file) || findProviderFor(file) != null;
    }

    private boolean isHandledByVcs(@Nonnull VirtualFile file) {
        return file.isInLocalFileSystem() && myVcsManager.getVcsFor(file) != null;
    }

    private class BaseContentImpl implements BaseContent {
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
