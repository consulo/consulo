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
package consulo.execution.profiler.impl.internal.toolwindow;

import consulo.application.Application;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.attach.LocalAttachHost;
import consulo.execution.attach.XAttachHost;
import consulo.execution.attach.XAttachHostProvider;
import consulo.execution.attach.XAttachPresentationGroup;
import consulo.execution.profiler.ProfilerProcess;
import consulo.execution.profiler.configuration.ProfilerAttacher;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.execution.profiler.configuration.ProfilerConfigurationTypeBase;
import consulo.execution.profiler.configuration.ProfilerRunConfigurationManager;
import consulo.execution.profiler.impl.internal.session.ProfilerSession;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionListener;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionManager;
import consulo.execution.profiler.impl.internal.snapshot.ProfilerSnapshotService;
import consulo.execution.profiler.impl.internal.view.ProfilerUIUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.ProcessInfo;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.Table;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerHomePanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(ProfilerHomePanel.class);

    private static final Comparator<ProfilerAttachRow> ROW_ORDER = Comparator.comparingInt(ProfilerAttachRow::hostOrder)
        .thenComparingInt(row -> row.process().getPid());

    private final Project myProject;
    private final Application myApplication;
    private final ProfilerRunConfigurationManager myConfigurationsManager;
    private final ProfilerSnapshotService mySnapshotService;
    private final ProfilerSessionManager mySessionManager;
    private final ProfilerToolWindowManagerImpl myToolWindowManager;
    private final MutableFlatDataModel<ProfilerAttachRow> myProcessModel;
    private final Table<ProfilerAttachRow> myProcessTable;
    private final Button myRefreshButton;
    private final Label myProcessStatus;
    private final DockLayout myProcessArea;
    private final MutableFlatDataModel<String> myRecentModel;
    private final MutableFlatDataModel<ProfilerSession> mySessionModel;
    private final Table<ProfilerSession> mySessionTable;
    private final Button myCloseSessionButton;
    private final TwoComponentSplitLayout myRoot;
    private final AtomicInteger myLoadStamp = new AtomicInteger();

    private boolean myUpdatingSessions;
    private volatile boolean myDisposed;

    @RequiredUIAccess
    public ProfilerHomePanel(
        Project project,
        Application application,
        ProfilerRunConfigurationManager configurationsManager,
        ProfilerSnapshotService snapshotService,
        ProfilerSessionManager sessionManager,
        ProfilerToolWindowManagerImpl toolWindowManager
    ) {
        myProject = project;
        myApplication = application;
        myConfigurationsManager = configurationsManager;
        mySnapshotService = snapshotService;
        mySessionManager = sessionManager;
        myToolWindowManager = toolWindowManager;

        myProcessModel = FlatDataModel.of(List.of());
        myProcessTable = Table.create(myProcessModel);
        myProcessTable.addColumn(LocalizeValue.localizeTODO("PID"), row -> row.process().getPid())
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        myProcessTable.addColumn(LocalizeValue.localizeTODO("Process"), row -> row.process().getExecutableDisplayName())
            .setSortable(String.CASE_INSENSITIVE_ORDER);
        myProcessTable.addColumn(LocalizeValue.localizeTODO("Host"), ProfilerAttachRow::hostName)
            .setSortable(String.CASE_INSENSITIVE_ORDER);
        myProcessTable.addColumn(LocalizeValue.localizeTODO("Profilers"), ProfilerAttachRow::getProfilersText);
        myProcessTable.addColumn(LocalizeValue.localizeTODO("Command Line"), row -> row.process().getCommandLine());
        myProcessTable.setSpeedSearchConverter(row -> row.process().getExecutableDisplayName() + " " + row.process().getPid());
        myProcessTable.addSelectListener(event -> updateAttachBar(event.getValue()));
        myProcessTable.addDoubleClickListener(event -> {
            ProfilerAttachRow row = event.getValue();
            if (row != null && !row.options().isEmpty()) {
                attach(row, row.options().get(0));
            }
        });

        myRefreshButton = Button.create(LocalizeValue.localizeTODO("Refresh"), event -> reload());
        myRefreshButton.setIcon(PlatformIconGroup.actionsRefresh());

        myProcessStatus = Label.create();

        HorizontalLayout processHeader = HorizontalLayout.create(Space.SMALL);
        processHeader.add(myRefreshButton);
        processHeader.add(myProcessStatus);

        myProcessArea = DockLayout.create(Space.SMALL);
        myProcessArea.top(processHeader);
        myProcessArea.center(ScrollableLayout.create(myProcessTable));

        Button openSnapshotButton = Button.create(
            LocalizeValue.localizeTODO("Open Snapshot..."),
            event -> mySnapshotService.chooseAndOpen()
        );
        openSnapshotButton.setIcon(PlatformIconGroup.actionsMenu_open());

        myRecentModel = FlatDataModel.of(List.of());
        ListBox<String> recentList = ListBox.create(myRecentModel);
        recentList.setRender((presentation, item) -> {
            String path = item.getValue();
            if (path != null) {
                renderRecentSnapshot(path, presentation);
            }
        });
        recentList.addDoubleClickListener(event -> {
            String path = event.getValue();
            if (path != null) {
                openRecentSnapshot(path);
            }
        });

        HorizontalLayout snapshotHeader = HorizontalLayout.create(Space.SMALL);
        snapshotHeader.add(openSnapshotButton);
        snapshotHeader.add(Label.create(LocalizeValue.localizeTODO("Recent snapshots:")));

        DockLayout snapshotArea = DockLayout.create(Space.SMALL);
        snapshotArea.top(snapshotHeader);
        snapshotArea.center(ScrollableLayout.create(recentList));

        mySessionModel = FlatDataModel.of(List.of());
        mySessionTable = Table.create(mySessionModel);
        mySessionTable.addColumn(LocalizeValue.localizeTODO("Process"), ProfilerSession::getTargetName)
            .setSortable(String.CASE_INSENSITIVE_ORDER);
        mySessionTable.addColumn(LocalizeValue.localizeTODO("Configuration"), session -> session.getConfigurationName().get())
            .setSortable(String.CASE_INSENSITIVE_ORDER);
        mySessionTable.addColumn(LocalizeValue.localizeTODO("State"), session -> session.getStateText().get());
        mySessionTable.setSpeedSearchConverter(ProfilerSession::getTitle);
        mySessionTable.addSelectListener(event -> {
            ProfilerSession session = event.getValue();
            updateCloseSessionButton();
            if (session != null && !myUpdatingSessions) {
                myToolWindowManager.bringSessionToFront(session);
            }
        });
        mySessionTable.addDoubleClickListener(event -> {
            ProfilerSession session = event.getValue();
            if (session != null) {
                myToolWindowManager.showSession(session, true);
            }
        });

        myCloseSessionButton = Button.create(LocalizeValue.localizeTODO("Close Session"), event -> closeSelectedSession());
        myCloseSessionButton.setIcon(PlatformIconGroup.actionsClose());

        HorizontalLayout sessionHeader = HorizontalLayout.create(Space.SMALL);
        sessionHeader.add(Label.create(LocalizeValue.localizeTODO("Sessions:")));
        sessionHeader.add(myCloseSessionButton);

        DockLayout sessionArea = DockLayout.create(Space.SMALL);
        sessionArea.top(sessionHeader);
        sessionArea.center(ScrollableLayout.create(mySessionTable));

        TwoComponentSplitLayout sideArea = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        sideArea.setFirstComponent(sessionArea);
        sideArea.setSecondComponent(snapshotArea);
        sideArea.setProportion(50);

        myRoot = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        myRoot.setFirstComponent(myProcessArea);
        myRoot.setSecondComponent(sideArea);
        myRoot.setProportion(60);

        Disposer.register(this, configurationsManager.addChangeListener(() -> project.getUIAccess().give(() -> {
            if (!myDisposed && !myProject.isDisposed()) {
                reload();
            }
        })));
        Disposer.register(this, snapshotService.addRecentSnapshotsListener(this::refreshRecentSnapshots));
        Disposer.register(this, sessionManager.addListener(new ProfilerSessionListener() {
            @Override
            @RequiredUIAccess
            public void sessionAdded(ProfilerSession session) {
                updateSessions(() -> mySessionModel.add(session));
            }

            @Override
            @RequiredUIAccess
            public void sessionRemoved(ProfilerSession session) {
                if (mySessionModel.indexOf(session) >= 0) {
                    updateSessions(() -> mySessionModel.remove(session));
                }
            }

            @Override
            @RequiredUIAccess
            public void statusChanged(ProfilerSession session) {
                if (mySessionModel.indexOf(session) >= 0) {
                    updateSessions(() -> mySessionModel.update(session));
                }
            }
        }));

        updateAttachBar(null);
        refreshRecentSnapshots();
        updateSessions(() -> mySessionModel.replaceAll(sessionManager.getSessions()));
        reload();
    }

    public Component getComponent() {
        return myRoot;
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    @RequiredUIAccess
    public void refreshRecentSnapshots() {
        if (myDisposed) {
            return;
        }
        myRecentModel.replaceAll(mySnapshotService.getRecentSnapshots());
    }

    @RequiredUIAccess
    private void updateSessions(@RequiredUIAccess Runnable update) {
        if (myDisposed) {
            return;
        }

        myUpdatingSessions = true;
        try {
            update.run();
        }
        finally {
            myUpdatingSessions = false;
        }
        updateCloseSessionButton();
    }

    @RequiredUIAccess
    private void updateCloseSessionButton() {
        ProfilerSession session = mySessionTable.getSelectedItem();
        myCloseSessionButton.setEnabled(session != null && !session.isDisposed() && !session.isActive());
    }

    @RequiredUIAccess
    private void closeSelectedSession() {
        ProfilerSession session = mySessionTable.getSelectedItem();
        if (session != null) {
            mySessionManager.closeSession(session);
        }
    }

    @RequiredUIAccess
    public void reload() {
        int stamp = myLoadStamp.incrementAndGet();
        myRefreshButton.setEnabled(false);
        myProcessStatus.setText(LocalizeValue.localizeTODO("Looking for processes a profiler can attach to..."));

        List<ProfilerConfigurationState> configurations = List.copyOf(myConfigurationsManager.getConfigurations());

        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(CodeExecution.<Void, ProfilerAttachScan>apply(ignored -> scanSafely(configurations)))
                .then(UIAction.<ProfilerAttachScan, ProfilerAttachScan>apply(scan -> {
                    if (!myDisposed && !myProject.isDisposed() && stamp == myLoadStamp.get()) {
                        applyScan(scan);
                    }
                    return scan;
                }))
        );
    }

    @RequiredUIAccess
    private void applyScan(ProfilerAttachScan scan) {
        myRefreshButton.setEnabled(true);

        List<ProfilerAttachRow> rows = scan.rows();
        myProcessModel.replaceAll(rows);

        if (!scan.hasAttachers()) {
            myProcessStatus.setText(LocalizeValue.localizeTODO("No installed profiler can attach to a running process"));
        }
        else if (rows.isEmpty()) {
            myProcessStatus.setText(LocalizeValue.localizeTODO("No running process can be profiled"));
        }
        else {
            myProcessStatus.setText(LocalizeValue.localizeTODO(rows.size() + " processes can be profiled"));
        }

        updateAttachBar(myProcessTable.getSelectedItem());
    }

    @RequiredUIAccess
    private void updateAttachBar(@Nullable ProfilerAttachRow row) {
        HorizontalLayout attachBar = HorizontalLayout.create(Space.SMALL);
        if (row == null) {
            attachBar.add(ProfilerUIUtil.hint(LocalizeValue.localizeTODO("Select a process to attach a profiler to it")));
        }
        else {
            for (ProfilerAttachOption option : row.options()) {
                Button button = Button.create(
                    LocalizeValue.localizeTODO("Attach with '" + option.name().get() + "'"),
                    event -> attach(row, option)
                );
                button.setIcon(option.icon());
                attachBar.add(button);
            }
        }
        myProcessArea.bottom(attachBar);
    }

    @RequiredUIAccess
    private void attach(ProfilerAttachRow row, ProfilerAttachOption option) {
        int pid = row.process().getPid();
        LocalizeValue configurationName = option.name();
        myProcessStatus.setText(LocalizeValue.localizeTODO("Attaching '" + configurationName.get() + "' to process " + pid + "..."));

        CompletableFuture<ProfilerProcess<?>> future;
        try {
            future = option.attacher().attach(myProject, row.host(), row.process());
        }
        catch (Throwable e) {
            future = CompletableFuture.failedFuture(e);
        }

        future.whenComplete((process, error) -> {
            if (error == null) {
                myToolWindowManager.addProfilerProcessTab(process, true);
            }

            myProject.getUIAccess().give(() -> {
                if (myDisposed || myProject.isDisposed()) {
                    return;
                }

                if (error == null) {
                    myProcessStatus.setText(LocalizeValue.localizeTODO("Attached '" + configurationName.get() + "' to process " + pid));
                    return;
                }

                LOG.warn("Failed to attach profiler configuration '" + configurationName.get() + "' to process " + pid, error);
                LocalizeValue message = LocalizeValue.localizeTODO(
                    "Can't attach '" + configurationName.get() + "' to process " + pid + ": " + ProfilerUIUtil.describe(error)
                );
                myProcessStatus.setText(message);
                MessageBoxes.okError(message).title(LocalizeValue.localizeTODO("Attach Profiler")).showAsync();
            });
        });
    }

    @RequiredUIAccess
    private void openRecentSnapshot(String path) {
        Path file;
        try {
            file = Path.of(path);
        }
        catch (InvalidPathException e) {
            LOG.warn("Invalid recent profiler snapshot path " + path, e);
            return;
        }
        mySnapshotService.open(file);
    }

    private static void renderRecentSnapshot(String path, TextItemPresentation presentation) {
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (separator < 0) {
            presentation.append(path);
            return;
        }

        presentation.append(path.substring(separator + 1));
        presentation.append(" " + path.substring(0, separator), TextAttribute.GRAYED);
    }

    private ProfilerAttachScan scanSafely(List<ProfilerConfigurationState> configurations) {
        try {
            return scan(configurations);
        }
        catch (ProcessCanceledException e) {
            return ProfilerAttachScan.NO_ATTACHERS;
        }
        catch (Throwable e) {
            LOG.error("Failed to list the processes a profiler can attach to", e);
            return ProfilerAttachScan.NO_ATTACHERS;
        }
    }

    @SuppressWarnings("unchecked")
    private ProfilerAttachScan scan(List<ProfilerConfigurationState> configurations) {
        List<ProfilerAttachOption> attachers = createAttachers(configurations);
        if (attachers.isEmpty()) {
            return ProfilerAttachScan.NO_ATTACHERS;
        }

        Set<ProfilerAttacher> failedAttachers = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ProfilerAttachRow> rows = new ArrayList<>();
        collectRows(LocalAttachHost.INSTANCE, LocalizeValue.localizeTODO("Local").get(), 0, attachers, failedAttachers, rows);

        AtomicInteger hostOrder = new AtomicInteger(1);
        myApplication.getExtensionPoint(XAttachHostProvider.class).forEach(provider -> {
            List<? extends XAttachHost> hosts = provider.getAvailableHosts(myProject);
            for (XAttachHost host : hosts) {
                if (host == LocalAttachHost.INSTANCE) {
                    continue;
                }
                collectRows(host, getHostName(provider, host), hostOrder.getAndIncrement(), attachers, failedAttachers, rows);
            }
        });

        rows.sort(ROW_ORDER);
        return new ProfilerAttachScan(true, List.copyOf(rows));
    }

    private List<ProfilerAttachOption> createAttachers(List<ProfilerConfigurationState> configurations) {
        List<ProfilerAttachOption> attachers = new ArrayList<>();
        for (ProfilerConfigurationState configuration : configurations) {
            ProfilerConfigurationTypeBase<?> type = ProfilerConfigurationTypeBase.findById(configuration.getConfigurationTypeId());
            if (type == null) {
                continue;
            }

            try {
                ProfilerAttacher attacher = type.createAttacherFor(configuration);
                if (attacher != null) {
                    LocalizeValue name = ProfilerConfigurationPresentation.getName(configuration);
                    attachers.add(new ProfilerAttachOption(configuration, name, type.getIcon(), attacher));
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                LOG.error("Profiler configuration type " + type.getId() + " failed to create an attacher", e);
            }
        }
        return attachers;
    }

    private void collectRows(
        XAttachHost host,
        String hostName,
        int hostOrder,
        List<ProfilerAttachOption> attachers,
        Set<ProfilerAttacher> failedAttachers,
        List<ProfilerAttachRow> rows
    ) {
        Collection<ProcessInfo> processes;
        try {
            processes = host.getProcessList();
        }
        catch (ExecutionException e) {
            LOG.warn("Can't list the processes of attach host " + hostName, e);
            return;
        }

        for (ProcessInfo process : processes) {
            List<ProfilerAttachOption> options = new ArrayList<>();
            for (ProfilerAttachOption option : attachers) {
                ProfilerAttacher attacher = option.attacher();
                if (failedAttachers.contains(attacher)) {
                    continue;
                }

                try {
                    if (attacher.isApplicable(host, process)) {
                        options.add(option);
                    }
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    failedAttachers.add(attacher);
                    LOG.error("Profiler configuration '" + option.name().get() + "' failed to check process " + process.getPid(), e);
                }
            }

            if (!options.isEmpty()) {
                rows.add(new ProfilerAttachRow(host, hostName, hostOrder, process, List.copyOf(options)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getHostName(XAttachHostProvider<?> provider, XAttachHost host) {
        try {
            XAttachPresentationGroup<XAttachHost> group = (XAttachPresentationGroup<XAttachHost>) provider.getPresentationGroup();
            String text = group.getItemDisplayText(myProject, host, new UserDataHolderBase());
            return text.isBlank() ? group.getGroupName() : text;
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error("Attach host provider " + provider + " failed to name host " + host, e);
            return String.valueOf(host);
        }
    }
}
