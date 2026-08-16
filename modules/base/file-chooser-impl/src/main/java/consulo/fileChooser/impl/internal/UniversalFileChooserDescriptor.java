// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * The size of the browser panel when nothing is stored in the dimension service yet.
 */
class UniversalFileChooserDescriptor extends DialogDescriptor {
    private static final int DEFAULT_WIDTH = 700;
    private static final int DEFAULT_HEIGHT = 500;

    private final FileChooserDescriptor myDescriptor;
    private final Project myProject;
    private final List<UniversalFileChooserContributor> myContributors;
    private final @Nullable Path myPreselectPath;

    private @Nullable UniversalFileChooserPanel myPanel;
    private Runnable myOkAction = () -> {
    };

    UniversalFileChooserDescriptor(
        FileChooserDescriptor descriptor,
        Project project,
        List<UniversalFileChooserContributor> contributors,
        @Nullable Path preselectPath
    ) {
        super(descriptor.getTitle());
        myDescriptor = descriptor;
        myProject = project;
        myContributors = contributors;
        myPreselectPath = preselectPath;
    }

    @Override
    public @Nullable Size2D getInitialSize() {
        return new Size2D(DEFAULT_WIDTH, DEFAULT_HEIGHT);
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
            () -> myOkAction.run(),
            myContributors,
            myPreselectPath,
            this::updateOkButtonState,
            uiDisposable
        );
        myPanel = panel;
        return panel.getComponent();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myPanel != null && myPanel.isOkEnabled();
    }

    void setOkAction(Runnable okAction) {
        myOkAction = okAction;
    }

    List<Path> getSelectedFiles() {
        return myPanel == null ? List.of() : myPanel.getSelectedFiles();
    }
}
