/*
 * Copyright 2013-2017 consulo.io
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
package consulo.wm.impl;

import com.intellij.ide.actions.ActivateToolWindowAction;
import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.*;
import com.intellij.openapi.wm.impl.commands.ApplyWindowInfoCmd;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;
import com.intellij.openapi.wm.impl.commands.InvokeLaterCmd;
import com.intellij.util.ArrayUtil;
import com.intellij.util.EventDispatcher;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBusConnection;
import consulo.component.PersistentStateComponentWithUIState;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.condition.ModuleExtensionCondition;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.*;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.shared.Rectangle2D;
import consulo.ui.style.StandardColors;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Provider;
import java.util.*;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public abstract class ToolWindowManagerBase extends ToolWindowManagerEx implements PersistentStateComponentWithUIState<Element, Element>, Disposable {
  public static final String ID = "ToolWindowManager";

  public static class InitToolWindowsActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
      ToolWindowManagerEx ex = ToolWindowManagerEx.getInstanceEx(project);
      if (ex instanceof ToolWindowManagerBase) {
        ToolWindowManagerBase manager = (ToolWindowManagerBase)ex;
        List<FinalizableCommand> list = new ArrayList<>();
        manager.registerToolWindowsFromBeans(list);
        manager.initAll(list);

        list.add(((ToolWindowManagerBase)ex).connectModuleExtensionListener());

        uiAccess.give(() -> {
          manager.execute(list);
          manager.flushCommands();
        });
      }
    }
  }

  /**
   * Spies on IdeToolWindow properties and applies them to the window
   * state.
   */
  private final class MyToolWindowPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent e) {
      ToolWindow toolWindow = (ToolWindow)e.getSource();
      if (ToolWindowEx.PROP_AVAILABLE.equals(e.getPropertyName())) {
        final WindowInfoImpl info = getInfo(toolWindow.getId());
        if (!toolWindow.isAvailable() && info.isVisible()) {
          hideToolWindow(toolWindow.getId(), false);
        }
      }
      ToolWindowStripeButton button = getStripeButton(toolWindow.getId());
      if (button != null) button.updatePresentation();
      ActivateToolWindowAction.updateToolWindowActionPresentation(toolWindow);
    }
  }

  /**
   * Translates events from InternalDecorator into ToolWindowManager method invocations.
   */
  protected abstract class MyInternalDecoratorListenerBase implements InternalDecoratorListener {
    @Override
    @RequiredUIAccess
    public void anchorChanged(@Nonnull final ToolWindowInternalDecorator source, @Nonnull final ToolWindowAnchor anchor) {
      setToolWindowAnchor(source.getToolWindow().getId(), anchor);
    }

    @Override
    @RequiredUIAccess
    public void autoHideChanged(@Nonnull final ToolWindowInternalDecorator source, final boolean autoHide) {
      setToolWindowAutoHide(source.getToolWindow().getId(), autoHide);
    }

    @Override
    @RequiredUIAccess
    public void hidden(@Nonnull final ToolWindowInternalDecorator source) {
      hideToolWindow(source.getToolWindow().getId(), false);
    }

    @Override
    @RequiredUIAccess
    public void hiddenSide(@Nonnull final ToolWindowInternalDecorator source) {
      hideToolWindow(source.getToolWindow().getId(), true);
    }

    @Override
    public void contentUiTypeChanges(@Nonnull ToolWindowInternalDecorator source, @Nonnull ToolWindowContentUiType type) {
      setContentUiType(source.getToolWindow().getId(), type);
    }

    @Override
    @RequiredUIAccess
    public void activated(@Nonnull final ToolWindowInternalDecorator source) {
      activateToolWindow(source.getToolWindow().getId(), true, true);
    }

    @Override
    @RequiredUIAccess
    public void typeChanged(@Nonnull final ToolWindowInternalDecorator source, @Nonnull final ToolWindowType type) {
      setToolWindowType(source.getToolWindow().getId(), type);
    }

    @Override
    public void sideStatusChanged(@Nonnull final ToolWindowInternalDecorator source, final boolean isSideTool) {
      setSideTool(source.getToolWindow().getId(), isSideTool);
    }

    @Override
    public void visibleStripeButtonChanged(@Nonnull ToolWindowInternalDecorator source, boolean visible) {
      setShowStripeButton(source.getToolWindow().getId(), visible);
    }
  }

  /**
   * This command hides and destroys floating decorator for tool window
   * with specified <code>ID</code>.
   */
  private final class RemoveFloatingDecoratorCmd extends FinalizableCommand {
    private final ToolWindowFloatingDecorator myFloatingDecorator;

    private RemoveFloatingDecoratorCmd(final WindowInfoImpl info) {
      super(myCommandProcessor);
      myFloatingDecorator = getFloatingDecorator(info.getId());
      assert myFloatingDecorator != null;
      myId2FloatingDecorator.remove(info.getId());
      info.setFloatingBounds(myFloatingDecorator.getDecoratorBounds());
    }

    @Override
    public void run() {
      try {
        myFloatingDecorator.dispose();
      }
      finally {
        finish();
      }
    }

    @Override
    @Nullable
    public Condition getExpireCondition() {
      return ApplicationManager.getApplication().getDisposed();
    }
  }

  @NonNls
  protected static final String EDITOR_ELEMENT = "editor";
  @NonNls
  protected static final String ACTIVE_ATTR_VALUE = "active";
  @NonNls
  protected static final String FRAME_ELEMENT = "frame";
  @NonNls
  protected static final String X_ATTR = "x";
  @NonNls
  protected static final String Y_ATTR = "y";
  @NonNls
  protected static final String WIDTH_ATTR = "width";
  @NonNls
  protected static final String HEIGHT_ATTR = "height";
  @NonNls
  protected static final String EXTENDED_STATE_ATTR = "extended-state";
  @NonNls
  protected static final String LAYOUT_TO_RESTORE = "layout-to-restore";

  private static final Logger LOG = Logger.getInstance(ToolWindowManagerBase.class);

  protected final Map<String, ToolWindowInternalDecorator> myId2InternalDecorator = new HashMap<>();
  protected final Map<String, ToolWindowFloatingDecorator> myId2FloatingDecorator = new HashMap<>();
  protected final Map<String, ToolWindowWindowedDecorator> myId2WindowedDecorator = new HashMap<>();

  protected final Map<String, ToolWindowStripeButton> myId2StripeButton = new HashMap<>();

  protected final ToolWindowSideStack mySideStack = new ToolWindowSideStack();
  protected final ToolWindowActiveStack myActiveStack = new ToolWindowActiveStack();

  protected final Project myProject;
  protected final Provider<WindowManager> myWindowManager;
  protected final EventDispatcher<ToolWindowManagerListener> myDispatcher = EventDispatcher.create(ToolWindowManagerListener.class);
  protected final ToolWindowLayout myLayout = new ToolWindowLayout();
  protected ToolWindowLayout myLayoutToRestoreLater = null;
  protected ToolWindowPanel myToolWindowPanel;

  protected final MyToolWindowPropertyChangeListener myToolWindowPropertyChangeListener = new MyToolWindowPropertyChangeListener();

  protected final InternalDecoratorListener myInternalDecoratorListener;
  protected final CommandProcessorBase myCommandProcessor;

  protected ToolWindowManagerBase(Project project, Provider<WindowManager> windowManager) {
    myProject = project;
    myWindowManager = windowManager;

    myCommandProcessor = createCommandProcessor();
    myInternalDecoratorListener = createInternalDecoratorListener();

    MessageBusConnection busConnection = project.getMessageBus().connect(this);
    busConnection.subscribe(ToolWindowManagerListener.TOPIC, myDispatcher.getMulticaster());
  }

  // region Factory Abstract Group
  @Nonnull
  protected abstract CommandProcessorBase createCommandProcessor();

  @Nonnull
  protected abstract InternalDecoratorListener createInternalDecoratorListener();

  @Nonnull
  protected abstract ToolWindowStripeButton createStripeButton(ToolWindowInternalDecorator internalDecorator);

  @Nonnull
  protected abstract ToolWindowEx createToolWindow(String id, LocalizeValue displayName, boolean canCloseContent, @Nullable Object component, boolean shouldBeAvailable);

  @Nonnull
  protected abstract ToolWindowInternalDecorator createInternalDecorator(Project project, @Nonnull WindowInfoImpl info, ToolWindowEx toolWindow, boolean dumbAware);

  // endregion

  // region help methods
  protected void installFocusWatcher(String id, ToolWindow toolWindow) {
  }

  protected void uninstallFocusWatcher(String id) {
  }

  protected void initAll(List<FinalizableCommand> commandsList) {
    appendUpdateToolWindowsPaneCmd(commandsList);
  }

  // endregion

  // region Abstract Platform Dependent Staff
  protected abstract void appendRequestFocusInToolWindowCmd(final String id, List<FinalizableCommand> commandList, boolean forced);

  protected abstract void appendRemoveWindowedDecoratorCmd(final WindowInfoImpl info, final List<FinalizableCommand> commandsList);

  protected abstract void appendAddFloatingDecorator(ToolWindowInternalDecorator internalDecorator, List<FinalizableCommand> commandList, WindowInfoImpl toBeShownInfo);

  protected abstract void appendAddWindowedDecorator(ToolWindowInternalDecorator internalDecorator, List<FinalizableCommand> commandList, WindowInfoImpl toBeShownInfo);

  protected abstract void appendUpdateToolWindowsPaneCmd(final List<FinalizableCommand> commandsList);

  public void appendSetEditorComponentCmd(@Nullable Object component, final List<FinalizableCommand> commandsList) {
    final CommandProcessorBase commandProcessor = myCommandProcessor;
    final FinalizableCommand command = myToolWindowPanel.createSetEditorComponentCmd(component, commandProcessor);
    commandsList.add(command);
  }

  protected abstract boolean hasModalChild(final WindowInfoImpl info);

  // endregion

  protected void applyInfo(@Nonnull String id, WindowInfoImpl info, List<FinalizableCommand> commandsList) {
    info.setVisible(false);
    if (info.isFloating()) {
      appendRemoveFloatingDecoratorCmd(info, commandsList);
    }
    else if (info.isWindowed()) {
      appendRemoveWindowedDecoratorCmd(info, commandsList);
    }
    else { // floating and sliding windows
      appendRemoveDecoratorCmd(id, false, commandsList);
    }
  }

  /**
   * This is helper method. It delegated its functionality to the WindowManager.
   * Before delegating it fires state changed.
   */
  public void execute(@Nonnull List<FinalizableCommand> commandList) {
    for (FinalizableCommand each : commandList) {
      if (each.willChangeState()) {
        fireStateChanged();
        break;
      }
    }

    for (FinalizableCommand each : commandList) {
      each.beforeExecute(this);
    }
    myCommandProcessor.execute(commandList, myProject.getDisposed());
  }

  protected void flushCommands() {
    myCommandProcessor.flush();
  }

  protected void registerToolWindowsFromBeans(List<FinalizableCommand> list) {
    final List<ToolWindowEP> beans = ToolWindowEP.EP_NAME.getExtensionList();
    for (final ToolWindowEP bean : beans) {
      if (checkCondition(myProject, bean)) {
        list.add(new FinalizableCommand(EmptyRunnable.INSTANCE) {
          @Override
          public void run() {
            try {
              initToolWindow(bean);
            }
            catch (ProcessCanceledException e) {
              throw e;
            }
            catch (Throwable t) {
              LOG.error("failed to init toolwindow " + bean.factoryClass, t);
            }
          }
        });
      }
    }
  }

  @Nonnull
  protected FinalizableCommand connectModuleExtensionListener() {
    return new FinalizableCommand(EmptyRunnable.getInstance()) {
      @Override
      public void run() {
        myProject.getMessageBus().connect().subscribe(ModuleExtension.CHANGE_TOPIC, (oldExtension, newExtension) -> {
          boolean extensionVal = newExtension.isEnabled();
          for (final ToolWindowEP bean : ToolWindowEP.EP_NAME.getExtensionList()) {
            boolean value = checkCondition(newExtension, bean);

            if (extensionVal && value) {
              if (isToolWindowRegistered(bean.id)) {
                continue;
              }
              initToolWindow(bean);
            }
            else if (!extensionVal && !value) {
              unregisterToolWindow(bean.id);
            }
          }
        });

        revalidateToolWindows();
      }

      private void revalidateToolWindows() {
        final List<ToolWindowEP> beans = ToolWindowEP.EP_NAME.getExtensionList();

        for (ToolWindowEP bean : beans) {
          boolean value = checkCondition(myProject, bean);

          if (value) {
            if (isToolWindowRegistered(bean.id)) {
              continue;
            }
            initToolWindow(bean);
          }
          else {
            unregisterToolWindow(bean.id);
          }
        }
      }
    };
  }

  public boolean isToolWindowRegistered(String id) {
    return myLayout.isToolWindowRegistered(id);
  }

  /**
   * Checkes whether the specified <code>id</code> defines installed tool
   * window. If it's not then throws <code>IllegalStateException</code>.
   *
   * @throws IllegalStateException if tool window isn't installed.
   */
  protected void checkId(final String id) {
    if (!myLayout.isToolWindowRegistered(id)) {
      throw new IllegalStateException("window with id=\"" + id + "\" isn't registered");
    }
  }

  @Nonnull
  protected ToolWindow registerDisposable(@Nonnull final String id, @Nonnull final Disposable parentDisposable, @Nonnull ToolWindow window) {
    Disposer.register(parentDisposable, () -> unregisterToolWindow(id));
    return window;
  }

  /**
   * @return internal decorator for the tool window with specified <code>ID</code>.
   */
  @Nullable
  protected ToolWindowInternalDecorator getInternalDecorator(final String id) {
    return myId2InternalDecorator.get(id);
  }

  /**
   * @return floating decorator for the tool window with specified <code>ID</code>.
   */
  @Nullable
  protected ToolWindowFloatingDecorator getFloatingDecorator(final String id) {
    return myId2FloatingDecorator.get(id);
  }

  /**
   * @return windowed decorator for the tool window with specified <code>ID</code>.
   */
  @Nullable
  protected ToolWindowWindowedDecorator getWindowedDecorator(String id) {
    return myId2WindowedDecorator.get(id);
  }


  /**
   * @return tool button for the window with specified <code>ID</code>.
   */
  @Nullable
  protected ToolWindowStripeButton getStripeButton(final String id) {
    return myId2StripeButton.get(id);
  }

  private static boolean checkCondition(Project project, ToolWindowEP toolWindowEP) {
    Condition<Project> condition = toolWindowEP.getCondition();
    if (condition != null && !condition.value(project)) {
      return false;
    }
    ModuleExtensionCondition moduleExtensionCondition = toolWindowEP.getModuleExtensionCondition();
    return moduleExtensionCondition.value(project);
  }

  private static boolean checkCondition(ModuleExtension<?> extension, ToolWindowEP toolWindowEP) {
    Condition<Project> condition = toolWindowEP.getCondition();
    if (condition != null && !condition.value(extension.getProject())) {
      return false;
    }
    ModuleExtensionCondition moduleExtensionCondition = toolWindowEP.getModuleExtensionCondition();
    return moduleExtensionCondition.value(extension);
  }

  protected void fireToolWindowRegistered(final String id) {
    myProject.getMessageBus().syncPublisher(ToolWindowManagerListener.TOPIC).toolWindowRegistered(id);
  }

  protected void fireStateChanged() {
    myProject.getMessageBus().syncPublisher(ToolWindowManagerListener.TOPIC).stateChanged();
  }

  public ToolWindowAnchor getToolWindowAnchor(final String id) {
    checkId(id);
    return getInfo(id).getAnchor();
  }

  @RequiredUIAccess
  public ToolWindowType getToolWindowInternalType(final String id) {
    UIAccess.assertIsUIThread();
    checkId(id);
    return getInfo(id).getInternalType();
  }

  public ToolWindowType getToolWindowType(final String id) {
    checkId(id);
    return getInfo(id).getType();
  }

  @RequiredUIAccess
  public boolean isToolWindowActive(final String id) {
    UIAccess.assertIsUIThread();
    checkId(id);
    return getInfo(id).isActive();
  }

  @RequiredUIAccess
  public boolean isToolWindowAutoHide(final String id) {
    UIAccess.assertIsUIThread();
    checkId(id);
    return getInfo(id).isAutoHide();
  }

  public boolean isToolWindowVisible(final String id) {
    checkId(id);
    return getInfo(id).isVisible();
  }

  /**
   * @return info for the tool window with specified <code>ID</code>.
   */
  protected WindowInfoImpl getInfo(final String id) {
    return myLayout.getInfo(id, true);
  }

  public void setDefaultContentUiType(ToolWindow toolWindow, ToolWindowContentUiType type) {
    final WindowInfoImpl info = getInfo(toolWindow.getId());
    if (info.wasRead()) return;
    toolWindow.setContentUiType(type, null);
  }

  /**
   * Helper method. It deactivates (and hides) window with specified <code>id</code>.
   *
   * @param id         <code>id</code> of the tool window to be deactivated.
   * @param shouldHide if <code>true</code> then also hides specified tool window.
   */
  protected void deactivateToolWindowImpl(@Nonnull String id, final boolean shouldHide, @Nonnull List<FinalizableCommand> commandsList) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: deactivateToolWindowImpl(" + id + "," + shouldHide + ")");
    }

    WindowInfoImpl info = getInfo(id);
    if (shouldHide && info.isVisible()) {
      applyInfo(id, info, commandsList);
    }
    info.setActive(false);
    appendApplyWindowInfoCmd(info, commandsList);
  }

  @RequiredUIAccess
  public boolean isSplitMode(String id) {
    UIAccess.assertIsUIThread();
    checkId(id);
    return getInfo(id).isSplit();
  }

  public void setSideTool(String id, boolean isSide) {
    List<FinalizableCommand> commandList = new SmartList<>();
    setSplitModeImpl(id, isSide, commandList);
    execute(commandList);
  }

  private void setSplitModeImpl(final String id, final boolean isSplit, final List<FinalizableCommand> commandList) {
    checkId(id);
    final WindowInfoImpl info = getInfo(id);
    if (isSplit == info.isSplit()) {
      return;
    }

    myLayout.setSplitMode(id, isSplit);

    boolean wasActive = info.isActive();
    if (wasActive) {
      deactivateToolWindowImpl(id, true, commandList);
    }
    final WindowInfoImpl[] infos = myLayout.getInfos();
    for (WindowInfoImpl info1 : infos) {
      appendApplyWindowInfoCmd(info1, commandList);
    }
    if (wasActive) {
      activateToolWindowImpl(id, commandList, true, true);
    }
    commandList.add(myToolWindowPanel.createUpdateButtonPositionCmd(id, myCommandProcessor));
  }

  protected void activateToolWindowImpl(final String id, List<FinalizableCommand> commandList, boolean forced, boolean autoFocusContents) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: activateToolWindowImpl(" + id + ")");
    }
    if (!getToolWindow(id).isAvailable()) {
      // Tool window can be "logically" active but not focused. For example,
      // when the user switched to another application. So we just need to bring
      // tool window's window to front.
      final ToolWindowInternalDecorator decorator = getInternalDecorator(id);
      if (!decorator.hasFocus() && autoFocusContents) {
        appendRequestFocusInToolWindowCmd(id, commandList, forced);
      }
      return;
    }
    deactivateWindows(id, commandList);
    showAndActivate(id, false, commandList, autoFocusContents, forced);
  }

  /**
   * Helper method. It makes window visible, activates it and request focus into the tool window.
   * But it doesn't deactivate other tool windows. Use <code>prepareForActivation</code> method to
   * deactivates other tool windows.
   *
   * @param dirtyMode if <code>true</code> then all UI operations are performed in "dirty" mode.
   *                  It means that UI isn't validated and repainted just after each add/remove operation.
   * @see #prepareForActivation
   */
  protected void showAndActivate(final String id, final boolean dirtyMode, List<FinalizableCommand> commandsList, boolean autoFocusContents, boolean forcedFocusRequest) {
    if (!getToolWindow(id).isAvailable()) {
      return;
    }
    // show activated
    final WindowInfoImpl info = getInfo(id);
    boolean toApplyInfo = false;
    if (!info.isActive()) {
      info.setActive(true);
      toApplyInfo = true;
    }

    showToolWindowImpl(id, dirtyMode, commandsList);

    // activate
    if (toApplyInfo) {
      appendApplyWindowInfoCmd(info, commandsList);
      myActiveStack.push(id);
    }

    if (autoFocusContents && ApplicationManager.getApplication().isActive()) {
      appendRequestFocusInToolWindowCmd(id, commandsList, forcedFocusRequest);
    }
  }

  /**
   * @param dirtyMode if <code>true</code> then all UI operations are performed in dirty mode.
   */
  protected void showToolWindowImpl(final String id, final boolean dirtyMode, final List<FinalizableCommand> commandsList) {
    final WindowInfoImpl toBeShownInfo = getInfo(id);
    if (toBeShownInfo.isVisible() || !getToolWindow(id).isAvailable()) {
      return;
    }

    toBeShownInfo.setVisible(true);
    final ToolWindowInternalDecorator decorator = getInternalDecorator(id);

    if (toBeShownInfo.isFloating()) {
      appendAddFloatingDecorator(decorator, commandsList, toBeShownInfo);
    }
    else if (toBeShownInfo.isWindowed()) {
      appendAddWindowedDecorator(decorator, commandsList, toBeShownInfo);
    }
    else { // docked and sliding windows

      // If there is tool window on the same side then we have to hide it, i.e.
      // clear place for tool window to be shown.
      //
      // We store WindowInfo of hidden tool window in the SideStack (if the tool window
      // is docked and not auto-hide one). Therefore it's possible to restore the
      // hidden tool window when showing tool window will be closed.

      final WindowInfoImpl[] infos = myLayout.getInfos();
      for (final WindowInfoImpl info : infos) {
        if (id.equals(info.getId())) {
          continue;
        }
        if (info.isVisible() && info.getType() == toBeShownInfo.getType() && info.getAnchor() == toBeShownInfo.getAnchor() && info.isSplit() == toBeShownInfo.isSplit()) {
          // hide and deactivate tool window
          info.setVisible(false);
          appendRemoveDecoratorCmd(info.getId(), false, commandsList);
          if (info.isActive()) {
            info.setActive(false);
          }
          appendApplyWindowInfoCmd(info, commandsList);
          // store WindowInfo into the SideStack
          if (info.isDocked() && !info.isAutoHide()) {
            mySideStack.push(info);
          }
        }
      }
      appendAddDecoratorCmd(decorator, toBeShownInfo, dirtyMode, commandsList);

      // Remove tool window from the SideStack.

      mySideStack.remove(id);
    }

    appendApplyWindowInfoCmd(toBeShownInfo, commandsList);
  }

  protected void appendApplyWindowInfoCmd(final WindowInfoImpl info, final List<FinalizableCommand> commandsList) {
    final ToolWindowStripeButton button = getStripeButton(info.getId());
    final ToolWindowInternalDecorator decorator = getInternalDecorator(info.getId());
    commandsList.add(new ApplyWindowInfoCmd(info, button, decorator, myCommandProcessor));
  }

  protected void appendAddButtonCmd(final ToolWindowStripeButton button, final WindowInfoImpl info, final List<FinalizableCommand> commandsList) {
    final Comparator<ToolWindowStripeButton> comparator = myLayout.comparator(info.getAnchor());
    final CommandProcessorBase commandProcessor = myCommandProcessor;
    final FinalizableCommand command = myToolWindowPanel.createAddButtonCmd(button, info, comparator, commandProcessor);
    commandsList.add(command);
  }

  protected void appendAddDecoratorCmd(final ToolWindowInternalDecorator decorator, final WindowInfoImpl info, final boolean dirtyMode, final List<FinalizableCommand> commandsList) {
    final CommandProcessorBase commandProcessor = myCommandProcessor;
    final FinalizableCommand command = myToolWindowPanel.createAddDecoratorCmd(decorator, info, dirtyMode, commandProcessor);
    commandsList.add(command);
  }

  protected void appendRemoveButtonCmd(final String id, final List<FinalizableCommand> commandsList) {
    final FinalizableCommand command = myToolWindowPanel.createRemoveButtonCmd(id, myCommandProcessor);
    if (command != null) {
      commandsList.add(command);
    }
  }

  protected void appendRemoveFloatingDecoratorCmd(final WindowInfoImpl info, final List<FinalizableCommand> commandsList) {
    final RemoveFloatingDecoratorCmd command = new RemoveFloatingDecoratorCmd(info);
    commandsList.add(command);
  }

  protected void appendRemoveDecoratorCmd(final String id, final boolean dirtyMode, final List<FinalizableCommand> commandsList) {
    final FinalizableCommand command = myToolWindowPanel.createRemoveDecoratorCmd(id, dirtyMode, myCommandProcessor);
    commandsList.add(command);
  }

  @RequiredUIAccess
  public ToolWindowContentUiType getContentUiType(String id) {
    UIAccess.assertIsUIThread();
    checkId(id);
    return getInfo(id).getContentUiType();
  }

  public void setContentUiType(String id, ToolWindowContentUiType type) {
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    checkId(id);
    WindowInfoImpl info = getInfo(id);
    info.setContentUiType(type);
    appendApplyWindowInfoCmd(info, commandList);
    execute(commandList);
  }

  @RequiredUIAccess
  public void setToolWindowAnchor(final String id, final ToolWindowAnchor anchor) {
    UIAccess.assertIsUIThread();
    setToolWindowAnchor(id, anchor, -1);
  }

  @RequiredUIAccess
  public void setToolWindowAnchor(final String id, final ToolWindowAnchor anchor, final int order) {
    UIAccess.assertIsUIThread();
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    setToolWindowAnchorImpl(id, anchor, order, commandList);
    execute(commandList);
  }

  private void setToolWindowAnchorImpl(final String id, final ToolWindowAnchor anchor, final int order, final ArrayList<FinalizableCommand> commandsList) {
    checkId(id);
    final WindowInfoImpl info = getInfo(id);
    if (anchor == info.getAnchor() && order == info.getOrder()) {
      return;
    }
    // if tool window isn't visible or only order number is changed then just remove/add stripe button
    if (!info.isVisible() || anchor == info.getAnchor() || info.isFloating()) {
      appendRemoveButtonCmd(id, commandsList);
      myLayout.setAnchor(id, anchor, order);
      // update infos for all window. Actually we have to update only infos affected by
      // setAnchor method
      final WindowInfoImpl[] infos = myLayout.getInfos();
      for (WindowInfoImpl info1 : infos) {
        appendApplyWindowInfoCmd(info1, commandsList);
      }
      appendAddButtonCmd(getStripeButton(id), info, commandsList);
    }
    else { // for docked and sliding windows we have to move buttons and window's decorators
      info.setVisible(false);
      appendRemoveDecoratorCmd(id, false, commandsList);
      appendRemoveButtonCmd(id, commandsList);
      myLayout.setAnchor(id, anchor, order);
      // update infos for all window. Actually we have to update only infos affected by
      // setAnchor method
      final WindowInfoImpl[] infos = myLayout.getInfos();
      for (WindowInfoImpl info1 : infos) {
        appendApplyWindowInfoCmd(info1, commandsList);
      }
      appendAddButtonCmd(getStripeButton(id), info, commandsList);
      showToolWindowImpl(id, false, commandsList);
      if (info.isActive()) {
        appendRequestFocusInToolWindowCmd(id, commandsList, true);
      }
    }
  }

  @RequiredUIAccess
  public void setSideToolAndAnchor(String id, ToolWindowAnchor anchor, int order, boolean isSide) {
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    setToolWindowAnchor(id, anchor, order);
    setSplitModeImpl(id, isSide, commandList);
    execute(commandList);
  }

  private void setSplitModeImpl(final String id, final boolean isSplit, final ArrayList<FinalizableCommand> commandList) {
    checkId(id);
    final WindowInfoImpl info = getInfo(id);
    if (isSplit == info.isSplit()) {
      return;
    }

    myLayout.setSplitMode(id, isSplit);

    boolean wasActive = info.isActive();
    if (wasActive) {
      deactivateToolWindowImpl(id, true, commandList);
    }
    final WindowInfoImpl[] infos = myLayout.getInfos();
    for (WindowInfoImpl info1 : infos) {
      appendApplyWindowInfoCmd(info1, commandList);
    }
    if (wasActive) {
      activateToolWindowImpl(id, commandList, true, true);
    }
    commandList.add(myToolWindowPanel.createUpdateButtonPositionCmd(id, myCommandProcessor));
  }

  @RequiredUIAccess
  public void setToolWindowAutoHide(final String id, final boolean autoHide) {
    UIAccess.assertIsUIThread();
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    setToolWindowAutoHideImpl(id, autoHide, commandList);
    execute(commandList);
  }

  protected void setToolWindowAutoHideImpl(final String id, final boolean autoHide, final ArrayList<FinalizableCommand> commandsList) {
    checkId(id);
    final WindowInfoImpl info = getInfo(id);
    if (info.isAutoHide() == autoHide) {
      return;
    }
    info.setAutoHide(autoHide);
    appendApplyWindowInfoCmd(info, commandsList);
    if (info.isVisible()) {
      deactivateWindows(id, commandsList);
      showAndActivate(id, false, commandsList, true, true);
    }
  }

  @RequiredUIAccess
  public void setToolWindowType(final String id, final ToolWindowType type) {
    UIAccess.assertIsUIThread();
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    setToolWindowTypeImpl(id, type, commandList);
    execute(commandList);
  }

  protected void setToolWindowTypeImpl(final String id, final ToolWindowType type, final ArrayList<FinalizableCommand> commandsList) {
    checkId(id);
    final WindowInfoImpl info = getInfo(id);
    if (info.getType() == type) {
      return;
    }
    if (info.isVisible()) {
      final boolean dirtyMode = info.isDocked() || info.isSliding();
      info.setVisible(false);
      if (info.isFloating()) {
        appendRemoveFloatingDecoratorCmd(info, commandsList);
      }
      else if (info.isWindowed()) {
        appendRemoveWindowedDecoratorCmd(info, commandsList);
      }
      else { // docked and sliding windows
        appendRemoveDecoratorCmd(id, dirtyMode, commandsList);
      }
      info.setType(type);
      appendApplyWindowInfoCmd(info, commandsList);
      deactivateWindows(id, commandsList);
      showAndActivate(id, dirtyMode, commandsList, true, true);
      appendUpdateToolWindowsPaneCmd(commandsList);
    }
    else {
      info.setType(type);
      appendApplyWindowInfoCmd(info, commandsList);
    }
  }

  @RequiredUIAccess
  public void activateToolWindow(final String id, boolean forced, boolean autoFocusContents) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: activateToolWindow(" + id + ")");
    }
    UIAccess.assertIsUIThread();
    checkId(id);

    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    activateToolWindowImpl(id, commandList, forced, autoFocusContents);
    execute(commandList);
  }

  private void deactivateWindows(@Nonnull String idToIgnore, @Nonnull List<FinalizableCommand> commandList) {
    for (WindowInfoImpl info : myLayout.getInfos()) {
      if (!idToIgnore.equals(info.getId())) {
        deactivateToolWindowImpl(info.getId(), isToHideOnDeactivation(info), commandList);
      }
    }
  }

  private static boolean isToHideOnDeactivation(@Nonnull final WindowInfoImpl info) {
    if (info.isFloating() || info.isWindowed()) return false;
    return info.isAutoHide() || info.isSliding();
  }

  @RequiredUIAccess
  public void showToolWindow(final String id) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: showToolWindow(" + id + ")");
    }
    UIAccess.assertIsUIThread();
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    showToolWindowImpl(id, false, commandList);
    execute(commandList);
  }

  @RequiredUIAccess
  @Override
  public void hideToolWindow(@Nonnull final String id, final boolean hideSide) {
    hideToolWindow(id, hideSide, true);
  }

  @RequiredUIAccess
  @Override
  public void hideToolWindow(final String id, final boolean hideSide, final boolean moveFocus) {
    UIAccess.assertIsUIThread();
    checkId(id);
    final WindowInfoImpl info = getInfo(id);
    if (!info.isVisible()) return;
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    final boolean wasActive = info.isActive();

    // hide and deactivate

    deactivateToolWindowImpl(id, true, commandList);

    if (hideSide && !info.isFloating()) {
      final List<String> ids = myLayout.getVisibleIdsOn(info.getAnchor(), this);
      for (String each : ids) {
        myActiveStack.remove(each, true);
      }


      while (!mySideStack.isEmpty(info.getAnchor())) {
        mySideStack.pop(info.getAnchor());
      }

      final String[] all = getToolWindowIds();
      for (String eachId : all) {
        final WindowInfoImpl eachInfo = getInfo(eachId);
        if (eachInfo.isVisible() && eachInfo.getAnchor() == info.getAnchor()) {
          deactivateToolWindowImpl(eachId, true, commandList);
        }
      }
    }
    else if (isStackEnabled()) {

      // first of all we have to find tool window that was located at the same side and
      // was hidden.

      WindowInfoImpl info2 = null;
      while (!mySideStack.isEmpty(info.getAnchor())) {
        final WindowInfoImpl storedInfo = mySideStack.pop(info.getAnchor());
        if (storedInfo.isSplit() != info.isSplit()) {
          continue;
        }

        final WindowInfoImpl currentInfo = getInfo(storedInfo.getId());
        LOG.assertTrue(currentInfo != null);
        // SideStack contains copies of real WindowInfos. It means that
        // these stored infos can be invalid. The following loop removes invalid WindowInfos.
        if (storedInfo.getAnchor() == currentInfo.getAnchor() && storedInfo.getType() == currentInfo.getType() && storedInfo.isAutoHide() == currentInfo.isAutoHide()) {
          info2 = storedInfo;
          break;
        }
      }
      if (info2 != null) {
        showToolWindowImpl(info2.getId(), false, commandList);
      }

      // If we hide currently active tool window then we should activate the previous
      // one which is located in the tool window stack.
      // Activate another tool window if no active tool window exists and
      // window stack is enabled.

      myActiveStack.remove(id, false); // hidden window should be at the top of stack


      if (wasActive && moveFocus && !myActiveStack.isEmpty()) {
        final String toBeActivatedId = myActiveStack.pop();
        if (getRegisteredInfoOrLogError(toBeActivatedId).isVisible() || isStackEnabled()) {
          activateToolWindowImpl(toBeActivatedId, commandList, false, true);
        }
        else {
          focusToolWindowByDefault(id);
        }
      }
    }

    execute(commandList);
  }

  @Nonnull
  private WindowInfoImpl getRegisteredInfoOrLogError(@Nonnull String id) {
    WindowInfoImpl info = myLayout.getInfo(id, true);
    if (info == null) {
      throw new IllegalThreadStateException("window with id=\"" + id + "\" is unknown");
    }
    return info;
  }

  @RequiredUIAccess
  protected void focusToolWindowByDefault(@Nullable String idToIngore) {
    String toFocus = null;

    for (String each : myActiveStack.getStack()) {
      if (idToIngore != null && idToIngore.equalsIgnoreCase(each)) continue;

      if (getInfo(each).isVisible()) {
        toFocus = each;
        break;
      }
    }

    if (toFocus == null) {
      for (String each : myActiveStack.getPersistentStack()) {
        if (idToIngore != null && idToIngore.equalsIgnoreCase(each)) continue;

        if (getInfo(each).isVisible()) {
          toFocus = each;
          break;
        }
      }
    }

    if (toFocus != null) {
      activateToolWindow(toFocus, false, true);
    }
  }

  protected boolean hasOpenEditorFiles() {
    return FileEditorManager.getInstance(myProject).getOpenFiles().length > 0;
  }

  protected static boolean isStackEnabled() {
    return Registry.is("ide.enable.toolwindow.stack");
  }

  @RequiredUIAccess
  @Override
  public void initToolWindow(@Nonnull ToolWindowEP bean) {
    WindowInfoImpl before = myLayout.getInfo(bean.id, false);
    boolean visible = before != null && before.isVisible();
    Object label = createInitializingLabel();
    ToolWindowAnchor toolWindowAnchor = ToolWindowAnchor.fromText(bean.anchor);
    final ToolWindowFactory factory = bean.getToolWindowFactory();
    ToolWindow window = registerToolWindow(bean.id, bean.displayName, label, toolWindowAnchor, false, bean.canCloseContents, DumbService.isDumbAware(factory), factory.shouldBeAvailable(myProject));
    final ToolWindowBase toolWindow = (ToolWindowBase)registerDisposable(bean.id, myProject, window);
    toolWindow.setContentFactory(factory);
    String beanIcon = bean.icon;
    if (beanIcon != null && toolWindow.getIcon() == null) {
      Image targetIcon;
      ImageKey imageKey = ImageKey.fromString(beanIcon, 13, 13);
      if(imageKey != null) {
        targetIcon = imageKey;
      }
      else {
        targetIcon = ImageEffects.colorFilled(13, 13, StandardColors.MAGENTA);
      }
      toolWindow.setIcon(targetIcon);
    }

    WindowInfoImpl info = getInfo(bean.id);
    if (!info.isSplit() && bean.secondary && !info.wasRead()) {
      toolWindow.setSplitMode(true, null);
    }

    final DumbAwareRunnable runnable = () -> {
      if (toolWindow.isDisposed()) return;

      toolWindow.ensureContentInitialized();
    };
    if (visible) {
      runnable.run();
    }
    else {
      doWhenFirstShown(label, runnable);
    }
  }

  @Nonnull
  protected abstract Object createInitializingLabel();

  @RequiredUIAccess
  protected abstract void doWhenFirstShown(Object component, Runnable runnable);

  public boolean isUnified() {
    return false;
  }

  @Nonnull
  @RequiredUIAccess
  protected ToolWindow registerToolWindow(@Nonnull final String id,
                                          @Nullable final Object component,
                                          @Nonnull final ToolWindowAnchor anchor,
                                          boolean sideTool,
                                          boolean canCloseContent,
                                          final boolean canWorkInDumbMode,
                                          boolean shouldBeAvailable) {
    return registerToolWindow(id, null, component, anchor, sideTool, canCloseContent, canWorkInDumbMode, shouldBeAvailable);
  }

  @Nonnull
  @RequiredUIAccess
  protected ToolWindow registerToolWindow(@Nonnull final String id,
                                          @Nullable LocalizeValue displayName,
                                          @Nullable final Object component,
                                          @Nonnull final ToolWindowAnchor anchor,
                                          boolean sideTool,
                                          boolean canCloseContent,
                                          final boolean canWorkInDumbMode,
                                          boolean shouldBeAvailable) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: installToolWindow(" + id + "," + component + "," + anchor + "\")");
    }
    UIAccess.assertIsUIThread();
    if (myLayout.isToolWindowRegistered(id)) {
      throw new IllegalArgumentException("window with id=\"" + id + "\" is already registered");
    }

    final WindowInfoImpl info = myLayout.register(id, anchor, sideTool);
    final boolean wasActive = info.isActive();
    final boolean wasVisible = info.isVisible();
    info.setActive(false);
    info.setVisible(false);

    LocalizeValue displayNameNonnull = displayName == null ? LocalizeValue.of(id) : displayName;

    // Create decorator
    ToolWindowEx toolWindow = createToolWindow(id, displayNameNonnull, canCloseContent, component, shouldBeAvailable);

    ToolWindowInternalDecorator decorator = createInternalDecorator(myProject, info.copy(), toolWindow, canWorkInDumbMode);
    ActivateToolWindowAction.ensureToolWindowActionRegistered(toolWindow);
    myId2InternalDecorator.put(id, decorator);
    decorator.addInternalDecoratorListener(myInternalDecoratorListener);
    toolWindow.addPropertyChangeListener(myToolWindowPropertyChangeListener);

    installFocusWatcher(id, toolWindow);

    // Create and show tool button

    final ToolWindowStripeButton button = createStripeButton(decorator);
    myId2StripeButton.put(id, button);
    List<FinalizableCommand> commandsList = new ArrayList<>();
    appendAddButtonCmd(button, info, commandsList);

    // If preloaded info is visible or active then we have to show/activate the installed
    // tool window. This step has sense only for windows which are not in the auto hide
    // mode. But if tool window was active but its mode doesn't allow to activate it again
    // (for example, tool window is in auto hide mode) then we just activate editor component.

    if (!info.isAutoHide() && (info.isDocked() || info.isFloating())) {
      if (wasActive) {
        activateToolWindowImpl(info.getId(), commandsList, true, true);
      }
      else if (wasVisible) {
        showToolWindowImpl(info.getId(), false, commandsList);
      }
    }

    execute(commandsList);
    fireToolWindowRegistered(id);
    return toolWindow;
  }

  @RequiredUIAccess
  @Override
  public void unregisterToolWindow(@Nonnull final String id) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: unregisterToolWindow(" + id + ")");
    }
    UIAccess.assertIsUIThread();
    if (!myLayout.isToolWindowRegistered(id)) {
      return;
    }

    final WindowInfoImpl info = getInfo(id);
    final ToolWindowEx toolWindow = (ToolWindowEx)getToolWindow(id);
    // Save recent appearance of tool window
    myLayout.unregister(id);
    // Remove decorator and tool button from the screen
    List<FinalizableCommand> commandsList = new ArrayList<>();
    if (info.isVisible()) {
      applyInfo(id, info, commandsList);
    }
    appendRemoveButtonCmd(id, commandsList);
    appendApplyWindowInfoCmd(info, commandsList);
    execute(commandsList);
    // Remove all references on tool window and save its last properties
    assert toolWindow != null;
    toolWindow.removePropertyChangeListener(myToolWindowPropertyChangeListener);
    myActiveStack.remove(id, true);
    mySideStack.remove(id);
    // Destroy stripe button
    final ToolWindowStripeButton button = getStripeButton(id);
    Disposer.dispose(button);
    myId2StripeButton.remove(id);

    uninstallFocusWatcher(id);

    // Destroy decorator
    final ToolWindowInternalDecorator decorator = getInternalDecorator(id);
    decorator.dispose();
    decorator.removeInternalDecoratorListener(myInternalDecoratorListener);
    myId2InternalDecorator.remove(id);
  }

  @Override
  public ToolWindow getToolWindow(final String id) {
    if (!myLayout.isToolWindowRegistered(id)) {
      return null;
    }
    ToolWindowInternalDecorator decorator = getInternalDecorator(id);
    return decorator != null ? decorator.getToolWindow() : null;
  }

  @Override
  public void invokeLater(@Nonnull final Runnable runnable) {
    List<FinalizableCommand> commandList = new ArrayList<>();
    commandList.add(new InvokeLaterCmd(runnable, myCommandProcessor));
    execute(commandList);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ToolWindow registerToolWindow(@Nonnull final String id, final boolean canCloseContent, @Nonnull final ToolWindowAnchor anchor) {
    return registerToolWindow(id, null, anchor, false, canCloseContent, false, true);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ToolWindow registerToolWindow(@Nonnull final String id, final boolean canCloseContent, @Nonnull final ToolWindowAnchor anchor, final boolean secondary) {
    return registerToolWindow(id, null, anchor, secondary, canCloseContent, false, true);
  }


  @RequiredUIAccess
  @Nonnull
  @Override
  public ToolWindow registerToolWindow(@Nonnull final String id,
                                       final boolean canCloseContent,
                                       @Nonnull final ToolWindowAnchor anchor,
                                       @Nonnull final Disposable parentDisposable,
                                       final boolean canWorkInDumbMode) {
    return registerToolWindow(id, canCloseContent, anchor, parentDisposable, canWorkInDumbMode, false);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ToolWindow registerToolWindow(@Nonnull String id, boolean canCloseContent, @Nonnull ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode, boolean secondary) {
    ToolWindow window = registerToolWindow(id, null, anchor, secondary, canCloseContent, canWorkInDumbMode, true);
    return registerDisposable(id, parentDisposable, window);
  }

  @Override
  public void addToolWindowManagerListener(@Nonnull ToolWindowManagerListener l) {
    myDispatcher.addListener(l);
  }

  @Override
  public void addToolWindowManagerListener(@Nonnull ToolWindowManagerListener l, @Nonnull Disposable parentDisposable) {
    myProject.getMessageBus().connect(parentDisposable).subscribe(ToolWindowManagerListener.TOPIC, l);
  }

  @Override
  public void removeToolWindowManagerListener(@Nonnull ToolWindowManagerListener l) {
    myDispatcher.removeListener(l);
  }

  @Override
  public void loadState(Element state) {
    for (Element e : state.getChildren()) {
      if (ToolWindowLayout.TAG.equals(e.getName())) {
        myLayout.readExternal(e);
      }
      else if (LAYOUT_TO_RESTORE.equals(e.getName())) {
        myLayoutToRestoreLater = new ToolWindowLayout();
        myLayoutToRestoreLater.readExternal(e);
      }
    }
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public ToolWindowLayout getLayout() {
    UIAccess.assertIsUIThread();
    return myLayout;
  }

  @Override
  public List<String> getIdsOn(@Nonnull final ToolWindowAnchor anchor) {
    return myLayout.getVisibleIdsOn(anchor, this);
  }

  @Nonnull
  @Override
  public String[] getToolWindowIds() {
    final WindowInfoImpl[] infos = myLayout.getInfos();
    final String[] ids = ArrayUtil.newStringArray(infos.length);
    for (int i = 0; i < infos.length; i++) {
      ids[i] = infos[i].getId();
    }
    return ids;
  }

  @Override
  public void setLayoutToRestoreLater(ToolWindowLayout layout) {
    myLayoutToRestoreLater = layout;
  }

  @Override
  public ToolWindowLayout getLayoutToRestoreLater() {
    return myLayoutToRestoreLater;
  }

  @Nonnull
  @Override
  public IdeFocusManager getFocusManager() {
    return IdeFocusManager.getInstance(myProject);
  }

  @RequiredUIAccess
  @Override
  public String getActiveToolWindowId() {
    UIAccess.assertIsUIThread();
    return myLayout.getActiveId();
  }

  @Override
  public void clearSideStack() {
    mySideStack.clear();
  }

  @Override
  public void dispose() {
    for (String id : new ArrayList<>(myId2StripeButton.keySet())) {
      unregisterToolWindow(id);
    }

    assert myId2StripeButton.isEmpty();
  }

  @RequiredUIAccess
  @Override
  public void setLayout(@Nonnull final ToolWindowLayout layout) {
    UIAccess.assertIsUIThread();
    final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
    // hide tool window that are invisible in new layout
    final WindowInfoImpl[] currentInfos = myLayout.getInfos();
    for (final WindowInfoImpl currentInfo : currentInfos) {
      final WindowInfoImpl info = layout.getInfo(currentInfo.getId(), false);
      if (info == null) {
        continue;
      }
      if (currentInfo.isVisible() && !info.isVisible()) {
        deactivateToolWindowImpl(currentInfo.getId(), true, commandList);
      }
    }
    // change anchor of tool windows
    for (final WindowInfoImpl currentInfo : currentInfos) {
      final WindowInfoImpl info = layout.getInfo(currentInfo.getId(), false);
      if (info == null) {
        continue;
      }
      if (currentInfo.getAnchor() != info.getAnchor() || currentInfo.getOrder() != info.getOrder()) {
        setToolWindowAnchorImpl(currentInfo.getId(), info.getAnchor(), info.getOrder(), commandList);
      }
    }
    // change types of tool windows
    for (final WindowInfoImpl currentInfo : currentInfos) {
      final WindowInfoImpl info = layout.getInfo(currentInfo.getId(), false);
      if (info == null) {
        continue;
      }
      if (currentInfo.getType() != info.getType()) {
        setToolWindowTypeImpl(currentInfo.getId(), info.getType(), commandList);
      }
    }
    // change auto-hide state
    for (final WindowInfoImpl currentInfo : currentInfos) {
      final WindowInfoImpl info = layout.getInfo(currentInfo.getId(), false);
      if (info == null) {
        continue;
      }
      if (currentInfo.isAutoHide() != info.isAutoHide()) {
        setToolWindowAutoHideImpl(currentInfo.getId(), info.isAutoHide(), commandList);
      }
    }
    // restore visibility
    for (final WindowInfoImpl currentInfo : currentInfos) {
      final WindowInfoImpl info = layout.getInfo(currentInfo.getId(), false);
      if (info == null) {
        continue;
      }
      if (info.isVisible()) {
        showToolWindowImpl(currentInfo.getId(), false, commandList);
      }
    }
    execute(commandList);
  }

  @RequiredUIAccess
  public void setDefaultState(@Nonnull final ToolWindow toolWindow, @Nullable final ToolWindowAnchor anchor, @Nullable final ToolWindowType type, @Nullable final Rectangle2D floatingBounds) {
    final WindowInfoImpl info = getInfo(toolWindow.getId());
    if (info.wasRead()) return;

    if (floatingBounds != null) {
      info.setFloatingBounds(floatingBounds);
    }

    if (anchor != null) {
      toolWindow.setAnchor(anchor, null);
    }

    if (type != null) {
      toolWindow.setType(type, null);
    }
  }

  public void setShowStripeButton(@Nonnull String id, boolean visibleOnPanel) {
    checkId(id);
    WindowInfoImpl info = getInfo(id);
    if (visibleOnPanel == info.isShowStripeButton()) {
      return;
    }
    info.setShowStripeButton(visibleOnPanel);
    UsageTrigger.trigger("StripeButton[" + id + "]." + (visibleOnPanel ? "shown" : "hidden"));

    List<FinalizableCommand> commandList = new ArrayList<>();
    appendApplyWindowInfoCmd(info, commandList);
    execute(commandList);
  }

  public boolean isShowStripeButton(@Nonnull String id) {
    WindowInfoImpl info = getInfo(id);
    return info == null || info.isShowStripeButton();
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  // TODO [VISTALL]  AWT & Swing dependency

  // region AWT & Swing dependency
  @Nonnull
  @Override
  @RequiredUIAccess
  public ToolWindow registerToolWindow(@Nonnull final String id, @Nonnull final javax.swing.JComponent component, @Nonnull final ToolWindowAnchor anchor) {
    return registerToolWindow(id, component, anchor, false);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ToolWindow registerToolWindow(@Nonnull final String id,
                                       @Nonnull javax.swing.JComponent component,
                                       @Nonnull ToolWindowAnchor anchor,
                                       @Nonnull Disposable parentDisposable,
                                       boolean canWorkInDumbMode,
                                       boolean canCloseContents) {
    return registerDisposable(id, parentDisposable, registerToolWindow(id, component, anchor, false, canCloseContents, canWorkInDumbMode, true));
  }

  @Nonnull
  @RequiredUIAccess
  private ToolWindow registerToolWindow(@Nonnull final String id, @Nonnull final javax.swing.JComponent component, @Nonnull final ToolWindowAnchor anchor, boolean canWorkInDumbMode) {
    return registerToolWindow(id, component, anchor, false, false, canWorkInDumbMode, true);
  }

  // endregion
}
