/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserFactory;
import consulo.fileChooser.PathChooserDialog;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.ui.popup.ActionPopupStep;
import consulo.ide.impl.idea.ui.popup.PopupFactoryImpl;
import consulo.ui.ex.awt.popup.PopupListElementRenderer;
import consulo.ide.localize.IdeLocalize;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.ErrorLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.ListSeparator;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.Conditions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
public abstract class RunAnythingChooseContextAction extends ActionGroup implements DumbAware {
    @RequiredReadAction
    public static List<RunAnythingContext> allContexts(Project project) {
        List<RunAnythingContext> contexts = projectAndModulesContexts(project);
        contexts.add(RunAnythingContext.BrowseRecentDirectoryContext.INSTANCE);

        List<String> paths = RunAnythingContextRecentDirectoryCache.getInstance(project).getState().paths;
        for (String path : paths) {
            contexts.add(new RunAnythingContext.RecentDirectoryContext(path));
        }
        return contexts;
    }

    @RequiredReadAction
    private static List<RunAnythingContext> projectAndModulesContexts(Project project) {
        List<RunAnythingContext> contexts = new ArrayList<>();
        contexts.add(new RunAnythingContext.ProjectContext(project));
        ModuleManager manager = ModuleManager.getInstance(project);
        Module[] modules = manager.getModules();
        if (modules.length == 1) {
            contexts.add(new RunAnythingContext.ModuleContext(modules[0]));
        }
        return contexts;
    }

    private abstract class ContextItem extends AnAction {
        protected RunAnythingContext context;

        private ContextItem(RunAnythingContext context) {
            this.context = context;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            setSelectedContext(this.context);
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setTextValue(context.getLabel());
            e.getPresentation().setDescriptionValue(context.getDescription());
            e.getPresentation().setIcon(context.getIcon());
        }
    }

    private class RecentDirectoryItem extends ContextItem {

        private RecentDirectoryItem(RunAnythingContext.RecentDirectoryContext context) {
            super(context);
        }
    }

    private class ProjectItem extends ContextItem {

        private ProjectItem(RunAnythingContext.ProjectContext context) {
            super(context);
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setIcon(Image.empty(16));
        }
    }

    private class ModuleItem extends ContextItem {

        private ModuleItem(RunAnythingContext.ModuleContext context) {
            super(context);
        }
    }

    private class BrowseDirectoryItem extends ContextItem {

        private BrowseDirectoryItem(RunAnythingContext.BrowseRecentDirectoryContext context) {
            super(context);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Application.get().invokeLater(() -> {
                Project project = e.getData(Project.KEY);

                FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                descriptor.setUseApplicationDialog();

                PathChooserDialog chooser = FileChooserFactory.getInstance().createPathChooser(
                    descriptor,
                    project,
                    e.getDataContext().getData(UIExAWTDataKey.CONTEXT_COMPONENT)
                );

                chooser.chooseAsync(project.getBaseDir()).doWhenDone(virtualFiles -> {
                    List<String> recentDirectories = RunAnythingContextRecentDirectoryCache.getInstance(project).getState().paths;

                    String path = ArrayUtil.getFirstElement(virtualFiles).getPath();

                    if (recentDirectories.size() >= Registry.intValue("run.anything.context.recent.directory.number")) {
                        recentDirectories.remove(0);
                    }

                    recentDirectories.add(path);

                    setSelectedContext(new RunAnythingContext.RecentDirectoryContext(path));
                });
            });
        }
    }

    class ChooseContextPopup extends PopupFactoryImpl.ActionGroupPopup {
        ChooseContextPopup(ActionPopupStep step, DataContext dataContext) {
            super(null, step, null, dataContext, ActionPlaces.POPUP, -1);
        }

        @Override
        protected ListCellRenderer getListElementRenderer() {
            return new PopupListElementRenderer<PopupFactoryImpl.ActionItem>(this) {
                private JLabel myInfoLabel;

                @Override
                protected JComponent createItemComponent() {
                    myTextLabel = new ErrorLabel();
                    myInfoLabel = new JLabel();
                    myTextLabel.setBorder(JBUI.Borders.empty(10));

                    JPanel textPanel = new JPanel(new BorderLayout());
                    textPanel.add(myTextLabel, BorderLayout.WEST);
                    textPanel.add(myInfoLabel, BorderLayout.CENTER);
                    return layoutComponent(textPanel);
                }

                @Override
                protected void customizeComponent(
                    JList<? extends PopupFactoryImpl.ActionItem> list,
                    PopupFactoryImpl.ActionItem actionItem,
                    boolean isSelected
                ) {
                    AnActionEvent event = ActionUtil.createEmptyEvent();
                    ActionUtil.performDumbAwareUpdate(actionItem.getAction(), event, false);

                    String description = event.getPresentation().getDescription();
                    if (description != null) {
                        myInfoLabel.setText(description);
                    }

                    myTextLabel.setText(event.getPresentation().getText());
                    myInfoLabel.setForeground(
                        isSelected ? UIUtil.getListSelectionForeground(true) : UIUtil.getInactiveTextColor()
                    );
                }
            };
        }
    }

    class ChooseContextPopupStep extends ActionPopupStep {
        private final List<PopupFactoryImpl.ActionItem> myActions;

        public ChooseContextPopupStep(
            List<PopupFactoryImpl.ActionItem> actions,
            DataContext dataContext
        ) {
            super(
                actions,
                IdeLocalize.runAnythingContextTitleWorkingDirectory().get(),
                () -> dataContext,
                null,
                true,
                Conditions.alwaysFalse(),
                false,
                true,
                null
            );
            myActions = actions;
        }

        @Override
        public ListSeparator getSeparatorAbove(PopupFactoryImpl.ActionItem value) {
            AnAction action = value.getAction();
            if (action instanceof BrowseDirectoryItem) {
                return new ListSeparator(IdeLocalize.runAnythingContextSeparatorDirectories().get());
            }
            else if (action instanceof ModuleItem
                && action == ContainerUtil.filter(myActions, it -> it.getAction() instanceof ModuleItem).get(0).getAction()) {
                return new ListSeparator(IdeLocalize.runAnythingContextSeparatorModules().get());
            }
            return super.getSeparatorAbove(value);
        }
    }

    private final JPanel containingPanel;

    public RunAnythingChooseContextAction(JPanel containingPanel) {
        this.containingPanel = containingPanel;
    }

    public abstract void setSelectedContext(@Nullable RunAnythingContext context);

    @Nullable
    public abstract RunAnythingContext getSelectedContext();

    @Nonnull
    public abstract List<RunAnythingContext> getAvailableContexts();

    public abstract void setAvailableContexts(List<? extends RunAnythingContext> contexts);

    @Override
    public boolean canBePerformed(@Nonnull DataContext context) {
        return true;
    }

    @Override
    public boolean isPopup() {
        return true;
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return AnAction.EMPTY_ARRAY;
    }

    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setDescriptionValue(IdeLocalize.runAnythingContextTooltip());

        if (getAvailableContexts().isEmpty()) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        if (getSelectedContext() != null && !getAvailableContexts().contains(getSelectedContext())) {
            setSelectedContext(null);
        }

        setSelectedContext(getSelectedContext() == null ? getAvailableContexts().get(0) : getSelectedContext());

        presentation.setEnabledAndVisible(true);
        presentation.setTextValue(getSelectedContext().getLabel());
        presentation.setIcon(getSelectedContext().getIcon());

        containingPanel.revalidate();
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        Component component = e.getInputEvent().getComponent();
        if (component == null) {
            return;
        }

        DataContext dataContext = e.getDataContext();
        List<PopupFactoryImpl.ActionItem> actionItems = ActionPopupStep.createActionItems(
            new DefaultActionGroup(createItems()),
            dataContext,
            false,
            false,
            true,
            true,
            ActionPlaces.POPUP,
            null
        );

        ChooseContextPopup popup = new ChooseContextPopup(new ChooseContextPopupStep(actionItems, dataContext), dataContext);
        popup.setSize(new Dimension(300, 300));
        popup.setRequestFocus(false);
        popup.showUnderneathOf(component);
    }

    private List<ContextItem> createItems() {
        return ContainerUtil.map(getAvailableContexts(), it -> {
            if (it instanceof RunAnythingContext.ProjectContext projectContext) {
                return new ProjectItem(projectContext);
            }
            else if (it instanceof RunAnythingContext.ModuleContext moduleContext) {
                return new ModuleItem(moduleContext);
            }
            else if (it instanceof RunAnythingContext.BrowseRecentDirectoryContext browseRecentDirectoryContext) {
                return new BrowseDirectoryItem(browseRecentDirectoryContext);
            }
            else if (it instanceof RunAnythingContext.RecentDirectoryContext recentDirectoryContext) {
                return new RecentDirectoryItem(recentDirectoryContext);
            }
            else {
                throw new UnsupportedOperationException();
            }
        });
    }
}
