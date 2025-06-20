// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.ex.UIBundle;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

@ExtensionImpl(id = "lineSeparatorWidget", order = "after positionWidget")
public class LineSeparatorWidgetFactory extends StatusBarEditorBasedWidgetFactory {
    @Nls
    @Nonnull
    @Override
    public String getDisplayName() {
        return UIBundle.message("status.bar.line.separator.widget.name");
    }

    @Override
    @Nonnull
    public StatusBarWidget createWidget(@Nonnull Project project) {
        return new LineSeparatorPanel(project, this);
    }
}
