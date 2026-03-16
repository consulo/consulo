// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.localize.UILocalize;

@ExtensionImpl(id = "memoryIndicatorWidget", order = "last")
public class MemoryIndicatorWidgetFactory implements StatusBarWidgetFactory {
    
    @Override
    public String getDisplayName() {
        return UILocalize.statusBarMemoryUsageWidgetName().get();
    }

    @Override
    public boolean isAvailable(Project project) {
        return true;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    
    public StatusBarWidget createWidget(Project project) {
        return new MemoryUsagePanel(project, this);
    }

    @Override
    public boolean canBeEnabledOn(StatusBar statusBar) {
        return true;
    }
}
