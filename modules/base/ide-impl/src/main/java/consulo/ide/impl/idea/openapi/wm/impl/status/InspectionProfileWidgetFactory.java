// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.ex.localize.UILocalize;
import jakarta.annotation.Nonnull;

// fully disabled
@Deprecated
public abstract class InspectionProfileWidgetFactory extends StatusBarEditorBasedWidgetFactory {
    @Override
    public boolean isAvailable(@Nonnull Project project) {
        return false; // Possibly add a Registry key
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return UILocalize.statusBarInspectionProfileWidgetName().get();
    }

    @Nonnull
    @Override
    public StatusBarWidget createWidget(@Nonnull Project project) {
        return new TogglePopupHintsPanel(project, this);
    }
}
