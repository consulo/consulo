// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.statusBar.StatusBarEditorBasedWidgetFactory;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.ex.localize.UILocalize;

@ExtensionImpl(id = "readOnlyWidget", order = "after codeStyleWidget")
public class ReadOnlyAttributeWidgetFactory extends StatusBarEditorBasedWidgetFactory {
    
    @Override
    public String getDisplayName() {
        return UILocalize.statusBarReadOnlyWidgetName().get();
    }

    @Override
    public
    
    StatusBarWidget createWidget(Project project) {
        return new ToggleReadOnlyAttributePanel(this);
    }
}
