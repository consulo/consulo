// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.distributed.ui;

import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.statusBar.EditorBasedWidget;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.branch.DvcsBranchUtil;
import consulo.versionControlSystem.distributed.branch.DvcsInOutCountersInProject;
import consulo.versionControlSystem.distributed.branch.DvcsIncomingOutgoingListener;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryMappingListener;
import consulo.versionControlSystem.icon.VersionControlSystemIconGroup;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class DvcsStatusWidget<T extends Repository> extends EditorBasedWidget
    implements StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget.Multiframe {
    protected static final Logger LOG = Logger.getInstance(DvcsStatusWidget.class);

    private final String myVcsName;

    private volatile @Nullable String myText;

    private volatile LocalizeValue myTooltip = LocalizeValue.empty();
    private volatile @Nullable Image myIcon;
    private volatile @Nullable Image mySecondaryIcon;
    private volatile @Nullable T myRepository;
    private @Nullable Future<?> myUpdateBackgroundFuture;

    protected DvcsStatusWidget(Project project, StatusBarWidgetFactory factory, String vcsName) {
        super(project, factory);
        myVcsName = vcsName;

        project.getMessageBus().connect(this).subscribe(VcsRepositoryMappingListener.class, () -> {
            LOG.debug("repository mappings changed");
            updateLater();
        });
        project.getMessageBus().connect(this).subscribe(DvcsIncomingOutgoingListener.class, this::updateLater);
    }

    /**
     * @see DvcsUtil#guessWidgetRepository
     */
    protected abstract @Nullable T guessCurrentRepository(Project project, @Nullable VirtualFile selectedFile);

    protected abstract String getFullBranchName(T repository);

    protected @Nullable Image getIcon(T repository) {
        if (repository.getState() != Repository.State.NORMAL) {
            return PlatformIconGroup.generalWarning();
        }
        return VersionControlSystemIconGroup.branch();
    }

    /**
     * @return incoming and outgoing commit counters of the current branch of the repository
     */
    protected DvcsInOutCountersInProject getInOutCounters(T repository) {
        return DvcsInOutCountersInProject.EMPTY;
    }

    protected abstract boolean isMultiRoot(Project project);

    protected abstract ListPopup getPopup(Project project, T repository);

    protected abstract void rememberRecentRoot(String path);

    @Override
    public void install(StatusBar statusBar) {
        super.install(statusBar);
        updateLater();
    }

    /**
     * @deprecated dvcs widgets are controlled by {@link consulo.ide.impl.idea.openapi.wm.impl.status.widget.StatusBarWidgetsManager}
     * and cannot be removed manually
     */
    @Deprecated
    public void deactivate() {
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void selectionChanged(FileEditorManagerEvent event) {
        LOG.debug("selection changed");
        updateLater();
    }

    @Override
    public void fileOpened(FileEditorManager source, VirtualFile file) {
        LOG.debug("file opened");
        updateLater();
    }

    @Override
    public void fileClosed(FileEditorManager source, VirtualFile file) {
        LOG.debug("file closed");
        updateLater();
    }

    @Override
    @RequiredUIAccess
    public @Nullable String getSelectedValue() {
        return StringUtil.defaultIfEmpty(myText, "");
    }

    @Override
    public LocalizeValue getTooltipText() {
        return myTooltip;
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public @Nullable Image getSecondaryIcon() {
        return mySecondaryIcon;
    }

    @Override
    @RequiredUIAccess
    public @Nullable ListPopup getPopupStep(DataContext dataContext) {
        if (isDisposed()) {
            return null;
        }

        T repository = myRepository;
        if (repository == null) {
            return null;
        }

        return getPopup(getProject(), repository);
    }

    private void clearStatus() {
        myText = null;
        myTooltip = LocalizeValue.empty();
        myIcon = null;
        mySecondaryIcon = null;
        myRepository = null;
    }

    protected void updateLater() {
        UIAccess uiAccess = getProject().getUIAccess();
        if (!uiAccess.isValid()) {
            return;
        }

        uiAccess.giveIfNeed(() -> {
            if (isDisposed()) {
                return;
            }

            VirtualFile selectedFile = DvcsUtil.getSelectedFile(getProject());
            Future<?> previous = myUpdateBackgroundFuture;
            if (previous != null) {
                previous.cancel(false);
            }
            myUpdateBackgroundFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule(
                () -> {
                    if (isDisposed()) {
                        clearStatus();
                        return;
                    }

                    if (!ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> updateOnBackground(selectedFile))) {
                        updateLater();
                    }
                },
                10,
                TimeUnit.MILLISECONDS
            );
        });
    }

    @Override
    public void dispose() {
        Future<?> future = myUpdateBackgroundFuture;
        if (future != null) {
            future.cancel(false);
        }
        super.dispose();
    }

    private void updateOnBackground(@Nullable VirtualFile selectedFile) {
        T repository;
        try {
            Project project = getProject();
            repository = guessCurrentRepository(project, selectedFile);
            if (repository == null) {
                clearStatus();
                return;
            }
        }
        catch (ProcessCanceledException e) {
            // do nothing - a new update task is scheduled, or widget is disposed
            return;
        }
        catch (Throwable t) {
            LOG.error(t);
            clearStatus();
            return;
        }

        myText = DvcsBranchUtil.shortenBranchName(getFullBranchName(repository));
        myTooltip = getToolTip(repository);
        myIcon = getIcon(repository);
        mySecondaryIcon = repository.getState() == Repository.State.NORMAL
            ? DvcsIncomingOutgoingUi.getInAndOutIcons(getInOutCounters(repository))
            : null;
        myRepository = repository;

        if (myStatusBar != null) {
            myStatusBar.updateWidget(getId());
        }

        rememberRecentRoot(repository.getRoot().getPath());
    }

    protected LocalizeValue getToolTip(@Nullable T repository) {
        if (repository == null) {
            return LocalizeValue.empty();
        }
        LocalizeValue message = DistributedVcsLocalize.tooltipBranchWidgetVcsBranchNameText(myVcsName, getFullBranchName(repository));
        if (isMultiRoot(repository.getProject())) {
            message = LocalizeValue.join(
                message,
                LocalizeValue.of("\n"),
                DistributedVcsLocalize.tooltipBranchWidgetRootNameText(repository.getRoot().getName())
            );
        }
        LocalizeValue inOutTooltip = getInOutCounters(repository).calcTooltip();
        if (inOutTooltip != LocalizeValue.empty()) {
            message = LocalizeValue.join(message, LocalizeValue.of("\n"), inOutTooltip);
        }
        return message;
    }
}