/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.dialog;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationManagerEx;
import consulo.application.ui.ApplicationWindowStateService;
import consulo.application.ui.WindowStateService;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.Queryable;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import consulo.awt.hacking.DialogHacking;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.desktop.awt.startup.splash.DesktopSplash;
import consulo.desktop.awt.ui.impl.window.JDialogAsUIWindow;
import consulo.desktop.awt.wm.impl.DesktopWindowManagerImpl;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.IdeEventQueue;
import consulo.ide.impl.idea.ide.impl.TypeSafeDataProviderAdapter;
import consulo.ide.impl.idea.openapi.command.CommandProcessorEx;
import consulo.ide.impl.idea.openapi.ui.impl.AbstractDialog;
import consulo.ide.impl.idea.openapi.ui.impl.HeadlessDialog;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.util.ui.OwnerOptional;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.ProjectWindowStateService;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameUtil;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.StackingPopupDispatcher;
import consulo.undoRedo.CommandProcessor;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DialogWrapperPeerImpl extends DialogWrapperPeer {
  private static final Logger LOG = Logger.getInstance(DialogWrapperPeerImpl.class);

  private final DialogWrapper myWrapper;
  private final AbstractDialog myDialog;
  private final boolean myCanBeParent;
  private DesktopWindowManagerImpl myWindowManager;
  private final List<Runnable> myDisposeActions = new ArrayList<>();
  private Project myProject;

  protected DialogWrapperPeerImpl(@Nonnull DialogWrapper wrapper,
                                  @Nullable Project project,
                                  boolean canBeParent,
                                  @Nonnull DialogWrapper.IdeModalityType ideModalityType) {
    myWrapper = wrapper;
    myWindowManager = null;
    myCanBeParent = canBeParent;
    Application application = ApplicationManager.getApplication();
    if (application != null) {
      myWindowManager = (DesktopWindowManagerImpl)WindowManager.getInstance();
    }

    consulo.ui.Window window = null;
    if (myWindowManager != null) {

      if (project == null) {
        //noinspection deprecation
        project = DataManager.getInstance().getDataContext().getData(CommonDataKeys.PROJECT);
      }

      myProject = project;

      window = myWindowManager.suggestParentWindow(project);
      if (window == null) {
        consulo.ui.Window focusedWindow = myWindowManager.getMostRecentFocusedWindow();
        if (IdeFrameUtil.isRootIdeFrameWindow(focusedWindow)) {
          window = focusedWindow;
        }
      }
      if (window == null) {
        IdeFrame[] frames = myWindowManager.getAllProjectFrames();
        for (IdeFrame frame : frames) {
          if (frame.isActive()) {
            window = frame.getWindow();
            break;
          }
        }
      }
    }

    Window owner;
    if (window != null) {
      owner = TargetAWT.to(window);
    }
    else {
      if (!isHeadless()) {
        owner = JOptionPane.getRootFrame();
      }
      else {
        owner = null;
      }
    }

    myDialog = createDialog(owner, ideModalityType);
  }

  /**
   * Creates modal <code>DialogWrapper</code>. The currently active window will be the dialog's parent.
   *
   * @param project     parent window for the dialog will be calculated based on focused window for the
   *                    specified <code>project</code>. This parameter can be <code>null</code>. In this case parent window
   *                    will be suggested based on current focused window.
   * @param canBeParent specifies whether the dialog can be parent for other windows. This parameter is used
   *                    by <code>WindowManager</code>.
   */
  protected DialogWrapperPeerImpl(@Nonnull DialogWrapper wrapper, @Nullable Project project, boolean canBeParent) {
    this(wrapper, project, canBeParent, DialogWrapper.IdeModalityType.IDE);
  }

  protected DialogWrapperPeerImpl(@Nonnull DialogWrapper wrapper, boolean canBeParent) {
    this(wrapper, (Project)null, canBeParent);
  }

  @Override
  public boolean isRealDialog() {
    return true;
  }

  @Override
  public boolean isHeadless() {
    return isHeadlessEnv();
  }

  @Override
  public Object[] getCurrentModalEntities() {
    return LaterInvocator.getCurrentModalEntities();
  }

  /**
   * @param parent parent component which is used to calculate heavy weight window ancestor.
   *               <code>parent</code> cannot be <code>null</code> and must be showing.
   */
  protected DialogWrapperPeerImpl(@Nonnull DialogWrapper wrapper, @Nonnull Component parent, final boolean canBeParent) {
    myWrapper = wrapper;
    myCanBeParent = canBeParent;

    myWindowManager = null;
    Application application = ApplicationManager.getApplication();
    if (application != null) {
      myWindowManager = (DesktopWindowManagerImpl)WindowManager.getInstance();
    }

    OwnerOptional ownerOptional = OwnerOptional.fromComponent(parent);
    myDialog = createDialog(ownerOptional.get(), DialogWrapper.IdeModalityType.IDE);
  }

  public DialogWrapperPeerImpl(@Nonnull final DialogWrapper wrapper,
                               final Window owner,
                               final boolean canBeParent,
                               final DialogWrapper.IdeModalityType ideModalityType) {
    myWrapper = wrapper;
    myWindowManager = null;
    myCanBeParent = canBeParent;
    Application application = ApplicationManager.getApplication();
    if (application != null) {
      myWindowManager = (DesktopWindowManagerImpl)WindowManager.getInstance();
    }

    myDialog = createDialog(owner, ideModalityType);
  }

  /**
   * @see DialogWrapper#DialogWrapper(boolean, boolean)
   */
  @Deprecated
  public DialogWrapperPeerImpl(@Nonnull DialogWrapper wrapper, final boolean canBeParent, final boolean applicationModalIfPossible) {
    this(wrapper, null, canBeParent, applicationModalIfPossible);
  }

  @Deprecated
  public DialogWrapperPeerImpl(@Nonnull DialogWrapper wrapper,
                               final Window owner,
                               final boolean canBeParent,
                               final boolean applicationModalIfPossible) {
    this(wrapper,
         owner,
         canBeParent,
         applicationModalIfPossible ? DialogWrapper.IdeModalityType.IDE : DialogWrapper.IdeModalityType.PROJECT);
  }

  @Override
  public void setUndecorated(boolean undecorated) {
    myDialog.setUndecorated(undecorated);
  }

  @Override
  public void addMouseListener(MouseListener listener) {
    myDialog.addMouseListener(listener);
  }

  @Override
  public void addMouseListener(MouseMotionListener listener) {
    myDialog.addMouseMotionListener(listener);
  }

  @Override
  public void addKeyListener(KeyListener listener) {
    myDialog.addKeyListener(listener);
  }

  @Nonnull
  private AbstractDialog createDialog(@Nullable Window owner, @Nonnull DialogWrapper.IdeModalityType ideModalityType) {
    if (isHeadless()) {
      return new HeadlessDialog(myWrapper);
    }
    else {
      ActionCallback focused = new ActionCallback("DialogFocusedCallback");

      MyDialog dialog = new MyDialog(owner, myWrapper, myProject, focused);

      Dialog.ModalityType modalityType = ideModalityType.toAwtModality();
      if (!isHeadless()) {
        if (ideModalityType != DialogWrapper.IdeModalityType.MODELESS) {
          modalityType = DialogWrapper.IdeModalityType.IDE.toAwtModality();
          if (ModalityPerProjectEAPDescriptor.is()) {
            modalityType = ideModalityType.toAwtModality();
          }
        }
      }

      dialog.setModalityType(modalityType);

      return dialog;
    }
  }

  @Nonnull
  @Deprecated
  private AbstractDialog createDialog(@Nullable Window owner) {
    return createDialog(owner, DialogWrapper.IdeModalityType.IDE);
  }

  @Override
  public void toFront() {
    myDialog.toFront();
  }

  @Override
  public void toBack() {
    myDialog.toBack();
  }

  @Override
  @SuppressWarnings("SSBasedInspection")
  public void dispose() {
    LOG.assertTrue(EventQueue.isDispatchThread(), "Access is allowed from event dispatch thread only");
    for (Runnable runnable : myDisposeActions) {
      runnable.run();
    }
    myDisposeActions.clear();
    Runnable disposer = () -> {
      Disposer.dispose(myDialog);
      myProject = null;

      SwingUtilities.invokeLater(() -> {
        if (myDialog != null && myDialog.getRootPane() != null) {
          myDialog.remove(myDialog.getRootPane());
        }
      });
    };

    UIUtil.invokeLaterIfNeeded(disposer);
  }

  private boolean isProgressDialog() {
    return myWrapper.isModalProgress();
  }

  @Override
  @Nullable
  public Container getContentPane() {
    return getRootPane() != null ? myDialog.getContentPane() : null;
  }

  /**
   * @see javax.swing.JDialog#validate
   */
  @Override
  public void validate() {
    myDialog.validate();
  }

  /**
   * @see javax.swing.JDialog#repaint
   */
  @Override
  public void repaint() {
    myDialog.repaint();
  }

  @Override
  public Window getOwner() {
    return myDialog.getOwner();
  }

  @Override
  public Window getWindow() {
    return myDialog.getWindow();
  }

  @Override
  public JRootPane getRootPane() {
    return myDialog.getRootPane();
  }

  @Override
  public Dimension getSize() {
    return myDialog.getSize();
  }

  @Override
  public String getTitle() {
    return myDialog.getTitle();
  }

  /**
   * @see java.awt.Window#pack
   */
  @Override
  public void pack() {
    myDialog.pack();
  }

  @Override
  public void setAppIcons() {
    AppIconUtil.updateWindowIcon(getWindow());
  }

  @Override
  public Dimension getPreferredSize() {
    return myDialog.getPreferredSize();
  }

  @Override
  public void setModal(boolean modal) {
    myDialog.setModal(modal);
  }

  @Override
  public boolean isModal() {
    return myDialog.isModal();
  }

  @Override
  public boolean isVisible() {
    return myDialog.isVisible();
  }

  @Override
  public boolean isShowing() {
    return myDialog.isShowing();
  }

  @Override
  public void setSize(int width, int height) {
    myDialog.setSize(width, height);
  }

  @Override
  public void setTitle(String title) {
    myDialog.setTitle(title);
  }

  @Override
  public boolean isResizable() {
    return myDialog.isResizable();
  }

  @Override
  public void setResizable(boolean resizable) {
    myDialog.setResizable(resizable);
  }

  @Nonnull
  @Override
  public Point getLocation() {
    return myDialog.getLocation();
  }

  @Override
  public void setLocation(@Nonnull Point p) {
    myDialog.setLocation(p);
  }

  @Override
  public void setLocation(int x, int y) {
    myDialog.setLocation(x, y);
  }

  @Override
  public ActionCallback show() {
    LOG.assertTrue(EventQueue.isDispatchThread(), "Access is allowed from event dispatch thread only");

    LOG.assertTrue(EventQueue.isDispatchThread(), "Access is allowed from event dispatch thread only");
    final ActionCallback result = new ActionCallback();

    final AnCancelAction anCancelAction = new AnCancelAction();
    final JRootPane rootPane = getRootPane();
    SwingUIDecorator.apply(SwingUIDecorator::decorateWindowTitle, rootPane);
    anCancelAction.registerCustomShortcutSet(CommonShortcuts.ESCAPE, rootPane);
    myDisposeActions.add(() -> anCancelAction.unregisterCustomShortcutSet(rootPane));

    if (!myCanBeParent && myWindowManager != null) {
      myWindowManager.doNotSuggestAsParent(TargetAWT.from(myDialog.getWindow()));
    }

    final CommandProcessorEx commandProcessor =
      ApplicationManager.getApplication() != null ? (CommandProcessorEx)CommandProcessor.getInstance() : null;
    final boolean appStarted = commandProcessor != null;

    boolean changeModalityState = appStarted && myDialog.isModal() && !isProgressDialog(); // ProgressWindow starts a modality state itself
    Project project = myProject;

    if (changeModalityState) {
      commandProcessor.enterModal();
      if (ModalityPerProjectEAPDescriptor.is()) {
        LaterInvocator.enterModal(project, myDialog.getWindow());
      }
      else {
        LaterInvocator.enterModal(myDialog);
      }
    }

    if (appStarted) {
      hidePopupsIfNeeded();
    }

    try {
      myDialog.show();
    }
    finally {
      if (changeModalityState) {
        commandProcessor.leaveModal();
        if (ModalityPerProjectEAPDescriptor.is()) {
          LaterInvocator.leaveModal(project, myDialog.getWindow());
        }
        else {
          LaterInvocator.leaveModal(myDialog);
        }
      }

      myDialog.getFocusManager().doWhenFocusSettlesDown(result.createSetDoneRunnable());
    }

    return result;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> showAsync() {
    LOG.assertTrue(EventQueue.isDispatchThread(), "Access is allowed from event dispatch thread only");

    final AnCancelAction anCancelAction = new AnCancelAction();
    final JRootPane rootPane = getRootPane();
    SwingUIDecorator.apply(SwingUIDecorator::decorateWindowTitle, rootPane);
    anCancelAction.registerCustomShortcutSet(CommonShortcuts.ESCAPE, rootPane);
    myDisposeActions.add(() -> anCancelAction.unregisterCustomShortcutSet(rootPane));

    if (!myCanBeParent && myWindowManager != null) {
      myWindowManager.doNotSuggestAsParent(TargetAWT.from(myDialog.getWindow()));
    }

    final CommandProcessorEx commandProcessor =
      ApplicationManager.getApplication() != null ? (CommandProcessorEx)CommandProcessor.getInstance() : null;
    final boolean appStarted = commandProcessor != null;

    boolean changeModalityState = appStarted && myDialog.isModal() && !isProgressDialog(); // ProgressWindow starts a modality state itself
    Project project = myProject;

    UIAccess uiAccess = UIAccess.current();

    AsyncResult<Void> result = AsyncResult.undefined();

    uiAccess.give(() -> {
      if (changeModalityState) {
        commandProcessor.enterModal();

        if (ModalityPerProjectEAPDescriptor.is()) {
          LaterInvocator.enterModal(project, myDialog.getWindow());
        }
        else {
          LaterInvocator.enterModal(myDialog);
        }
      }

      if (appStarted) {
        hidePopupsIfNeeded();
      }

      try {
        myDialog.show();
      }
      finally {
        if (changeModalityState) {
          commandProcessor.leaveModal();

          if (ModalityPerProjectEAPDescriptor.is()) {
            LaterInvocator.leaveModal(project, myDialog.getWindow());
          }
          else {
            LaterInvocator.leaveModal(myDialog);
          }
        }
      }
    }).notify(result);

    return result;
  }

  //hopefully this whole code will go away
  private void hidePopupsIfNeeded() {
    if (!SystemInfo.isMac) return;

    StackingPopupDispatcher.getInstance().hidePersistentPopups();
    myDisposeActions.add(() -> StackingPopupDispatcher.getInstance().restorePersistentPopups());
  }

  private class AnCancelAction extends AnAction implements DumbAware {

    @Override
    public void update(AnActionEvent e) {
      Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      e.getPresentation().setEnabled(false);
      if (focusOwner instanceof JComponent && SpeedSearchBase.hasActiveSpeedSearch((JComponent)focusOwner)) {
        return;
      }

      if (StackingPopupDispatcher.getInstance().isPopupFocused()) return;
      JTree tree = UIUtil.getParentOfType(JTree.class, focusOwner);
      JTable table = UIUtil.getParentOfType(JTable.class, focusOwner);

      if (tree != null || table != null) {
        if (hasNoEditingTreesOrTablesUpward(focusOwner)) {
          e.getPresentation().setEnabled(true);
        }
      }
    }

    private boolean hasNoEditingTreesOrTablesUpward(Component comp) {
      while (comp != null) {
        if (isEditingTreeOrTable(comp)) return false;
        comp = comp.getParent();
      }
      return true;
    }

    private boolean isEditingTreeOrTable(Component comp) {
      if (comp instanceof JTree) {
        return ((JTree)comp).isEditing();
      }
      else if (comp instanceof JTable) {
        return ((JTable)comp).isEditing();
      }
      return false;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      myWrapper.doCancelAction(e.getInputEvent());
    }
  }


  private static class MyDialog extends JDialogAsUIWindow implements DialogWrapperDialog, DataProvider, Queryable, AbstractDialog {
    private final WeakReference<DialogWrapper> myDialogWrapper;

    /**
     * Initial size of the dialog. When the dialog is being closed and
     * current size of the dialog is not equals to the initial size then the
     * current (changed) size is stored in the {@code DimensionService}.
     */
    private Dimension myInitialSize;
    private String myDimensionServiceKey;
    private boolean myOpened = false;
    private boolean myActivated = false;

    private MyDialog.MyWindowListener myWindowListener;

    private final WeakReference<Project> myProject;
    private final ActionCallback myFocusedCallback;

    public MyDialog(Window owner, DialogWrapper dialogWrapper, Project project, @Nonnull ActionCallback focused) {
      super((consulo.ui.Window)TargetAWT.from(owner), null);
      myDialogWrapper = new WeakReference<>(dialogWrapper);
      myProject = project != null ? new WeakReference<>(project) : null;

      setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
        @Override
        protected boolean accept(Component aComponent) {
          if (UIUtil.isFocusProxy(aComponent)) return false;
          return super.accept(aComponent);
        }
      });

      myFocusedCallback = focused;

      final long typeAhead = getDialogWrapper().getTypeAheadTimeoutMs();

      setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      myWindowListener = new MyWindowListener();
      addWindowListener(myWindowListener);
    }

    @Override
    public JDialog getWindow() {
      return this;
    }

    @Override
    public void putInfo(@Nonnull Map<String, String> info) {
      info.put("dialog", getTitle());
    }

    @Override
    public DialogWrapper getDialogWrapper() {
      return myDialogWrapper.get();
    }

    @Override
    public void centerInParent() {
      setLocationRelativeTo(getOwner());
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      final DialogWrapper wrapper = myDialogWrapper.get();
      if (wrapper instanceof DataProvider) {
        return ((DataProvider)wrapper).getData(dataId);
      }
      if (wrapper instanceof TypeSafeDataProvider) {
        TypeSafeDataProviderAdapter adapter = new TypeSafeDataProviderAdapter((TypeSafeDataProvider)wrapper);
        return adapter.getData(dataId);
      }
      return null;
    }

    @Override
    public void setSize(int width, int height) {
      _setSizeForLocation(width, height, null);
    }

    private void _setSizeForLocation(int width, int height, @Nullable Point initial) {
      Point location = initial != null ? initial : getLocation();
      Rectangle rect = new Rectangle(location.x, location.y, width, height);
      ScreenUtil.fitToScreen(rect);
      if (initial != null || location.x != rect.x || location.y != rect.y) {
        setLocation(rect.x, rect.y);
      }

      super.setSize(rect.width, rect.height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
      Rectangle rect = new Rectangle(x, y, width, height);
      ScreenUtil.fitToScreen(rect);
      super.setBounds(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void setBounds(Rectangle r) {
      ScreenUtil.fitToScreen(r);
      super.setBounds(r);
    }

    @Nonnull
    @Override
    protected JRootPane createRootPane() {
      return new DialogRootPane();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void show() {
      final DialogWrapper dialogWrapper = getDialogWrapper();
      boolean isAutoAdjustable = dialogWrapper.isAutoAdjustable();
      Point location = null;
      if (isAutoAdjustable) {
        pack();

        Dimension packedSize = getSize();
        Dimension minSize = getMinimumSize();
        setSize(Math.max(packedSize.width, minSize.width), Math.max(packedSize.height, minSize.height));

        setSize((int)(getWidth() * dialogWrapper.getHorizontalStretch()), (int)(getHeight() * dialogWrapper.getVerticalStretch()));

        // Restore dialog's size and location

        myDimensionServiceKey = dialogWrapper.getDimensionKey();

        if (myDimensionServiceKey != null) {
          final Project projectGuess = DataManager.getInstance().getDataContext((Component)this).getData(CommonDataKeys.PROJECT);
          location = TargetAWT.to(getWindowStateService(projectGuess).getLocation(myDimensionServiceKey));
          Dimension size = TargetAWT.to(getWindowStateService(projectGuess).getSize(myDimensionServiceKey));
          if (size != null) {
            myInitialSize = new Dimension(size);
            _setSizeForLocation(myInitialSize.width, myInitialSize.height, location);
          }
        }

        if (myInitialSize == null) {
          myInitialSize = getSize();
        }
      }

      if (location == null) {
        location = dialogWrapper.getInitialLocation();
      }

      if (location != null) {
        setLocation(location);
      }
      else {
        setLocationRelativeTo(getOwner());
      }

      if (isAutoAdjustable) {
        final Rectangle bounds = getBounds();
        ScreenUtil.fitToScreen(bounds);
        setBounds(bounds);
      }

      if (Registry.is("actionSystem.fixLostTyping")) {
        final IdeEventQueue queue = IdeEventQueue.getInstance();
        if (queue != null) {
          queue.getKeyEventDispatcher().resetState();
        }

      }

      // Workaround for switching workspaces on dialog show
      if (SystemInfo.isMac && myProject != null && Registry.is("ide.mac.fix.dialog.showing") && !dialogWrapper.isModalProgress()) {
        final IdeFrame frame = WindowManager.getInstance().getIdeFrame(myProject.get());
        AppIcon.getInstance().requestFocus(frame.getWindow());
      }

      setBackground(UIUtil.getPanelBackground());

      final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
      if (app != null && !app.isLoaded() && DesktopSplash.BOUNDS != null) {
        final Point loc = getLocation();
        loc.y = DesktopSplash.BOUNDS.y + DesktopSplash.BOUNDS.height;
        setLocation(loc);
      }
      super.show();
    }

    @Nullable
    private Project getProject() {
      return SoftReference.dereference(myProject);
    }

    @Nonnull
    @Override
    public IdeFocusManager getFocusManager() {
      Project project = getProject();
      if (project != null && !project.isDisposed()) {
        return ProjectIdeFocusManager.getInstance(project);
      }
      else {
        return IdeFocusManager.findInstance();
      }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void hide() {
      super.hide();
    }

    @Override
    public void dispose() {
      if (isShowing()) {
        hide();
      }

      if (myWindowListener != null) {
        myWindowListener.saveSize();
        removeWindowListener(myWindowListener);
        myWindowListener = null;
      }

      DialogWrapper.cleanupWindowListeners(this);

      final BufferStrategy strategy = getBufferStrategy();
      if (strategy != null) {
        strategy.dispose();
      }
      super.dispose();

      removeAll();
      DialogWrapper.cleanupRootPane(rootPane);
      rootPane = null;

      // http://bugs.sun.com/view_bug.do?bug_id=6614056
      try {
        synchronized (getTreeLock()) {
          List<Dialog> list = DialogHacking.modalDialogs();
          list.remove(this);
        }
      }
      catch (final Exception ignored) {
      }
    }

    @Override
    public Component getMostRecentFocusOwner() {
      if (!myOpened) {
        final DialogWrapper wrapper = getDialogWrapper();
        if (wrapper != null) {
          JComponent toFocus = wrapper.getPreferredFocusedComponent();
          if (toFocus != null) {
            return toFocus;
          }
        }
      }
      return super.getMostRecentFocusOwner();
    }

    @Override
    public void paint(Graphics g) {
      if (!SystemInfo.isMac || UIUtil.isUnderAquaLookAndFeel()) {  // avoid rendering problems with non-aqua (alloy) LaFs under mac
        // actually, it's a bad idea to globally enable this for dialog graphics since renderers, for example, may not
        // inherit graphics so rendering hints won't be applied and trees or lists may render ugly.
        UISettingsUtil.setupAntialiasing(g);
      }

      super.paint(g);
    }

    @SuppressWarnings("SSBasedInspection")
    private class MyWindowListener extends WindowAdapter {
      @Override
      public void windowClosing(WindowEvent e) {
        DialogWrapper dialogWrapper = getDialogWrapper();
        if (dialogWrapper.shouldCloseOnCross()) {
          dialogWrapper.doCancelAction(e);
        }
      }

      @Override
      public void windowClosed(WindowEvent e) {
        saveSize();
      }

      public void saveSize() {
        if (myDimensionServiceKey != null && myInitialSize != null && myOpened) { // myInitialSize can be null only if dialog is disposed before first showing
          final Project projectGuess = DataManager.getInstance().getDataContext((Component)MyDialog.this).getData(CommonDataKeys.PROJECT);

          // Save location
          Point location = getLocation();
          getWindowStateService(projectGuess).putLocation(myDimensionServiceKey, TargetAWT.from(location));
          // Save size
          Dimension size = getSize();
          if (!myInitialSize.equals(size)) {
            getWindowStateService(projectGuess).putSize(myDimensionServiceKey, TargetAWT.from(size));
          }
          myOpened = false;
        }
      }

      @Override
      public void windowOpened(final WindowEvent e) {
        SwingUtilities.invokeLater(() -> {
          myOpened = true;
          final DialogWrapper activeWrapper = getActiveWrapper();
          for (JComponent c : UIUtil.uiTraverser(e.getWindow()).filter(JComponent.class)) {
            GraphicsUtil.setAntialiasingType(c, DesktopAntialiasingTypeUtil.getAntialiasingTypeForSwingComponent());
          }
          if (activeWrapper == null) {
            myFocusedCallback.setRejected();
          }

          final DialogWrapper wrapper = getActiveWrapper();
          if (wrapper == null && !myFocusedCallback.isProcessed()) {
            myFocusedCallback.setRejected();
            return;
          }

          if (myActivated) {
            return;
          }
          myActivated = true;
          JComponent toFocus = wrapper == null ? null : wrapper.getPreferredFocusedComponent();
          if (getRootPane() != null && toFocus == null) {
            toFocus = getRootPane().getDefaultButton();
          }

          if (getRootPane() != null) {
            IJSwingUtilities.moveMousePointerOn(getRootPane().getDefaultButton());
          }
          setupSelectionOnPreferredComponent(toFocus);

          if (toFocus != null) {
            if (isShowing() && (ApplicationManager.getApplication() == null || ApplicationManager.getApplication().isActive())) {
              toFocus.requestFocus();
            }
            else {
              toFocus.requestFocusInWindow();
            }
            notifyFocused(wrapper);
          }
          else {
            if (isShowing()) {
              notifyFocused(wrapper);
            }
          }
        });
      }

      private void notifyFocused(DialogWrapper wrapper) {
        myFocusedCallback.setDone();
      }

      private DialogWrapper getActiveWrapper() {
        DialogWrapper activeWrapper = getDialogWrapper();
        if (activeWrapper == null || !activeWrapper.isShowing()) {
          return null;
        }

        return activeWrapper;
      }
    }

    private class DialogRootPane extends JRootPane implements DataProvider {

      private final boolean myGlassPaneIsSet;

      private Dimension myLastMinimumSize;

      private DialogRootPane() {
        setGlassPane(new IdeGlassPaneImpl(this));
        myGlassPaneIsSet = true;
        putClientProperty("DIALOG_ROOT_PANE", true);
      }

      @Nonnull
      @Override
      protected JLayeredPane createLayeredPane() {
        JLayeredPane p = new JBLayeredPane();
        p.setName(this.getName() + ".layeredPane");
        return p;
      }

      @Override
      public void validate() {
        super.validate();
        DialogWrapper wrapper = myDialogWrapper.get();
        if (wrapper != null && wrapper.isAutoAdjustable()) {
          Window window = wrapper.getWindow();
          if (window != null) {
            Dimension size = getMinimumSize();
            if (!(size == null ? myLastMinimumSize == null : size.equals(myLastMinimumSize))) {
              // update window minimum size only if root pane minimum size is changed
              if (size == null) {
                myLastMinimumSize = null;
              }
              else {
                myLastMinimumSize = new Dimension(size);
                JBInsets.addTo(size, window.getInsets());
              }
              window.setMinimumSize(size);
            }
          }
        }
      }

      @Override
      public void setGlassPane(final Component glass) {
        if (myGlassPaneIsSet) {
          LOG.warn("Setting of glass pane for DialogWrapper is prohibited", new Exception());
          return;
        }

        super.setGlassPane(glass);
      }

      @Override
      public void setContentPane(Container contentPane) {
        super.setContentPane(contentPane);
        if (contentPane != null) {
          contentPane.addMouseMotionListener(new MouseMotionAdapter() {
          }); // listen to mouse motino events for a11y
        }
      }

      @Override
      public Object getData(@Nonnull Key<?> dataId) {
        final DialogWrapper wrapper = myDialogWrapper.get();
        return wrapper != null && PlatformDataKeys.UI_DISPOSABLE == dataId ? wrapper.getDisposable() : null;
      }
    }

    @Nonnull
    private static WindowStateService getWindowStateService(@Nullable Project project) {
      return project == null ? ApplicationWindowStateService.getInstance() : ProjectWindowStateService.getInstance(project);
    }
  }

  private static void setupSelectionOnPreferredComponent(final JComponent component) {
    if (component instanceof JTextField) {
      JTextField field = (JTextField)component;
      String text = field.getText();
      if (text != null && field.getClientProperty(HAVE_INITIAL_SELECTION) == null) {
        field.setSelectionStart(0);
        field.setSelectionEnd(text.length());
      }
    }
    else if (component instanceof JComboBox) {
      JComboBox combobox = (JComboBox)component;
      combobox.getEditor().selectAll();
    }
  }

  @Override
  public void setContentPane(JComponent content) {
    myDialog.setContentPane(content);
  }

  @Override
  public void centerInParent() {
    myDialog.centerInParent();
  }

  @Override
  public void setAutoRequestFocus(boolean b) {
    UIUtil.setAutoRequestFocus((JDialog)myDialog, b);
  }
}
