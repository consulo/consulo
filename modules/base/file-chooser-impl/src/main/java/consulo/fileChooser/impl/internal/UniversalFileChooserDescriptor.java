// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.UniversalFileChooserContributor;
import consulo.fileChooser.util.FileChooserUtil;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.WidthAndHeight;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * The size of the browser panel when nothing is stored in the dimension service yet.
 */
class UniversalFileChooserDescriptor extends DialogDescriptor {
    private static final float DEFAULT_WIDTH = 35;
    private static final float DEFAULT_HEIGHT = 25;

    private final FileChooserDescriptor myDescriptor;
    private final Project myProject;
    private final List<UniversalFileChooserContributor> myContributors;
    private final @Nullable Path myPreselectPath;
    private final ApplicationTreeExecutorFactory myTreeExecutorFactory;

    private @Nullable UniversalFileChooserPanel myPanel;
    private @Nullable Dialog myDialog;
    private boolean myValidating;

    UniversalFileChooserDescriptor(
        FileChooserDescriptor descriptor,
        Project project,
        List<UniversalFileChooserContributor> contributors,
        @Nullable Path preselectPath,
        ApplicationTreeExecutorFactory treeExecutorFactory
    ) {
        super(descriptor.getTitle());
        myDescriptor = descriptor;
        myProject = project;
        myContributors = contributors;
        myPreselectPath = preselectPath;
        myTreeExecutorFactory = treeExecutorFactory;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "UniversalFileChooserDialog";
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        UniversalFileChooserPanel panel = new UniversalFileChooserPanel(
            myDescriptor,
            myProject,
            this::validateAndClose,
            myContributors,
            myPreselectPath,
            this::updateOkButtonState,
            myTreeExecutorFactory,
            uiDisposable
        );
        myPanel = panel;
        return panel.getComponent();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myPanel != null && myPanel.isOkEnabled();
    }

    void setDialog(Dialog dialog) {
        myDialog = dialog;
    }

    /**
     * The validation is async - the dialog is closed by {@link #validateAndClose()} once the chosen paths are accepted.
     */
    @RequiredUIAccess
    @Override
    public boolean canHandle(AnAction action, @Nullable DialogValue value, Window window) {
        if (value == null) {
            return true;
        }
        validateAndClose();
        return false;
    }

    @RequiredUIAccess
    private void validateAndClose() {
        Dialog dialog = myDialog;
        if (dialog == null || myValidating) {
            return;
        }

        myValidating = true;
        FileChooserUtil.validateSelectedPaths(myDescriptor, getSelectedFiles()).whenComplete((message, error) -> {
            myValidating = false;
            if (error != null) {
                return;
            }

            if (message.isNotEmpty()) {
                MessageBoxes.okError(message)
                    .title(getTitle())
                    .showAsync(dialog.getWindow());
                return;
            }

            dialog.doOkAction(getOkValue());
        });
    }

    List<Path> getSelectedFiles() {
        return myPanel == null ? List.of() : myPanel.getSelectedFiles();
    }
}
