// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.application.util.UserHomeFileUtil;
import consulo.diff.DiffFilePath;
import consulo.diff.combined.CombinedPathBlockId;
import consulo.diff.impl.internal.action.OpenInEditorAction;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.style.ComponentColors;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import org.jspecify.annotations.Nullable;

final class CombinedSimpleDiffHeader {
    private final Component myComponent;

    private @Nullable ActionToolbar myToolbar;

    @RequiredUIAccess
    CombinedSimpleDiffHeader(Project project, CombinedPathBlockId blockId, boolean withPathOnly, Runnable onClick) {
        myComponent = createTextComponent(project, blockId.getPath(), onClick);

        if (!withPathOnly) {
            buildToolbar();
        }
    }

    @RequiredUIAccess
    private ActionToolbar buildToolbar() {
        DefaultActionGroup toolbarGroup = new DefaultActionGroup();
        toolbarGroup.add(new OpenInEditorAction(null));

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("CombinedDiffBlockHeaderToolbar", toolbarGroup, true);
        myToolbar = toolbar;
        return toolbar;
    }

    Component getComponent() {
        return myComponent;
    }

    @Nullable Component getToolbarComponent() {
        ActionToolbar toolbar = myToolbar;
        return toolbar != null ? toolbar.getUIComponent() : null;
    }

    @RequiredUIAccess
    void setToolbarTargetComponent(Component component) {
        ActionToolbar toolbar = myToolbar;
        if (toolbar != null) {
            toolbar.setTargetUIComponent(component);
        }
    }

    @RequiredUIAccess
    private static Component createTextComponent(Project project, DiffFilePath path, Runnable onClick) {
        HorizontalLayout layout = HorizontalLayout.create();
        layout.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);

        Label nameLabel = Label.create(LocalizeValue.of(path.getName()));
        nameLabel.setImage(getIcon(project, path));
        nameLabel.addClickListener(event -> onClick.run());
        layout.add(nameLabel);

        DiffFilePath parentPath = path.getParentPath();
        if (parentPath != null) {
            String parentText = UserHomeFileUtil.getLocationRelativeToUserHome(parentPath.getPresentableUrl());
            Label pathLabel = Label.create(LocalizeValue.of(parentText != null ? parentText : parentPath.getPresentableUrl()));
            pathLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
            pathLabel.addClickListener(event -> onClick.run());
            layout.add(pathLabel);
        }
        return layout;
    }

    private static @Nullable Image getIcon(Project project, DiffFilePath path) {
        if (project.isDisposed()) {
            return null;
        }
        VirtualFile file = path.getVirtualFile();
        if (file != null) {
            return file.getFileType().getIcon();
        }
        return FileTypeRegistry.getInstance().getFileTypeByFileName(path.getName()).getIcon();
    }
}
