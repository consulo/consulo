/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.localHistory.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.registry.Registry;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.localHistory.*;
import consulo.localHistory.impl.internal.ui.model.DirectoryHistoryDialogModel;
import consulo.localHistory.impl.internal.ui.model.EntireFileHistoryDialogModel;
import consulo.localHistory.impl.internal.ui.model.HistoryDialogModel;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.io.FileUtil;
import consulo.util.lang.ShutDownTracker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.BulkFileListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@ServiceImpl
public class LocalHistoryImpl extends LocalHistory implements Disposable {
    private final MessageBus myBus;
    private MessageBusConnection myConnection;
    private ChangeList myChangeList;
    private LocalHistoryFacade myVcs;
    private IdeaGateway myGateway;

    private LocalHistoryEventDispatcher myEventDispatcher;

    private final AtomicBoolean isInitialized = new AtomicBoolean();
    private Runnable myShutdownTask;

    public static LocalHistoryImpl getInstanceImpl() {
        return (LocalHistoryImpl)getInstance();
    }

    @Inject
    public LocalHistoryImpl(@Nonnull Application application) {
        myBus = application.getMessageBus();

        if (application.isUnitTestMode() || !application.isHeadlessEnvironment()) {
            initComponent();
        }
    }

    private void initComponent() {
        myShutdownTask = this::doDispose;
        ShutDownTracker.getInstance().registerShutdownTask(myShutdownTask);

        initHistory();
        isInitialized.set(true);
    }

    protected void initHistory() {
        ChangeListStorage storage;
        try {
            storage = new ChangeListStorageImpl(getStorageDir());
        }
        catch (Throwable e) {
            LocalHistoryLog.LOG.warn("cannot create storage, in-memory  implementation will be used", e);
            storage = new InMemoryChangeListStorage();
        }
        myChangeList = new ChangeList(storage);
        myVcs = new LocalHistoryFacade(myChangeList);

        myGateway = new IdeaGateway();

        myEventDispatcher = new LocalHistoryEventDispatcher(myVcs, myGateway);

        CommandProcessor.getInstance().addCommandListener(myEventDispatcher, this);

        myConnection = myBus.connect();
        myConnection.subscribe(BulkFileListener.class, myEventDispatcher);

        VirtualFileManager fm = VirtualFileManager.getInstance();
        fm.addVirtualFileManagerListener(myEventDispatcher, this);
    }

    public File getStorageDir() {
        return new File(getSystemPath(), "LocalHistory");
    }

    protected String getSystemPath() {
        return ContainerPathManager.get().getSystemPath();
    }

    @Override
    public void dispose() {
        doDispose();
    }

    private void doDispose() {
        if (!isInitialized.getAndSet(false)) {
            return;
        }

        long period = Registry.intValue("localHistory.daysToKeep") * 1000L * 60L * 60L * 24L;

        myConnection.disconnect();
        myConnection = null;

        LocalHistoryLog.LOG.debug("Purging local history...");
        myChangeList.purgeObsolete(period);
        myChangeList.close();
        LocalHistoryLog.LOG.debug("Local history storage successfully closed.");

        ShutDownTracker.getInstance().unregisterShutdownTask(myShutdownTask);
    }

    @Override
    @TestOnly
    public void cleanupForNextTest() {
        doDispose();
        FileUtil.delete(getStorageDir());
        initComponent();
    }

    @Override
    public LocalHistoryAction startAction(String name) {
        if (!isInitialized()) {
            return LocalHistoryAction.NULL;
        }

        LocalHistoryActionImpl a = new LocalHistoryActionImpl(myEventDispatcher, name);
        a.start();
        return a;
    }

    @Override
    public Label putUserLabel(Project p, @Nonnull String name) {
        if (!isInitialized()) {
            return Label.NULL_INSTANCE;
        }
        myGateway.registerUnsavedDocuments(myVcs);
        return label(myVcs.putUserLabel(name, getProjectId(p)));
    }

    private static String getProjectId(Project p) {
        return p.getLocationHash();
    }

    @Override
    public Label putSystemLabel(Project project, @Nonnull String name, int color) {
        if (!isInitialized()) {
            return Label.NULL_INSTANCE;
        }
        myGateway.registerUnsavedDocuments(myVcs);
        return label(myVcs.putSystemLabel(name, getProjectId(project), color));
    }

    public void addVFSListenerAfterLocalHistoryOne(BulkFileListener virtualFileListener, Disposable disposable) {
        myEventDispatcher.addVirtualFileListener(virtualFileListener, disposable);
    }

    private Label label(final LabelImpl impl) {
        return new Label() {
            @Override
            @RequiredUIAccess
            public void revert(@Nonnull Project project, @Nonnull VirtualFile file) throws LocalHistoryException {
                revertToLabel(project, file, impl);
            }

            @Override
            public ByteContent getByteContent(String path) {
                ThrowableComputable<ByteContent, RuntimeException> action =
                    () -> impl.getByteContent(myGateway.createTransientRootEntryForPathOnly(path), path);
                return AccessRule.read(action);
            }
        };
    }

    @Nullable
    @Override
    public byte[] getByteContent(VirtualFile f, FileRevisionTimestampComparator c) {
        if (!isInitialized()) {
            return null;
        }
        if (!myGateway.areContentChangesVersioned(f)) {
            return null;
        }
        ThrowableComputable<byte[], RuntimeException> action = () -> new ByteContentRetriever(myGateway, myVcs, f, c).getResult();
        return AccessRule.read(action);
    }

    @Override
    public boolean isUnderControl(VirtualFile f) {
        return isInitialized() && myGateway.isVersioned(f);
    }

    private boolean isInitialized() {
        return isInitialized.get();
    }

    @Nullable
    public LocalHistoryFacade getFacade() {
        return myVcs;
    }

    @Nullable
    public IdeaGateway getGateway() {
        return myGateway;
    }

    @RequiredUIAccess
    private void revertToLabel(@Nonnull Project project, @Nonnull VirtualFile f, @Nonnull LabelImpl impl) throws LocalHistoryException {
        HistoryDialogModel dirHistoryModel = f.isDirectory()
            ? new DirectoryHistoryDialogModel(project, myGateway, myVcs, f)
            : new EntireFileHistoryDialogModel(project, myGateway, myVcs, f);
        int leftRev = LocalHistoryUtil.findRevisionIndexToRevert(dirHistoryModel, impl);
        if (leftRev < 0) {
            throw new LocalHistoryException("Couldn't find label revision");
        }
        if (leftRev == 0) {
            return; // we shouldn't revert because no changes found to revert;
        }
        try {
            //-1 because we should revert all changes up to previous one, but not label-related.
            dirHistoryModel.selectRevisions(-1, leftRev - 1);
            dirHistoryModel.createReverter().revert();
        }
        catch (IOException e) {
            throw new LocalHistoryException(String.format("Couldn't revert %s to local history label.", f.getName()), e);
        }
    }
}