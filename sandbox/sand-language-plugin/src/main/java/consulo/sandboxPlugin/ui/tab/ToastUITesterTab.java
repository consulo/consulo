/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.MoreActionGroup;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-16
 */
@ExtensionImpl(id = "toast", order = "after components")
public class ToastUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Toast");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();
        Label status = Label.create(LocalizeValue.of(" "));

        layout.add(HorizontalLayout.create()
            .add(Button.create(LocalizeValue.of("Notify Application"), event -> notifyAll(null, status)))
            .add(Button.create(LocalizeValue.of("Notify Project"), event -> {
                Project[] projects = ProjectManager.getInstance().getOpenProjects();
                notifyAll(projects.length == 0 ? null : projects[0], status);
            })));

        HorizontalLayout balloons = HorizontalLayout.create();
        for (NotificationType type : NotificationType.values()) {
            balloons.add(Button.create(LocalizeValue.of("Project Balloon " + type), event -> {
                Project project = firstProject();
                if (project != null) {
                    ToolWindowManager.getInstance(project).notifyByBalloon(
                        ToolWindowId.PROJECT_VIEW,
                        type,
                        "Project view says <b>" + type + "</b>.\nSecond line with a <a href=\"tw\">link</a>.",
                        null,
                        e -> status.setText(LocalizeValue.of("link: " + e.getDescription()))
                    );
                }
            }));
        }
        balloons.add(Button.create(LocalizeValue.of("Status Bar Balloon"), event -> {
            Project project = firstProject();
            StatusBar statusBar = project == null ? null : WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.notifyByBalloon(
                    StatusBar.PROGRESS_WIDGET_ID,
                    NotificationType.WARNING,
                    "Unable to rename certain usages. <a href=\"sb\">Browse</a>",
                    null,
                    e -> status.setText(LocalizeValue.of("link: " + e.getDescription()))
                );
            }
        }));
        layout.add(balloons);

        layout.add(toast(layout, status, ToastStyle.SUCCESS, null, "Build finished", "Compilation completed successfully in 4 s.",
            List.of(), List.of()));
        layout.add(toast(layout, status, ToastStyle.INFO, null, "Indexing",
            "Indexing project files. Some features are unavailable until it finishes.", List.of("Pause"), List.of()));
        layout.add(toast(layout, status, ToastStyle.WARNING, null, "Process control",
            "The IDE ignores SIGINT - the Stop button in run configurations may not work.", List.of("Learn more"),
            List.of("Do not show again", "Configure notifications...")));
        layout.add(toast(layout, status, null, ImageEffects.resize(PlatformIconGroup.generalQuestiondialog(), 16), "Reload project",
            "Build files changed on disk. Reload the project model?", List.of("Reload", "Ignore"), List.of()));
        layout.add(toast(layout, status, ToastStyle.ERROR, null, "Debugger", "Cannot connect to the debugger at localhost:5005.",
            List.of("Retry"), List.of("Copy details", "Show log")));
        layout.add(status);

        return ScrollableLayout.create(layout);
    }

    private static @Nullable Project firstProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        return projects.length == 0 ? null : projects[0];
    }

    private static void notifyAll(@Nullable Project project, Label status) {
        NotificationService service = Application.get().getInstance(NotificationService.class);
        String scope = project == null ? "application" : "project";
        service.newInfo(NotificationGroup.SYSTEM_MESSAGES)
            .title(LocalizeValue.of("Indexing (" + scope + ")"))
            .content(LocalizeValue.of("Indexing project files. Some features are unavailable until it finishes."))
            .addAction(LocalizeValue.of("Pause"), () -> status.setText(LocalizeValue.of("Pause")))
            .notify(project);
        service.newWarn(NotificationGroup.SYSTEM_MESSAGES)
            .title(LocalizeValue.of("Process control (" + scope + ")"))
            .content(LocalizeValue.of("The IDE ignores SIGINT - <a href=\"learn\">learn more</a>."))
            .hyperlinkListener((notification, event) -> status.setText(LocalizeValue.of("link: " + event.getDescription())))
            .notify(project);
        service.newError(NotificationGroup.SYSTEM_MESSAGES)
            .title(LocalizeValue.of("Debugger (" + scope + ")"))
            .content(LocalizeValue.of("Cannot connect to the debugger at localhost:5005."))
            .addClosingAction(LocalizeValue.of("Retry"), () -> status.setText(LocalizeValue.of("Retry")))
            .notify(project);
    }

    @RequiredUIAccess
    private static Toast toast(VerticalLayout layout,
                               Label status,
                               @Nullable ToastStyle style,
                               consulo.ui.image.@Nullable Image icon,
                               String title,
                               String text,
                               List<String> actions,
                               List<String> options) {
        Toast toast = Toast.create();
        if (style != null) {
            toast.addStyle(style);
        }
        toast.setIcon(icon);
        toast.setTitle(LocalizeValue.of(title));
        toast.setText(LocalizeValue.of(text));
        for (String action : actions) {
            toast.addAction(LocalizeValue.of(action), () -> status.setText(LocalizeValue.of(action)));
        }

        ActionGroup.Builder group = ActionGroup.newImmutableBuilder();
        if (!options.isEmpty()) {
            MoreActionGroup more = new MoreActionGroup(false);
            more.setPopup(true);
            for (String option : options) {
                more.add(DumbAwareAction.create(LocalizeValue.of(option), e -> status.setText(LocalizeValue.of(option))));
            }
            group.add(more);
        }
        group.add(new DumbAwareAction(LocalizeValue.of("Close"), LocalizeValue.empty(), PlatformIconGroup.actionsClose()) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                layout.remove(toast);
            }
        });

        ActionToolbar toolbar = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.TOOLBAR, group.build(), ActionToolbar.Style.INPLACE);
        toolbar.setTargetUIComponent(toast);
        toolbar.updateActionsAsync();
        toast.setToolbarComponent(toolbar.getUIComponent());
        return toast;
    }
}
