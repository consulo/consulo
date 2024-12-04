// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.impl.internal.action;

import consulo.application.AllIcons;
import consulo.dataContext.DataContext;
import consulo.execution.ExecutionManager;
import consulo.execution.configuration.RunProfile;
import consulo.execution.icon.ExecutionIconGroup;
import consulo.execution.impl.internal.ExecutionManagerImpl;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.RunContentDescriptor;
import consulo.localize.LocalizeValue;
import consulo.process.KillableProcessHandler;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.popup.GroupedItemsListRenderer;
import consulo.ui.ex.awt.popup.ListItemDescriptorAdapter;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StopAction extends DumbAwareAction implements AnAction.TransparentUpdate {
  private WeakReference<JBPopup> myActivePopupRef = null;

  private static boolean isPlaceGlobal(@Nonnull AnActionEvent e) {
    return ActionPlaces.isMainMenuOrActionSearch(e.getPlace()) ||
      ActionPlaces.MAIN_TOOLBAR.equals(e.getPlace()) ||
      ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(e.getPlace()) ||
      ActionPlaces.TOUCHBAR_GENERAL.equals(e.getPlace());
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull final AnActionEvent e) {
    boolean enable = false;
    Image icon = getTemplatePresentation().getIcon();
    LocalizeValue description = getTemplatePresentation().getDescriptionValue();
    Presentation presentation = e.getPresentation();
    if (isPlaceGlobal(e)) {
      List<RunContentDescriptor> stoppableDescriptors = getActiveStoppableDescriptors(e.getDataContext());
      int stopCount = stoppableDescriptors.size();
      enable = stopCount >= 1;
      if (stopCount > 1) {
        presentation.setText(getTemplatePresentation().getText() + "...");
        icon = ImageEffects.withText(icon, String.valueOf(stopCount));
      }
      else if (stopCount == 1) {
        presentation.setTextValue(ExecutionLocalize.stopConfigurationActionName(
          StringUtil.escapeMnemonics(StringUtil.notNullize(stoppableDescriptors.get(0).getDisplayName()))
        ));
      }
    }
    else {
      RunContentDescriptor contentDescriptor = e.getData(RunContentDescriptor.KEY);
      ProcessHandler processHandler = contentDescriptor == null ? null : contentDescriptor.getProcessHandler();
      if (processHandler != null && !processHandler.isProcessTerminated()) {
        if (!processHandler.isProcessTerminating()) {
          enable = true;
        }
        else if (processHandler instanceof KillableProcessHandler && ((KillableProcessHandler)processHandler).canKillProcess()) {
          enable = true;
          icon = ExecutionIconGroup.actionKillprocess();
          description = ExecutionLocalize.actionTerminatingProcessProgressKillDescription();
        }
      }

      RunProfile runProfile = e.getData(RunProfile.KEY);
      if (runProfile == null && contentDescriptor == null) {
        presentation.setTextValue(getTemplatePresentation().getTextValue());
      }
      else {
        presentation.setTextValue(ExecutionLocalize.stopConfigurationActionName(
          StringUtil.escapeMnemonics(runProfile == null ? StringUtil.notNullize(contentDescriptor.getDisplayName()) : runProfile.getName())
        ));
      }
    }

    presentation.setEnabled(enable);
    presentation.setIcon(icon);
    presentation.setDescriptionValue(description);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getData(Project.KEY);
    List<RunContentDescriptor> stoppableDescriptors = getActiveStoppableDescriptors(dataContext);
    int stopCount = stoppableDescriptors.size();
    if (isPlaceGlobal(e)) {
      if (stopCount == 1) {
        ExecutionManagerImpl.stopProcess(stoppableDescriptors.get(0));
        return;
      }

      Pair<List<HandlerItem>, HandlerItem> handlerItems =
        getItemsList(stoppableDescriptors, getRecentlyStartedContentDescriptor(dataContext));
      if (handlerItems == null || handlerItems.first.isEmpty()) {
        return;
      }

      HandlerItem stopAllItem = new HandlerItem(
        ExecutionLocalize.stopAll(KeymapUtil.getFirstKeyboardShortcutText("Stop")).get(),
        AllIcons.Actions.Suspend,
        true
      ) {
        @Override
        void stop() {
          for (HandlerItem item : handlerItems.first) {
            if (item == this) continue;
            item.stop();
          }
        }
      };
      JBPopup activePopup = SoftReference.dereference(myActivePopupRef);
      if (activePopup != null) {
        stopAllItem.stop();
        activePopup.cancel();
        return;
      }

      List<HandlerItem> items = handlerItems.first;
      if (stopCount > 1) {
        items.add(stopAllItem);
      }

      IPopupChooserBuilder<HandlerItem> builder =
        JBPopupFactory.getInstance()
                      .createPopupChooserBuilder(items)
                      .setRenderer(new GroupedItemsListRenderer<>(new ListItemDescriptorAdapter<HandlerItem>() {
                        @Nullable
                        @Override
                        public String getTextFor(HandlerItem item) {
                          return item.displayName;
                        }

                        @Nullable
                        @Override
                        public Image getIconFor(HandlerItem item) {
                          return item.icon;
                        }

                        @Override
                        public boolean hasSeparatorAboveOf(HandlerItem item) {
                          return item.hasSeparator;
                        }
                      }))
                      .setMovable(true)
                      .setTitle(items.size() == 1 ? ExecutionLocalize.confirmProcessStop().get() : ExecutionLocalize.stopProcess().get())
                      .setNamerForFiltering(o -> o.displayName)
                      .setItemsChosenCallback((valuesList) -> {
                        for (HandlerItem item : valuesList) {
                          item.stop();
                        }
                      })
                      .addListener(new JBPopupListener() {
                        @Override
                        public void onClosed(@Nonnull LightweightWindowEvent event) {
                          myActivePopupRef = null;
                        }
                      })
                      .setRequestFocus(true);
      if (handlerItems.second != null) {
        builder.setSelectedValue(handlerItems.second, true);
      }
      JBPopup popup = builder.createPopup();

      myActivePopupRef = new WeakReference<>(popup);
      InputEvent inputEvent = e.getInputEvent();
      Component component = inputEvent != null ? inputEvent.getComponent() : null;
      if (component != null && (ActionPlaces.MAIN_TOOLBAR.equals(e.getPlace()) || ActionPlaces.NAVIGATION_BAR_TOOLBAR.equals(e.getPlace()))) {
        popup.showUnderneathOf(component);
      }
      else if (project == null) {
        popup.showInBestPositionFor(dataContext);
      }
      else {
        popup.showCenteredInCurrentWindow(project);
      }
    }
    else {
      ExecutionManagerImpl.stopProcess(getRecentlyStartedContentDescriptor(dataContext));
    }
  }

  @Nullable
  private static Pair<List<HandlerItem>, HandlerItem> getItemsList(List<? extends RunContentDescriptor> descriptors,
                                                                   RunContentDescriptor toSelect) {
    if (descriptors.isEmpty()) {
      return null;
    }

    List<HandlerItem> items = new ArrayList<>(descriptors.size());
    HandlerItem selected = null;
    for (final RunContentDescriptor descriptor : descriptors) {
      final ProcessHandler handler = descriptor.getProcessHandler();
      if (handler != null) {
        HandlerItem item = new HandlerItem(descriptor.getDisplayName(), descriptor.getIcon(), false) {
          @Override
          void stop() {
            ExecutionManagerImpl.stopProcess(descriptor);
          }
        };
        items.add(item);
        if (descriptor == toSelect) {
          selected = item;
        }
      }
    }

    return Pair.create(items, selected);
  }

  @Nullable
  public static RunContentDescriptor getRecentlyStartedContentDescriptor(@Nonnull DataContext dataContext) {
    final RunContentDescriptor contentDescriptor = dataContext.getData(RunContentDescriptor.KEY);
    if (contentDescriptor != null) {
      // toolwindow case
      return contentDescriptor;
    }
    else {
      // main menu toolbar
      final Project project = dataContext.getData(Project.KEY);
      return project == null ? null : ExecutionManager.getInstance(project).getContentManager().getSelectedContent();
    }
  }

  @Nonnull
  private static List<RunContentDescriptor> getActiveStoppableDescriptors(@Nonnull DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    List<RunContentDescriptor> runningProcesses =
      project == null ? Collections.emptyList() : ExecutionManager.getInstance(project).getContentManager().getAllDescriptors();
    if (runningProcesses.isEmpty()) {
      return Collections.emptyList();
    }

    List<RunContentDescriptor> activeDescriptors = new SmartList<>();
    for (RunContentDescriptor descriptor : runningProcesses) {
      if (canBeStopped(descriptor)) {
        activeDescriptors.add(descriptor);
      }
    }
    return activeDescriptors;
  }

  private static boolean canBeStopped(@Nullable RunContentDescriptor descriptor) {
    @Nullable ProcessHandler processHandler = descriptor != null ? descriptor.getProcessHandler() : null;
    return processHandler != null &&
      !processHandler.isProcessTerminated() &&
      (!processHandler.isProcessTerminating() || processHandler instanceof KillableProcessHandler && ((KillableProcessHandler)processHandler).canKillProcess());
  }

  public abstract static class HandlerItem {
    final String displayName;
    final Image icon;
    final boolean hasSeparator;

    HandlerItem(String displayName, Image icon, boolean hasSeparator) {
      this.displayName = displayName;
      this.icon = icon;
      this.hasSeparator = hasSeparator;
    }

    public String toString() {
      return displayName;
    }

    abstract void stop();
  }
}
