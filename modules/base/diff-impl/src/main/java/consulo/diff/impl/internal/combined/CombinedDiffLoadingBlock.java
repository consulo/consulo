// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.FrameDiffTool;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

public class CombinedDiffLoadingBlock implements FrameDiffTool.DiffViewer {
    public static final int DEFAULT_LOADING_BLOCK_HEIGHT = 150;

    private final DockLayout myLoadingPanel;

    @RequiredUIAccess
    public CombinedDiffLoadingBlock(@Nullable Integer height) {
        Label label = Label.create(LocalizeValue.empty());
        label.setImage(Image.busy());
        myLoadingPanel = DockLayout.create();
        myLoadingPanel.center(label);
        myLoadingPanel.setHeight(height != null ? height : DEFAULT_LOADING_BLOCK_HEIGHT);
    }

    @Override
    public Component getComponent() {
        return myLoadingPanel;
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myLoadingPanel;
    }

    @RequiredUIAccess
    @Override
    public FrameDiffTool.ToolbarComponents init() {
        return new FrameDiffTool.ToolbarComponents();
    }

    @RequiredUIAccess
    @Override
    public void dispose() {
    }
}
