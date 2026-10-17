// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalService.impl.internal.errorReport;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.internal.MessagePool;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.localize.UILocalize;
import jakarta.inject.Inject;

@ExtensionImpl(id = "fatalErrorWidget", order = "after notificationsWidget")
public class FatalErrorWidgetFactory implements StatusBarWidgetFactory {
    private final DialogService myDialogService;

    @Inject
    public FatalErrorWidgetFactory(DialogService dialogService) {
        myDialogService = dialogService;
    }

    @Override
    public String getDisplayName() {
        return UILocalize.statusBarFatalErrorWidgetName().get();
    }

    @Override
    public boolean isAvailable(Project project) {
        return true;
    }

    @Override
    public StatusBarWidget createWidget(Project project) {
        return new IdeMessagePanel(this, WindowManager.getInstance().getIdeFrame(project), MessagePool.getInstance(), myDialogService);
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean canBeEnabledOn(StatusBar statusBar) {
        return false;
    }
}
