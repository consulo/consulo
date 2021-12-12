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

/*
 * @author max
 */
package com.intellij.openapi.wm.impl.welcomeScreen;

import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.UniqueNameBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.ClickListener;
import com.intellij.ui.ListUtil;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.speedSearch.ListWithFilter;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecentProjectPanel {
  private static final Logger LOG = Logger.getInstance(RecentProjectPanel.class);

  public static final String RECENT_PROJECTS_LABEL = "Recent Projects";
  protected final UniqueNameBuilder<ReopenProjectAction> myPathShortener;
  protected AnAction removeRecentProjectAction;
  protected FilePathChecker myChecker;
  private int myHoverIndex = -1;
  private final int closeButtonInset = JBUI.scale(7);
  private Image currentIcon = AllIcons.General.Remove;

  private final JPanel myCloseButtonForEditor = new JPanel() {
    {
      setPreferredSize(new Dimension(currentIcon.getWidth(), currentIcon.getHeight()));
      setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
      TargetAWT.to(currentIcon).paintIcon(this, g, 0, 0);
    }
  };

  private boolean rectInListCoordinatesContains(Rectangle listCellBounds, Point p) {

    int realCloseButtonInset = UIUtil.isJreHiDPI(myRootPanel) ? (int)(closeButtonInset * JBUI.sysScale(myRootPanel)) : closeButtonInset;

    Rectangle closeButtonRect =
            new Rectangle(myCloseButtonForEditor.getX() - realCloseButtonInset, myCloseButtonForEditor.getY() - realCloseButtonInset, myCloseButtonForEditor.getWidth() + realCloseButtonInset * 2,
                          myCloseButtonForEditor.getHeight() + realCloseButtonInset * 2);

    Rectangle rectInListCoordinates = new Rectangle(new Point(closeButtonRect.x + listCellBounds.x, closeButtonRect.y + listCellBounds.y), closeButtonRect.getSize());
    return rectInListCoordinates.contains(p);
  }

  protected final JPanel myRootPanel;
  protected final JBList<AnAction> myList;
  protected final JScrollPane myScrollPane;

  protected final JComponent myTargetComponent;

  public RecentProjectPanel(@Nonnull consulo.disposer.Disposable parentDisposable) {
    myRootPanel = new JPanel(new BorderLayout());

    final AnAction[] recentProjectActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false, isUseGroups());

    myPathShortener = new UniqueNameBuilder<>(SystemProperties.getUserHome(), File.separator, 40);

    Collection<String> pathsToCheck = new HashSet<>();
    for (AnAction action : recentProjectActions) {
      if (action instanceof ReopenProjectAction) {
        final ReopenProjectAction item = (ReopenProjectAction)action;

        myPathShortener.addPath(item, item.getProjectPath());

        pathsToCheck.add(item.getProjectPath());
      }
    }

    myChecker = new FilePathChecker(new Runnable() {
      @Override
      public void run() {
        if (myList.isShowing()) {
          myList.revalidate();
          myList.repaint();
        }
      }
    }, pathsToCheck);
    Disposer.register(parentDisposable, myChecker);

    myList = createList(recentProjectActions, getPreferredScrollableViewportSize());
    myList.setCellRenderer(createRenderer(myPathShortener));

    new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
        int selectedIndex = myList.getSelectedIndex();
        if (selectedIndex >= 0) {
          Rectangle cellBounds = myList.getCellBounds(selectedIndex, selectedIndex);
          if (cellBounds.contains(event.getPoint())) {
            AnAction selection = myList.getSelectedValue();
            if (selection != null) {
              AnAction selectedAction = performSelectedAction(event, selection);
              // remove action from list if needed
              if (selectedAction instanceof ReopenProjectAction) {
                if (((ReopenProjectAction)selectedAction).isRemoved()) {
                  ListUtil.removeSelectedItems(myList);
                }
              }
            }
          }
        }
        return true;
      }
    }.installOn(myList);

    myList.registerKeyboardAction(e -> {
      List<AnAction> selectedValues = myList.getSelectedValuesList();
      if (selectedValues != null) {
        for (AnAction selectedAction : selectedValues) {
          if (selectedAction != null) {
            InputEvent event = new KeyEvent(myList, KeyEvent.KEY_PRESSED, e.getWhen(), e.getModifiers(), KeyEvent.VK_ENTER, '\r');
            performSelectedAction(event, selectedAction);
          }
        }
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


    removeRecentProjectAction = new AnAction() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        List<AnAction> selection = myList.getSelectedValuesList();

        if (selection != null && !selection.isEmpty()) {
          final int rc = Messages.showOkCancelDialog(myRootPanel, "Remove '" + StringUtil.join(selection, action -> action.getTemplatePresentation().getText(), "'\n'") + "' from recent projects list?", "Remove Recent Project", Messages.getQuestionIcon());
          if (rc == Messages.OK) {
            for (Object projectAction : selection) {
              removeRecentProjectElement(projectAction);
            }
            ListUtil.removeSelectedItems(myList);
          }
        }
      }
    };

    removeRecentProjectAction.registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), myList, parentDisposable);

    addMouseMotionListener();

    myList.setSelectedIndex(0);

    myScrollPane = ScrollPaneFactory.createScrollPane(myList, true);

    myTargetComponent = recentProjectActions.length == 0 ? myList : ListWithFilter.wrap(myList, myScrollPane, o -> {
      if (o instanceof ReopenProjectAction) {
        ReopenProjectAction item = (ReopenProjectAction)o;
        String home = SystemProperties.getUserHome();
        String path = item.getProjectPath();
        if (FileUtil.startsWith(path, home)) {
          path = path.substring(home.length());
        }
        return item.getProjectName() + " " + path;
      }
      else if (o instanceof ProjectGroupActionGroup) {
        return ((ProjectGroupActionGroup)o).getGroup().getName();
      }
      return o.toString();
    });

    myRootPanel.add(myTargetComponent, BorderLayout.CENTER);
  }

  public JPanel getRootPanel() {
    return myRootPanel;
  }

  @Nonnull
  public JBList<AnAction> getList() {
    return myList;
  }

  @Nonnull
  private AnAction performSelectedAction(@Nonnull InputEvent event, AnAction selection) {
    String actionPlace = UIUtil.uiParents(myList, true).filter(FlatWelcomeFrame.class).isEmpty() ? ActionPlaces.POPUP : ActionPlaces.WELCOME_SCREEN;
    AnActionEvent actionEvent = AnActionEvent.createFromInputEvent(event, actionPlace, selection.getTemplatePresentation(), DataManager.getInstance().getDataContext(myList), false, false);
    ActionUtil.performActionDumbAwareWithCallbacks(selection, actionEvent, actionEvent.getDataContext());
    return selection;
  }

  protected static void removeRecentProjectElement(Object element) {
    final RecentProjectsManager manager = RecentProjectsManager.getInstance();
    if (element instanceof ReopenProjectAction) {
      manager.removePath(((ReopenProjectAction)element).getProjectPath());
    }
    else if (element instanceof ProjectGroupActionGroup) {
      final ProjectGroup group = ((ProjectGroupActionGroup)element).getGroup();
      for (String path : group.getProjects()) {
        manager.removePath(path);
      }
      manager.removeGroup(group);
    }
  }

  protected boolean isUseGroups() {
    return false;
  }

  protected Dimension getPreferredScrollableViewportSize() {
    return JBUI.size(250, 400);
  }

  protected void addMouseMotionListener() {

    MouseAdapter mouseAdapter = new MouseAdapter() {
      boolean myIsEngaged = false;

      @Override
      public void mouseMoved(MouseEvent e) {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
          IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myList);
        }
        if (myList.getSelectedIndices().length > 1) {
          return;
        }
        if (myIsEngaged && !UIUtil.isSelectionButtonDown(e) && !(focusOwner instanceof JRootPane)) {
          Point point = e.getPoint();
          int index = myList.locationToIndex(point);
          myList.setSelectedIndex(index);

          final Rectangle cellBounds = myList.getCellBounds(index, index);
          if (cellBounds != null && cellBounds.contains(point)) {
            myList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            myHoverIndex = index;
            myList.repaint(cellBounds);
          }
          else {
            myList.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            myHoverIndex = -1;
            myList.repaint();
          }
        }
        else {
          myIsEngaged = true;
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        myHoverIndex = -1;
        myList.repaint();
      }
    };

    myList.addMouseMotionListener(mouseAdapter);
    myList.addMouseListener(mouseAdapter);

  }

  protected boolean isPathValid(String path) {
    return myChecker == null || myChecker.isValid(path);
  }

  protected JBList<AnAction> createList(AnAction[] recentProjectActions, Dimension size) {
    return new MyList(size, recentProjectActions);
  }

  protected ListCellRenderer<AnAction> createRenderer(UniqueNameBuilder<ReopenProjectAction> pathShortener) {
    return new RecentProjectItemRenderer(pathShortener);
  }

  protected static class MyList extends JBList<AnAction> {
    private final Dimension mySize;

    protected MyList(Dimension size, @Nonnull AnAction... listData) {
      super(listData);
      mySize = size;
      setEmptyText("  No Project Open Yet  ");
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      getAccessibleContext().setAccessibleName(RECENT_PROJECTS_LABEL);
      final MouseHandler handler = new MouseHandler();
      addMouseListener(handler);
      addMouseMotionListener(handler);
    }

    public Rectangle getCloseIconRect(int index) {
      final Rectangle bounds = getCellBounds(index, index);
      Image icon = PlatformIconGroup.actionsMore();
      return new Rectangle(bounds.width - icon.getWidth() * 2, bounds.y, icon.getWidth() * 2, (int)bounds.getHeight());
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);

      // this is debug for getCloseIconRect()
      //int i = getSelectedIndex();
      //if(i == -1) {
      //  return;
      //}
      //g.setColor(Color.RED);
      //Rectangle closeIconRect = getCloseIconRect(i);
      //g.fillRect(closeIconRect.x, closeIconRect.y, closeIconRect.width, closeIconRect.height);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
      super.processMouseEvent(e);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return mySize == null ? super.getPreferredScrollableViewportSize() : mySize;
    }

    class MouseHandler extends MouseAdapter {
      @Override
      public void mouseReleased(MouseEvent e) {
        final Point point = e.getPoint();
        final MyList list = MyList.this;
        final int index = list.locationToIndex(point);
        if (index != -1) {
          if (getCloseIconRect(index).contains(point)) {
            e.consume();
            
            onActionClick(index, e);
          }
        }
      }
    }

    protected void onActionClick(int index, MouseEvent e) {
      final Object element = getModel().getElementAt(index);
      removeRecentProjectElement(element);
      ListUtil.removeSelectedItems(MyList.this);
    }
  }

  protected class RecentProjectItemRenderer extends JPanel implements ListCellRenderer<AnAction> {

    protected final JLabel myName = new JLabel();
    protected final JLabel myPath = new JLabel();
    protected boolean myHovered;
    protected JPanel myCloseThisItem = myCloseButtonForEditor;

    private final UniqueNameBuilder<ReopenProjectAction> myShortener;

    protected RecentProjectItemRenderer(UniqueNameBuilder<ReopenProjectAction> pathShortener) {
      super(new VerticalFlowLayout());
      myShortener = pathShortener;
      myPath.setFont(JBUI.Fonts.label(SystemInfo.isMac ? 10f : 11f));
      setFocusable(true);
      layoutComponents();
    }

    protected void layoutComponents() {
      add(myName);
      add(myPath);
    }

    protected Color getListBackground(boolean isSelected, boolean hasFocus) {
      return UIUtil.getListBackground(isSelected);
    }

    protected Color getListForeground(boolean isSelected, boolean hasFocus) {
      return UIUtil.getListForeground(isSelected);
    }

    @Override
    public Component getListCellRendererComponent(JList list, AnAction value, int index, boolean isSelected, boolean cellHasFocus) {
      myHovered = myHoverIndex == index;
      Color fore = getListForeground(isSelected, list.hasFocus());
      Color back = getListBackground(isSelected, list.hasFocus());

      myName.setForeground(fore);
      myPath.setForeground(isSelected ? fore : UIUtil.getInactiveTextColor());

      setBackground(back);

      if (value instanceof ReopenProjectAction) {
        ReopenProjectAction item = (ReopenProjectAction)value;
        myName.setText(getTitle2Text(item.getTemplatePresentation().getText(), myName, JBUI.scale(55)));
        myPath.setText(getTitle2Text(item.getProjectPath(), myPath, JBUI.scale(55)));
      }
      else if (value instanceof ProjectGroupActionGroup) {
        final ProjectGroupActionGroup group = (ProjectGroupActionGroup)value;
        myName.setText(group.getGroup().getName());
        myPath.setText("");
      }
      AccessibleContextUtil.setCombinedName(this, myName, " - ", myPath);
      AccessibleContextUtil.setCombinedDescription(this, myName, " - ", myPath);
      return this;
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      return new Dimension(Math.min(size.width, JBUI.scale(245)), size.height);
    }

    @Nonnull
    @Override
    public Dimension getSize() {
      return getPreferredSize();
    }
  }

  protected String getTitle2Text(String fullText, JComponent pathLabel, int leftOffset) {
    if (StringUtil.isEmpty(fullText)) return " ";

    fullText = FileUtil.getLocationRelativeToUserHome(fullText, false);

    try {
      FontMetrics fm = pathLabel.getFontMetrics(pathLabel.getFont());
      int maxWidth = myRootPanel.getWidth() - leftOffset;
      if (maxWidth > 0 && fm.stringWidth(fullText) > maxWidth) {
        int left = 1;
        int right = 1;
        int center = fullText.length() / 2;
        String s = fullText.substring(0, center - left) + "..." + fullText.substring(center + right);
        while (fm.stringWidth(s) > maxWidth) {
          if (left == right) {
            left++;
          }
          else {
            right++;
          }

          if (center - left < 0 || center + right >= fullText.length()) {
            return "";
          }
          s = fullText.substring(0, center - left) + "..." + fullText.substring(center + right);
        }
        return s;
      }
    }
    catch (Exception e) {
      LOG.error("Path label font: " + pathLabel.getFont());
      LOG.error("Panel width: " + myRootPanel.getWidth());
      LOG.error(e);
    }

    return fullText;
  }

  private static class FilePathChecker implements Disposable, ApplicationActivationListener, PowerSaveMode.Listener {
    private static final int MIN_AUTO_UPDATE_MILLIS = 2500;
    private ScheduledExecutorService myService = null;
    private final Set<String> myInvalidPaths = Collections.synchronizedSet(new HashSet<>());

    private final Runnable myCallback;
    private final Collection<String> myPaths;

    FilePathChecker(Runnable callback, Collection<String> paths) {
      myCallback = callback;
      myPaths = paths;
      MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(this);
      connection.subscribe(ApplicationActivationListener.TOPIC, this);
      connection.subscribe(PowerSaveMode.TOPIC, this);
      onAppStateChanged();
    }

    boolean isValid(String path) {
      return !myInvalidPaths.contains(path);
    }

    @Override
    public void applicationActivated(@Nonnull IdeFrame ideFrame) {
      onAppStateChanged();
    }

    @Override
    public void delayedApplicationDeactivated(@Nonnull IdeFrame ideFrame) {
      onAppStateChanged();
    }

    @Override
    public void applicationDeactivated(@Nonnull IdeFrame ideFrame) {
    }

    @Override
    public void powerSaveStateChanged() {
      onAppStateChanged();
    }

    private void onAppStateChanged() {
      boolean settingsAreOK = !PowerSaveMode.isEnabled();
      boolean everythingIsOK = settingsAreOK && ApplicationManager.getApplication().isActive();
      if (myService == null && everythingIsOK) {
        myService = AppExecutorUtil.createBoundedScheduledExecutorService("CheckRecentProjectPaths Service", 2);
        for (String path : myPaths) {
          scheduleCheck(path, 0);
        }
        ApplicationManager.getApplication().invokeLater(myCallback);
      }
      if (myService != null && !everythingIsOK) {
        if (!settingsAreOK) {
          myInvalidPaths.clear();
        }
        if (!myService.isShutdown()) {
          myService.shutdown();
          myService = null;
        }
        ApplicationManager.getApplication().invokeLater(myCallback);
      }
    }

    @Override
    public void dispose() {
      if (myService != null) {
        myService.shutdownNow();
      }
    }

    private void scheduleCheck(String path, long delay) {
      if (myService == null || myService.isShutdown()) return;

      myService.schedule(() -> {
        final long startTime = System.currentTimeMillis();
        boolean pathIsValid;
        try {
          pathIsValid = !RecentProjectsManagerBase.isFileSystemPath(path) || isPathAvailable(path);
        }
        catch (Exception e) {
          pathIsValid = false;
        }
        if (myInvalidPaths.contains(path) == pathIsValid) {
          if (pathIsValid) {
            myInvalidPaths.remove(path);
          }
          else {
            myInvalidPaths.add(path);
          }
          ApplicationManager.getApplication().invokeLater(myCallback);
        }
        scheduleCheck(path, Math.max(MIN_AUTO_UPDATE_MILLIS, 10 * (System.currentTimeMillis() - startTime)));
      }, delay, TimeUnit.MILLISECONDS);
    }
  }

  private static boolean isPathAvailable(String pathStr) {
    Path path = Paths.get(pathStr), pathRoot = path.getRoot();
    if (pathRoot == null) return false;
    if (SystemInfo.isWindows && pathRoot.toString().startsWith("\\\\")) return true;
    for (Path fsRoot : pathRoot.getFileSystem().getRootDirectories()) {
      if (pathRoot.equals(fsRoot)) return Files.exists(path);
    }
    return false;
  }
}
