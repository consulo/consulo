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
package consulo.desktop.awt.welcomeScreen;

import consulo.application.AllIcons;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.UniqueNameBuilder;
import consulo.application.util.UserHomeFileUtil;
import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.*;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.RecentProjectsWelcomeScreenActionBase;
import consulo.ui.ex.awt.ListWithFilter;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ProjectGroup;
import consulo.project.internal.RecentProjectsChecker;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * @author max
 */
public class RecentProjectPanel {
    private static final Logger LOG = Logger.getInstance(RecentProjectPanel.class);

    public static final String RECENT_PROJECTS_LABEL = "Recent Projects";
    protected final UniqueNameBuilder<ReopenProjectAction> myPathShortener;
    protected AnAction removeRecentProjectAction;
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

        int realCloseButtonInset =
            UIUtil.isJreHiDPI(myRootPanel) ? (int) (closeButtonInset * JBUI.sysScale(myRootPanel)) : closeButtonInset;

        Rectangle closeButtonRect = new Rectangle(
            myCloseButtonForEditor.getX() - realCloseButtonInset,
            myCloseButtonForEditor.getY() - realCloseButtonInset,
            myCloseButtonForEditor.getWidth() + realCloseButtonInset * 2,
            myCloseButtonForEditor.getHeight() + realCloseButtonInset * 2
        );

        Rectangle rectInListCoordinates =
            new Rectangle(new Point(closeButtonRect.x + listCellBounds.x, closeButtonRect.y + listCellBounds.y), closeButtonRect.getSize());
        return rectInListCoordinates.contains(p);
    }

    protected final JPanel myRootPanel;
    protected final JBList<AnAction> myList;
    protected final JScrollPane myScrollPane;

    protected final JComponent myTargetComponent;

    public RecentProjectPanel(consulo.disposer.Disposable parentDisposable) {
        myRootPanel = new JPanel(new BorderLayout());

        AnAction[] recentProjectActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false, isUseGroups());

        myPathShortener = new UniqueNameBuilder<>(Platform.current().user().homePath().toString(), File.separator, 40);

        Collection<String> pathsToCheck = new HashSet<>();
        for (AnAction action : recentProjectActions) {
            if (action instanceof ReopenProjectAction item) {
                myPathShortener.addPath(item, item.getProjectPath());

                pathsToCheck.add(item.getProjectPath());
            }
        }

        myList = createList(recentProjectActions, getPreferredScrollableViewportSize());
        myList.setCellRenderer(createRenderer(myPathShortener));

        Runnable checkerCallback = () -> {
            if (myList.isShowing()) {
                myList.revalidate();
                myList.repaint();
            }
        };

        RecentProjectsChecker checker = RecentProjectsChecker.getInstance();
        checker.addCallback(checkerCallback, pathsToCheck);
        Disposer.register(parentDisposable, () -> checker.removeCallback(checkerCallback));

        new ClickListener() {
            @Override
            public boolean onClick(MouseEvent event, int clickCount) {
                int selectedIndex = myList.getSelectedIndex();
                if (selectedIndex >= 0) {
                    Rectangle cellBounds = myList.getCellBounds(selectedIndex, selectedIndex);
                    if (cellBounds.contains(event.getPoint())) {
                        AnAction selection = myList.getSelectedValue();
                        if (selection != null) {
                            AnAction selectedAction = performSelectedAction(event, selection);
                            // remove action from list if needed
                            if (selectedAction instanceof ReopenProjectAction reopenProjectAction) {
                                if (reopenProjectAction.isRemoved()) {
                                    ListUtil.removeSelectedItems(myList);
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }.installOn(myList);

        myList.registerKeyboardAction(
            e -> {
                List<AnAction> selectedValues = myList.getSelectedValuesList();
                if (selectedValues != null) {
                    for (AnAction selectedAction : selectedValues) {
                        if (selectedAction != null) {
                            InputEvent event =
                                new KeyEvent(myList, KeyEvent.KEY_PRESSED, e.getWhen(), e.getModifiers(), KeyEvent.VK_ENTER, '\r');
                            performSelectedAction(event, selectedAction);
                        }
                    }
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        removeRecentProjectAction = new AnAction() {
            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                List<AnAction> selection = myList.getSelectedValuesList();

                if (selection != null && !selection.isEmpty()) {
                    int rc = Messages.showOkCancelDialog(
                        myRootPanel,
                        "Remove '" + StringUtil.join(
                            selection,
                            action -> action.getTemplatePresentation().getText(),
                            "'\n'"
                        ) + "' from recent projects list?",
                        "Remove Recent Project",
                        UIUtil.getQuestionIcon()
                    );
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
            if (o instanceof ReopenProjectAction item) {
                String home = Platform.current().user().homePath().toString();
                String path = item.getProjectPath();
                if (FileUtil.startsWith(path, home)) {
                    path = path.substring(home.length());
                }
                return item.getProjectName() + " " + path;
            }
            else if (o instanceof PopupProjectGroupActionGroup actionGroup) {
                return actionGroup.getGroup().getName();
            }
            return o.toString();
        });

        myRootPanel.add(myTargetComponent, BorderLayout.CENTER);

        ClientProperty.put(myList, UiDataProvider.KEY, sink -> {
            sink.set(RecentProjectsWelcomeScreenActionBase.RECENT_PROJECTS_LIST, myList);
        });
    }

    public JPanel getRootPanel() {
        return myRootPanel;
    }

    
    public JBList<AnAction> getList() {
        return myList;
    }

    
    private AnAction performSelectedAction(InputEvent event, AnAction selection) {
        String actionPlace =
            UIUtil.uiParents(myList, true).filter(FlatWelcomeFrame.class).isEmpty() ? ActionPlaces.POPUP : ActionPlaces.WELCOME_SCREEN;
        AnActionEvent actionEvent = AnActionEvent.createFromInputEvent(
            event,
            actionPlace,
            selection.getTemplatePresentation(),
            DataManager.getInstance().getDataContext(myList),
            false,
            false,
            DesktopAWTInputDetails.convert(myList, event)
        );
        ActionImplUtil.performActionDumbAwareWithCallbacks(selection, actionEvent, actionEvent.getDataContext());
        return selection;
    }

    protected static void removeRecentProjectElement(Object element) {
        RecentProjectsManager manager = RecentProjectsManager.getInstance();
        if (element instanceof ReopenProjectAction reopenProjectAction) {
            manager.removePath(reopenProjectAction.getProjectPath());
        }
        else if (element instanceof PopupProjectGroupActionGroup actionGroup) {
            ProjectGroup group = actionGroup.getGroup();
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

                    Rectangle cellBounds = myList.getCellBounds(index, index);
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
        return RecentProjectsChecker.getInstance().isValid(path);
    }

    protected JBList<AnAction> createList(AnAction[] recentProjectActions, Dimension size) {
        return new MyList(size, recentProjectActions);
    }

    protected ListCellRenderer<AnAction> createRenderer(UniqueNameBuilder<ReopenProjectAction> pathShortener) {
        return new RecentProjectItemRenderer(pathShortener);
    }

    protected static class MyList extends JBList<AnAction> {
        private final Dimension mySize;

        protected MyList(Dimension size, AnAction... listData) {
            super(listData);
            mySize = size;
            setEmptyText("  No Project Open Yet  ");
            setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            getAccessibleContext().setAccessibleName(RECENT_PROJECTS_LABEL);
            MouseHandler handler = new MouseHandler();
            addMouseListener(handler);
            addMouseMotionListener(handler);
        }

        public Rectangle getCloseIconRect(int index) {
            Rectangle bounds = getCellBounds(index, index);
            Image icon = PlatformIconGroup.actionsMorevertical();
            return new Rectangle(bounds.width - icon.getWidth() * 2, bounds.y, icon.getWidth() * 2, (int) bounds.getHeight());
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
                Point point = e.getPoint();
                MyList list = MyList.this;
                int index = list.locationToIndex(point);
                if (index != -1) {
                    if (getCloseIconRect(index).contains(point)) {
                        e.consume();

                        onActionClick(index, e);
                    }
                }
            }
        }

        protected void onActionClick(int index, MouseEvent e) {
            Object element = getModel().getElementAt(index);
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
            myPath.setFont(JBUI.Fonts.label(Platform.current().os().isMac() ? 10f : 11f));
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

            if (value instanceof ReopenProjectAction item) {
                String name = item.getTemplatePresentation().getText();
                String branch = RecentProjectsChecker.getInstance().getBranch(item.getProjectPath());
                if (!StringUtil.isEmptyOrSpaces(branch)) {
                    name += " [" + branch + "]";
                }
                myName.setText(getTitle2Text(name, myName, JBUI.scale(55)));
                myPath.setText(getTitle2Text(item.getProjectPath(), myPath, JBUI.scale(55)));
            }
            else if (value instanceof PopupProjectGroupActionGroup group) {
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

        
        @Override
        public Dimension getSize() {
            return getPreferredSize();
        }
    }

    protected String getTitle2Text(String fullText, JComponent pathLabel, int leftOffset) {
        if (StringUtil.isEmpty(fullText)) {
            return " ";
        }

        fullText = UserHomeFileUtil.getLocationRelativeToUserHome(fullText, false);

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
}
