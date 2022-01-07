/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.execution.console;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ObservableConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DuplexConsoleView<S extends ConsoleView, T extends ConsoleView> extends JPanel implements ConsoleView, ObservableConsoleView, DataProvider {
  private final static String PRIMARY_CONSOLE_PANEL = "PRIMARY_CONSOLE_PANEL";
  private final static String SECONDARY_CONSOLE_PANEL = "SECONDARY_CONSOLE_PANEL";

  @Nonnull
  private final S myPrimaryConsoleView;
  @Nonnull
  private final T mySecondaryConsoleView;
  @Nullable
  private final String myStateStorageKey;

  private boolean myPrimary;
  @Nullable
  private ProcessHandler myProcessHandler;
  @Nonnull
  private final SwitchDuplexConsoleViewAction mySwitchConsoleAction;
  private boolean myDisableSwitchConsoleActionOnProcessEnd = true;

  public DuplexConsoleView(@Nonnull S primaryConsoleView, @Nonnull T secondaryConsoleView) {
    this(primaryConsoleView, secondaryConsoleView, null);
  }

  public DuplexConsoleView(@Nonnull S primaryConsoleView, @Nonnull T secondaryConsoleView, @Nullable String stateStorageKey) {
    super(new CardLayout());
    myPrimaryConsoleView = primaryConsoleView;
    mySecondaryConsoleView = secondaryConsoleView;
    myStateStorageKey = stateStorageKey;

    add(myPrimaryConsoleView.getComponent(), PRIMARY_CONSOLE_PANEL);
    add(mySecondaryConsoleView.getComponent(), SECONDARY_CONSOLE_PANEL);

    mySwitchConsoleAction = new SwitchDuplexConsoleViewAction();

    myPrimary = true;
    enableConsole(getStoredState());

    Disposer.register(this, myPrimaryConsoleView);
    Disposer.register(this, mySecondaryConsoleView);
  }

  public static <S extends ConsoleView, T extends ConsoleView> DuplexConsoleView<S, T> create(@Nonnull S primary,
                                                                                              @Nonnull T secondary,
                                                                                              @Nullable String stateStorageKey) {
    return new DuplexConsoleView<>(primary, secondary, stateStorageKey);
  }

  private void setStoredState(boolean primary) {
    if (myStateStorageKey != null) {
      PropertiesComponent.getInstance().setValue(myStateStorageKey, primary);
    }
  }

  private boolean getStoredState() {
    if (myStateStorageKey == null) {
      return false;
    }
    return PropertiesComponent.getInstance().getBoolean(myStateStorageKey);
  }

  public void enableConsole(boolean primary) {
    if (primary == myPrimary) {
      // nothing to do
      return;
    }

    CardLayout cl = (CardLayout)(getLayout());
    cl.show(this, primary ? PRIMARY_CONSOLE_PANEL : SECONDARY_CONSOLE_PANEL);

    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(getSubConsoleView(primary).getComponent());

    myPrimary = primary;
  }

  public boolean isPrimaryConsoleEnabled() {
    return myPrimary;
  }

  @Nonnull
  public S getPrimaryConsoleView() {
    return myPrimaryConsoleView;
  }

  @Nonnull
  public T getSecondaryConsoleView() {
    return mySecondaryConsoleView;
  }

  public ConsoleView getSubConsoleView(boolean primary) {
    return primary ? getPrimaryConsoleView() : getSecondaryConsoleView();
  }

  @Override
  public void print(@Nonnull String s, @Nonnull ConsoleViewContentType contentType) {
    myPrimaryConsoleView.print(s, contentType);
    mySecondaryConsoleView.print(s, contentType);
  }

  @Override
  public void clear() {
    myPrimaryConsoleView.clear();
    mySecondaryConsoleView.clear();
  }

  @Override
  public void scrollTo(int offset) {
    myPrimaryConsoleView.scrollTo(offset);
    mySecondaryConsoleView.scrollTo(offset);
  }

  @Override
  public void attachToProcess(ProcessHandler processHandler) {
    myProcessHandler = processHandler;

    myPrimaryConsoleView.attachToProcess(processHandler);
    mySecondaryConsoleView.attachToProcess(processHandler);
  }

  @Override
  public void setOutputPaused(boolean value) {
    myPrimaryConsoleView.setOutputPaused(value);
    mySecondaryConsoleView.setOutputPaused(value);
  }

  @Override
  public boolean isOutputPaused() {
    return false;
  }

  @Override
  public boolean hasDeferredOutput() {
    return myPrimaryConsoleView.hasDeferredOutput() && mySecondaryConsoleView.hasDeferredOutput();
  }

  @Override
  public void performWhenNoDeferredOutput(@Nonnull Runnable runnable) {
  }

  @Override
  public void setHelpId(String helpId) {
    myPrimaryConsoleView.setHelpId(helpId);
    mySecondaryConsoleView.setHelpId(helpId);
  }

  @Override
  public void addMessageFilter(@Nonnull Filter filter) {
    myPrimaryConsoleView.addMessageFilter(filter);
    mySecondaryConsoleView.addMessageFilter(filter);
  }

  @Override
  public void printHyperlink(@Nonnull String hyperlinkText, HyperlinkInfo info) {
    myPrimaryConsoleView.printHyperlink(hyperlinkText, info);
    mySecondaryConsoleView.printHyperlink(hyperlinkText, info);
  }

  @Override
  public int getContentSize() {
    return myPrimaryConsoleView.getContentSize();
  }

  @Override
  public boolean canPause() {
    return false;
  }


  @Nonnull
  @Override
  public AnAction[] createConsoleActions() {
    List<AnAction> actions = new ArrayList<>();
    actions.addAll(
            mergeConsoleActions(Arrays.asList(myPrimaryConsoleView.createConsoleActions()), Arrays.asList(mySecondaryConsoleView.createConsoleActions())));
    actions.add(mySwitchConsoleAction);

    LanguageConsoleView langConsole = ContainerUtil.findInstance(Arrays.asList(myPrimaryConsoleView, mySecondaryConsoleView), LanguageConsoleView.class);
    ConsoleHistoryController controller = langConsole != null ? ConsoleHistoryController.getController(langConsole) : null;
    if (controller != null) actions.add(controller.getBrowseHistory());

    return ArrayUtil.toObjectArray(actions, AnAction.class);
  }

  @Override
  public void allowHeavyFilters() {
    myPrimaryConsoleView.allowHeavyFilters();
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return this;
  }

  @Override
  public void dispose() {
    // registered children in constructor
  }

  @Override
  public void addChangeListener(@Nonnull ChangeListener listener, @Nonnull Disposable parent) {
    if (myPrimaryConsoleView instanceof ObservableConsoleView) {
      ((ObservableConsoleView)myPrimaryConsoleView).addChangeListener(listener, parent);
    }
    if (mySecondaryConsoleView instanceof ObservableConsoleView) {
      ((ObservableConsoleView)mySecondaryConsoleView).addChangeListener(listener, parent);
    }
  }

  @Nullable
  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    final ConsoleView consoleView = getSubConsoleView(isPrimaryConsoleEnabled());
    if (consoleView instanceof DataProvider) {
      return ((DataProvider)consoleView).getData(dataId);
    }
    else {
      return null;
    }
  }

  @Nonnull
  public Presentation getSwitchConsoleActionPresentation() {
    return mySwitchConsoleAction.getTemplatePresentation();
  }

  public void setDisableSwitchConsoleActionOnProcessEnd(boolean disableSwitchConsoleActionOnProcessEnd) {
    myDisableSwitchConsoleActionOnProcessEnd = disableSwitchConsoleActionOnProcessEnd;
  }

  @Nonnull
  private List<AnAction> mergeConsoleActions(@Nonnull List<AnAction> actions1, @Nonnull Collection<AnAction> actions2) {
    return ContainerUtil.map(actions1, action1 -> {
      final AnAction action2 = ContainerUtil.find(actions2, action -> action1.getClass() == action.getClass() &&
                                                                      StringUtil.equals(action1.getTemplatePresentation().getText(),
                                                                                        action.getTemplatePresentation().getText()));
      if (action2 instanceof ToggleUseSoftWrapsToolbarAction) {
        return new MergedWrapTextAction(((ToggleUseSoftWrapsToolbarAction)action1), (ToggleUseSoftWrapsToolbarAction)action2);
      }
      else if (action2 instanceof ScrollToTheEndToolbarAction) {
        return new MergedToggleAction(((ToggleAction)action1), (ToggleAction)action2);
      }
      else if (action2 instanceof ConsoleViewImpl.ClearAllAction) {
        return new MergedAction(action1, action2);
      }
      else {
        return action1;
      }
    });
  }

  private class MergedWrapTextAction extends MergedToggleAction {

    private MergedWrapTextAction(@Nonnull ToggleUseSoftWrapsToolbarAction action1, @Nonnull ToggleUseSoftWrapsToolbarAction action2) {
      super(action1, action2);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      super.setSelected(e, state);
      DuplexConsoleView.this.getComponent().revalidate();
    }
  }

  private class SwitchDuplexConsoleViewAction extends ToggleAction implements DumbAware {

    public SwitchDuplexConsoleViewAction() {
      super(ExecutionBundle.message("run.configuration.show.command.line.action.name"), null, AllIcons.Debugger.ToolConsole);
    }

    @Override
    public boolean isSelected(final AnActionEvent event) {
      return !isPrimaryConsoleEnabled();
    }

    @Override
    public void setSelected(final AnActionEvent event, final boolean flag) {
      enableConsole(!flag);
      setStoredState(!flag);
      ApplicationManager.getApplication().invokeLater(() -> update(event));
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
      super.update(event);
      if (!myDisableSwitchConsoleActionOnProcessEnd) return;

      final Presentation presentation = event.getPresentation();
      final boolean isRunning = myProcessHandler != null && !myProcessHandler.isProcessTerminated();
      if (isRunning) {
        presentation.setEnabled(true);
      }
      else {
        enableConsole(true);
        presentation.putClientProperty(SELECTED_PROPERTY, false);
        presentation.setEnabled(false);
      }
    }
  }

  private static class MergedToggleAction extends ToggleAction implements DumbAware {
    @Nonnull
    private final ToggleAction myAction1;
    @Nonnull
    private final ToggleAction myAction2;

    private MergedToggleAction(@Nonnull ToggleAction action1, @Nonnull ToggleAction action2) {
      myAction1 = action1;
      myAction2 = action2;
      copyFrom(action1);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myAction1.isSelected(e);
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      myAction1.setSelected(e, state);
      myAction2.setSelected(e, state);
    }
  }

  private static class MergedAction extends AnAction implements DumbAware {
    @Nonnull
    private final AnAction myAction1;
    @Nonnull
    private final AnAction myAction2;

    private MergedAction(@Nonnull AnAction action1, @Nonnull AnAction action2) {
      myAction1 = action1;
      myAction2 = action2;
      copyFrom(action1);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      myAction1.actionPerformed(e);
      myAction2.actionPerformed(e);
    }
  }

}
