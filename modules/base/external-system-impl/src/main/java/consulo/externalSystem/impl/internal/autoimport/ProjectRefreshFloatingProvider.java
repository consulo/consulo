// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.impl.internal.autoimport;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.coroutine.DisposableCoroutineScope;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.toolbar.floating.AbstractFloatingToolbarProvider;
import consulo.codeEditor.toolbar.floating.FloatingToolbarComponent;
import consulo.codeEditor.toolbar.floating.FloatingToolbarUtil;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.externalSystem.autoimport.ExternalSystemProjectNotificationAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectNotificationAwareListener;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import jakarta.inject.Inject;

@ExtensionImpl(id = "ExternalSystem.ProjectRefreshFloatingProvider")
public class ProjectRefreshFloatingProvider extends AbstractFloatingToolbarProvider {
    private final Project myProject;

    @Inject
    public ProjectRefreshFloatingProvider(Project project) {
        super(ProjectRefreshActionGroup.ID);
        myProject = project;
    }

    @Override
    public boolean isAutoHideable() {
        return false;
    }

    @Override
    public Coroutine<?, Boolean> isApplicableAsync(DataContext dataContext) {
        return Coroutine.first(UIAction.<Object, Boolean>apply(ignored -> {
            Editor editor = dataContext.getData(Editor.KEY);
            return FloatingToolbarUtil.isInsideMainEditor(dataContext)
                && editor != null
                && editor.getEditorKind() == EditorKind.MAIN_EDITOR;
        }));
    }

    private void updateToolbarComponent(FloatingToolbarComponent component, Disposable parentDisposable) {
        DisposableCoroutineScope.launchAsync(myProject.coroutineContext(), parentDisposable,
            () -> Coroutine.first(UIAction.<Object, Void>apply(ignored -> {
                ExternalSystemProjectNotificationAware notificationAware = ExternalSystemProjectNotificationAware.getInstance(myProject);
                if (notificationAware.isNotificationVisible()) {
                    component.scheduleShow();
                }
                else {
                    component.scheduleHide();
                }
                return null;
            })));
    }

    @Override
    @RequiredUIAccess
    public void register(DataContext dataContext, FloatingToolbarComponent component, Disposable parentDisposable) {
        myProject.getMessageBus().connect(parentDisposable)
            .subscribe(ExternalSystemProjectNotificationAwareListener.class, new ExternalSystemProjectNotificationAwareListener() {
                @Override
                public void onNotificationChanged() {
                    updateToolbarComponent(component, parentDisposable);
                }
            });

        updateToolbarComponent(component, parentDisposable);
    }
}
