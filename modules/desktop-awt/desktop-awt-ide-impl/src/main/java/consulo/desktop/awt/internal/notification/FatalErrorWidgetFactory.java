// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.internal.MessagePool;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.localize.UILocalize;
import jakarta.annotation.Nonnull;

@ExtensionImpl(id = "fatalErrorWidget", order = "after notificationsWidget")
public class FatalErrorWidgetFactory implements StatusBarWidgetFactory {
    @Nonnull
    @Override
    public String getDisplayName() {
        return UILocalize.statusBarFatalErrorWidgetName().get();
    }

    @Override
    public boolean isAvailable(@Nonnull Project project) {
        return true;
    }

    @Override
    @Nonnull
    public StatusBarWidget createWidget(@Nonnull Project project) {
        return new IdeMessagePanel(project, this, WindowManager.getInstance().getIdeFrame(project), MessagePool.getInstance());
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
        return false;
    }
}
