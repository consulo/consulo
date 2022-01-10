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
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.ErrorLabel;
import com.intellij.ui.popup.ActionPopupStep;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.PopupListElementRenderer;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
public abstract class RunAnythingChooseContextAction extends ActionGroup implements CustomComponentAction, DumbAware {
  public static List<RunAnythingContext> allContexts(Project project) {
    List<RunAnythingContext> contexts = projectAndModulesContexts(project);
    contexts.add(RunAnythingContext.BrowseRecentDirectoryContext.INSTANCE);

    List<String> paths = RunAnythingContextRecentDirectoryCache.getInstance(project).getState().paths;
    for (String path : paths) {
      contexts.add(new RunAnythingContext.RecentDirectoryContext(path));
    }
    return contexts;
  }

  private static List<RunAnythingContext> projectAndModulesContexts(Project project) {
    List<RunAnythingContext> contexts = new ArrayList<>();
    contexts.add(new RunAnythingContext.ProjectContext(project));
    ModuleManager manager = ModuleManager.getInstance(project);
    Module[] modules = manager.getModules();
    if(modules.length == 1) {
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
      e.getPresentation().setText(context.getLabel());
      e.getPresentation().setDescription(context.getDescription());
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
        Project project = e.getProject();

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        descriptor.setUseApplicationDialog();

        PathChooserDialog chooser = FileChooserFactory.getInstance().createPathChooser(descriptor, project, e.getDataContext().getData(PlatformDataKeys.CONTEXT_COMPONENT));

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
        protected void customizeComponent(JList<? extends PopupFactoryImpl.ActionItem> list, PopupFactoryImpl.ActionItem actionItem, boolean isSelected) {
          AnActionEvent event = ActionUtil.createEmptyEvent();
          ActionUtil.performDumbAwareUpdate(true, actionItem.getAction(), event, false);

          String description = event.getPresentation().getDescription();
          if (description != null) {
            myInfoLabel.setText(description);
          }

          myTextLabel.setText(event.getPresentation().getText());
          myInfoLabel.setForeground(isSelected ? UIUtil.getListSelectionForeground(true) : UIUtil.getInactiveTextColor());
        }
      };
    }
  }

  class ChooseContextPopupStep extends ActionPopupStep {
    private final List<PopupFactoryImpl.ActionItem> myActions;
    private final Runnable myUpdateToolbar;

    public ChooseContextPopupStep(List<PopupFactoryImpl.ActionItem> actions, DataContext dataContext, Runnable updateToolbar) {
      super(actions, IdeBundle.message("run.anything.context.title.working.directory"), () -> dataContext, null, true, Conditions.alwaysFalse(), false, true, null);
      myActions = actions;
      myUpdateToolbar = updateToolbar;
    }

    @Override
    public ListSeparator getSeparatorAbove(PopupFactoryImpl.ActionItem value) {
      AnAction action = value.getAction();
      if (action instanceof BrowseDirectoryItem) {
        return new ListSeparator(IdeBundle.message("run.anything.context.separator.directories"));
      }
      else if (action instanceof ModuleItem && action == ContainerUtil.filter(myActions, it -> it.getAction() instanceof ModuleItem).get(0).getAction()) {
        return new ListSeparator(IdeBundle.message("run.anything.context.separator.modules"));
      }
      return super.getSeparatorAbove(value);
    }

    @Override
    public Runnable getFinalRunnable() {
      try {
        return super.getFinalRunnable();
      }
      finally {
        myUpdateToolbar.run();
      }
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
  public boolean canBePerformed(DataContext context) {
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

  @Nonnull
  @Override
  public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
    presentation.setDescription(IdeBundle.message("run.anything.context.tooltip"));
    return new ActionButtonWithText(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    if (getAvailableContexts().isEmpty()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    if (getSelectedContext() != null && !getAvailableContexts().contains(getSelectedContext())) {
      setSelectedContext(null);
    }

    setSelectedContext(getSelectedContext() == null ? getAvailableContexts().get(0) : getSelectedContext());

    e.getPresentation().setEnabledAndVisible(true);
    e.getPresentation().setText(getSelectedContext().getLabel());
    e.getPresentation().setIcon(getSelectedContext().getIcon());

    containingPanel.revalidate();
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }

    JComponent component = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
    if (component == null) {
      return;
    }

    Runnable updateToolbar = () -> {
      ActionToolbar toolbar = UIUtil.uiParents(component, true).filter(ActionToolbar.class).first();
      toolbar.updateActionsImmediately();
    };

    DataContext dataContext = e.getDataContext();
    List<PopupFactoryImpl.ActionItem> actionItems = ActionPopupStep.createActionItems(new DefaultActionGroup(createItems()), dataContext, false, false, true, true, ActionPlaces.POPUP, null);

    ChooseContextPopup popup = new ChooseContextPopup(new ChooseContextPopupStep(actionItems, dataContext, updateToolbar), dataContext);
    popup.setSize(new Dimension(300, 300));
    popup.setRequestFocus(false);
    popup.showUnderneathOf(component);
  }

  private List<ContextItem> createItems() {
    return ContainerUtil.map(getAvailableContexts(), it -> {
      if (it instanceof RunAnythingContext.ProjectContext) {
        return new ProjectItem((RunAnythingContext.ProjectContext)it);
      }
      else if (it instanceof RunAnythingContext.ModuleContext) {
        return new ModuleItem((RunAnythingContext.ModuleContext)it);
      }
      else if (it instanceof RunAnythingContext.BrowseRecentDirectoryContext) {
        return new BrowseDirectoryItem((RunAnythingContext.BrowseRecentDirectoryContext)it);
      }
      else if (it instanceof RunAnythingContext.RecentDirectoryContext) {
        return new RecentDirectoryItem((RunAnythingContext.RecentDirectoryContext)it);
      }
      else {
        throw new UnsupportedOperationException();
      }
    });
  }
}
