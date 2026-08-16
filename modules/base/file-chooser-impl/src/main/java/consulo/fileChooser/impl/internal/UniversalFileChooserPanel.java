// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.localize.FileChooserLocalize;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Alerts;
import consulo.ui.Component;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TabbedLayout;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class UniversalFileChooserPanel {
    private static final String TOOLBAR_PLACE = "UniversalFileChooserTopToolbar";

    private final FileChooserDescriptor myDescriptor;
    private final Project myProject;
    private final List<FileView> myFileViews = new ArrayList<>();
    private final DockLayout myComponent;

    private @Nullable TabbedLayout myTabbedLayout;

    UniversalFileChooserPanel(
        FileChooserDescriptor descriptor,
        Project project,
        Runnable okAction,
        List<UniversalFileChooserContributor> contributors,
        @Nullable Path preselectPath,
        Runnable okEnabledUpdater,
        Disposable disposable
    ) {
        myDescriptor = descriptor;
        myProject = project;

        for (UniversalFileChooserContributor contributor : contributors) {
            FileView fileView = new FileView(contributor, descriptor, project, okAction, okEnabledUpdater, false, disposable);
            fileView.setFileToSelect(preselectPath);
            myFileViews.add(fileView);
        }

        Component contentComponent;
        // If there is a single tab available, don't show the tab itself, only its content panel.
        if (myFileViews.size() == 1) {
            contentComponent = myFileViews.get(0).getTopComponent();
        }
        else {
            TabbedLayout tabbedLayout = TabbedLayout.create();
            for (FileView fileView : myFileViews) {
                tabbedLayout.addTab(fileView.getContributor().getTabTitle().get(), fileView.getTopComponent());
            }
            myTabbedLayout = tabbedLayout;
            contentComponent = tabbedLayout;
        }

        ActionToolbar topToolbar = createTopToolbar();

        myComponent = DockLayout.create();
        myComponent.top(topToolbar.getUIComponent());
        myComponent.center(contentComponent);

        topToolbar.setTargetUIComponent(myComponent);

        for (FileView fileView : myFileViews) {
            fileView.loadRoots();
        }
    }

    Component getComponent() {
        return myComponent;
    }

    List<Path> getSelectedFiles() {
        FileView activeFileView = getActiveFileView();
        return activeFileView == null ? List.of() : activeFileView.getSelectedFiles();
    }

    boolean isOkEnabled() {
        FileView activeFileView = getActiveFileView();
        return activeFileView != null && activeFileView.isOkEnabled();
    }

    private @Nullable FileView getActiveFileView() {
        if (myFileViews.isEmpty()) {
            return null;
        }
        return myFileViews.get(0);
    }

    private class DesktopAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        DesktopAction() {
            super(
                FileChooserLocalize.universalFileChooserActionDesktopText(),
                FileChooserLocalize.universalFileChooserActionDesktopDescription(),
                PlatformIconGroup.nodesDesktop()
            );
        }

        @Override
        public void update(AnActionEvent e) {
            boolean anyDesktop = false;
            for (FileView fileView : myFileViews) {
                if (fileView.getContributor().getDesktopPath() != null) {
                    anyDesktop = true;
                    break;
                }
            }
            e.getPresentation().setVisible(anyDesktop);
            e.getPresentation().setEnabled(true);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            navigateToDesktop();
        }
    }

    private class CreateDirectoryAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        CreateDirectoryAction() {
            super(
                FileChooserLocalize.universalFileChooserActionCreateDirectoryText(),
                FileChooserLocalize.universalFileChooserActionCreateDirectoryDescription(),
                PlatformIconGroup.actionsNewfolder()
            );
        }

        @Override
        public void update(AnActionEvent e) {
            FileView activeFileView = getActiveFileView();
            if (activeFileView == null) {
                e.getPresentation().setEnabled(false);
                return;
            }
            Path parent = activeFileView.getNewFileParent();
            e.getPresentation().setEnabled(
                parent != null && parent.getParent() != null && Files.isDirectory(parent) && Files.isWritable(parent));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            createNewFolder();
        }
    }

    private class DeleteAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        DeleteAction() {
            super(
                FileChooserLocalize.universalFileChooserActionDeleteText(),
                FileChooserLocalize.universalFileChooserActionDeleteDescription(),
                PlatformIconGroup.generalRemove()
            );
        }

        @Override
        public void update(AnActionEvent e) {
            FileView activeFileView = getActiveFileView();
            e.getPresentation().setEnabled(activeFileView != null && activeFileView.canDeleteSelectedFile());
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            deleteSelectedFile();
        }
    }

    private ActionToolbar createTopToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();

        group.add(new DumbAwareAction(
            FileChooserLocalize.universalFileChooserActionHomeText(),
            FileChooserLocalize.universalFileChooserActionHomeDescription(),
            PlatformIconGroup.nodesHomefolder()
        ) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                navigateToHome();
            }
        });

        group.add(new DesktopAction());

        if (!myProject.isDefault()) {
            group.add(new DumbAwareAction(
                FileChooserLocalize.universalFileChooserActionProjectText(),
                FileChooserLocalize.universalFileChooserActionProjectDescription(),
                PlatformIconGroup.nodesProject()
            ) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    navigateToProject();
                }
            });
        }

        group.addSeparator();

        group.add(new CreateDirectoryAction());
        group.add(new DeleteAction());

        group.addSeparator();

        group.add(new DumbAwareAction(
            FileChooserLocalize.universalFileChooserActionRefreshText(),
            FileChooserLocalize.universalFileChooserActionRefreshDescription(),
            PlatformIconGroup.actionsRefresh()
        ) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                FileView activeFileView = getActiveFileView();
                if (activeFileView != null) {
                    activeFileView.getFileTree().updateTree();
                }
            }
        });

        group.add(new DumbAwareToggleAction(
            FileChooserLocalize.universalFileChooserActionHiddenText(),
            FileChooserLocalize.universalFileChooserActionHiddenDescription(),
            PlatformIconGroup.actionsTogglevisibility()
        ) {
            @Override
            public boolean isSelected(AnActionEvent e) {
                FileView activeFileView = getActiveFileView();
                return activeFileView != null && activeFileView.getFileTree().areHiddensShown();
            }

            @Override
            public void setSelected(AnActionEvent e, boolean state) {
                for (FileView fileView : myFileViews) {
                    fileView.getFileTree().showHiddens(state);
                    fileView.getFileTree().updateTree();
                }
            }
        });

        return ActionToolbarFactory.getInstance()
            .createActionToolbar(TOOLBAR_PLACE, group, ActionToolbar.Style.HORIZONTAL);
    }

    private void navigateToHome() {
        FileView activeFileView = getActiveFileView();
        if (activeFileView != null) {
            activeFileView.navigateToFile(Platform.current().user().homePath());
        }
    }

    private void navigateToProject() {
        String basePath = myProject.getBasePath();
        FileView activeFileView = getActiveFileView();
        if (basePath != null && activeFileView != null) {
            activeFileView.navigateToFile(Path.of(basePath));
        }
    }

    private void navigateToDesktop() {
        FileView activeFileView = getActiveFileView();
        FileView targetView = activeFileView != null && activeFileView.getContributor().getDesktopPath() != null
            ? activeFileView
            : null;
        if (targetView == null) {
            for (FileView fileView : myFileViews) {
                if (fileView.getContributor().getDesktopPath() != null) {
                    targetView = fileView;
                    break;
                }
            }
        }
        if (targetView == null) {
            return;
        }
        Path desktopPath = targetView.getContributor().getDesktopPath();
        if (desktopPath != null) {
            targetView.navigateToFile(desktopPath);
        }
    }

    private void createNewFolder() {
        FileView activeFileView = getActiveFileView();
        if (activeFileView == null) {
            return;
        }
        Path parent = activeFileView.getNewFileParent();
        if (parent == null) {
            return;
        }
        NewFolderNameDescriptor nameDescriptor = new NewFolderNameDescriptor(
            FileChooserLocalize.newFolderDialogTitle(),
            FileChooserLocalize.createNewFolderEnterNewFolderNamePromptText()
        );

        Dialog nameDialog = Application.get().getInstance(DialogService.class).build(nameDescriptor);
        nameDialog.showAsync().whenComplete((value, throwable) -> {
            if (throwable != null || value == null) {
                return;
            }
            String newFolderName = nameDescriptor.getFolderName();
            if (newFolderName.isBlank()) {
                return;
            }
            Exception failReason = activeFileView.getFileTree().createNewFolder(parent, newFolderName);
            if (failReason != null) {
                Alerts.okError(FileChooserLocalize.createNewFolderCouldNotCreateFolderErrorMessage(newFolderName)).showAsync();
            }
        });
    }

    private void deleteSelectedFile() {
        FileView activeFileView = getActiveFileView();
        if (activeFileView != null) {
            activeFileView.deleteSelectedFile();
        }
    }
}
