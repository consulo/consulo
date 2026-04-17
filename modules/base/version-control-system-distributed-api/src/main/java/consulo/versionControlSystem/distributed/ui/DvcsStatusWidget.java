// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.distributed.ui;

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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.branch.DvcsBranchUtil;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryMappingListener;
import consulo.versionControlSystem.icon.VersionControlSystemIconGroup;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public abstract class DvcsStatusWidget<T extends Repository> extends EditorBasedWidget
    implements StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget.Multiframe {
    protected static final Logger LOG = Logger.getInstance(DvcsStatusWidget.class);

    private final String myVcsName;

    private @Nullable String myText;
    
    private LocalizeValue myTooltip = LocalizeValue.empty();
    private @Nullable Image myIcon;

    protected DvcsStatusWidget(Project project, StatusBarWidgetFactory factory, String vcsName) {
        super(project, factory);
        myVcsName = vcsName;

        project.getMessageBus().connect(this).subscribe(VcsRepositoryMappingListener.class, () -> {
            LOG.debug("repository mappings changed");
            updateLater();
        });
    }

    protected abstract @Nullable T guessCurrentRepository(Project project);

    protected abstract String getFullBranchName(T repository);

    protected @Nullable Image getIcon(T repository) {
        if (repository.getState() != Repository.State.NORMAL) {
            return PlatformIconGroup.generalWarning();
        }
        return VersionControlSystemIconGroup.branch();
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
        update();
    }

    @Override
    public void fileOpened(FileEditorManager source, VirtualFile file) {
        LOG.debug("file opened");
        update();
    }

    @Override
    public void fileClosed(FileEditorManager source, VirtualFile file) {
        LOG.debug("file closed");
        update();
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
    public @Nullable ListPopup getPopupStep() {
        if (isDisposed()) {
            return null;
        }
        Project project = getProject();
        T repository = guessCurrentRepository(project);
        if (repository == null) {
            return null;
        }

        return getPopup(project, repository);
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        // has no effect since the click opens a list popup, and the consumer is not called for the MultipleTextValuesPresentation
        return null;
    }

    protected void updateLater() {
        Project project = getProject();
        if (isDisposed()) {
            return;
        }
        project.getApplication().invokeLater(
            () -> {
                LOG.debug("update after repository change");
                update();
            },
            project.getDisposed()
        );
    }

    private void update() {
        myText = null;
        myTooltip = LocalizeValue.empty();
        myIcon = null;

        if (isDisposed()) {
            return;
        }
        Project project = getProject();
        T repository = guessCurrentRepository(project);
        if (repository == null) {
            return;
        }
        myText = DvcsBranchUtil.shortenBranchName(getFullBranchName(repository));
        myTooltip = getToolTip(repository);
        myIcon = getIcon(repository);
        if (myStatusBar != null) {
            myStatusBar.updateWidget(getId());
        }
        rememberRecentRoot(repository.getRoot().getPath());
    }

    private LocalizeValue getToolTip(@Nullable T repository) {
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
        return message;
    }
}