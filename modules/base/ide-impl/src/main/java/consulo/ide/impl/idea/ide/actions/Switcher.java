// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.UserHomeFileUtil;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.component.util.Iconable;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.fileEditor.EditorTabPresentationUtil;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.fileEditor.impl.internal.EditorHistoryManagerImpl;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.IdeDocumentHistoryImpl;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.virtualFileSystem.VfsIconUtil;
import consulo.ui.ex.internal.QuickSearchComponent;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.CaptionPanel;
import consulo.ide.impl.idea.ui.ListActions;
import consulo.ide.impl.idea.ui.WindowMoveListener;
import consulo.ide.impl.idea.ui.speedSearch.NameFilteringListModel;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.internal.ToolWindowManagerEx;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.speedSearch.SpeedSearchObjectWithWeight;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupUpdateProcessorBase;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.style.StandardColors;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * @author Konstantin Bulenkov
 */
public class Switcher extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(Switcher.class);
    private static final Key<SwitcherPanel> SWITCHER_KEY = Key.create("SWITCHER_KEY");
    private static final String TOGGLE_CHECK_BOX_ACTION_ID = "SwitcherRecentEditedChangedToggleCheckBox";

    private static final int MINIMUM_HEIGHT = JBUIScale.scale(400);
    private static final int MINIMUM_WIDTH = JBUIScale.scale(500);

    private static final Color ON_MOUSE_OVER_BG_COLOR = new JBColor(new Color(231, 242, 249), new Color(77, 80, 84));

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.hasData(Project.KEY));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        SwitcherPanel switcher = SWITCHER_KEY.get(project);

        boolean isNewSwitcher = false;
        if (switcher != null && switcher.isPinnedMode()) {
            switcher.cancel();
            switcher = null;
        }
        if (switcher == null) {
            isNewSwitcher = true;
            switcher = createAndShowSwitcher(project, "Switcher", IdeActions.ACTION_SWITCHER, false, false);
            FeatureUsageTracker.getInstance().triggerFeatureUsed("switcher");
        }

        if (!switcher.isPinnedMode()) {
            if (e.getInputEvent() != null && e.getInputEvent().isShiftDown()) {
                switcher.goBack();
            }
            else if (isNewSwitcher && !FileEditorManagerEx.getInstanceEx(project).hasOpenedFile()) {
                switcher.files.setSelectedIndex(0);
            }
            else {
                switcher.goForward();
            }
        }
    }

    /**
     * @deprecated Please use {@link Switcher#createAndShowSwitcher(AnActionEvent, String, String, boolean, boolean)}
     */
    @Deprecated
    @Nullable
    @RequiredUIAccess
    public static SwitcherPanel createAndShowSwitcher(
        @Nonnull AnActionEvent e,
        @Nonnull String title,
        boolean pinned,
        @Nullable VirtualFile[] vFiles
    ) {
        return createAndShowSwitcher(e, title, "RecentFiles", pinned, vFiles != null);
    }

    @Nullable
    @RequiredUIAccess
    public static SwitcherPanel createAndShowSwitcher(
        @Nonnull AnActionEvent e,
        @Nonnull String title,
        @Nonnull String actionId,
        boolean onlyEdited,
        boolean pinned
    ) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return null;
        }
        SwitcherPanel switcher = SWITCHER_KEY.get(project);
        if (switcher != null) {
            boolean sameShortcut = Comparing.equal(switcher.myTitle, title);
            if (sameShortcut) {
                if (switcher.isCheckboxMode()
                    && e.getInputEvent() instanceof KeyEvent keyEvent
                    && KeymapUtil.isEventForAction(keyEvent, TOGGLE_CHECK_BOX_ACTION_ID)) {
                    switcher.toggleShowEditedFiles();
                }
                else {
                    switcher.goForward();
                }
                return null;
            }
            else if (switcher.isCheckboxMode()) {
                switcher.setShowOnlyEditedFiles(onlyEdited);
                return null;
            }
        }
        return createAndShowSwitcher(project, title, actionId, onlyEdited, pinned);
    }

    @Nonnull
    @RequiredUIAccess
    private static SwitcherPanel createAndShowSwitcher(
        @Nonnull Project project,
        @Nonnull String title,
        @Nonnull String actionId,
        boolean onlyEdited,
        boolean pinned
    ) {
        SwitcherPanel switcher = new SwitcherPanel(project, title, actionId, onlyEdited, pinned);
        SWITCHER_KEY.set(project, switcher);
        return switcher;
    }

    public static class ToggleCheckBoxAction extends DumbAwareAction implements DumbAware {
        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            SwitcherPanel switcherPanel = SWITCHER_KEY.get(project);
            if (switcherPanel != null) {
                switcherPanel.toggleShowEditedFiles();
            }
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            Project project = e.getData(Project.KEY);
            e.getPresentation().setEnabledAndVisible(SWITCHER_KEY.get(project) != null);
        }

        static boolean isEnabled() {
            return getActiveKeymapShortcuts(TOGGLE_CHECK_BOX_ACTION_ID).getShortcuts().length > 0;
        }
    }

    public static class SwitcherPanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, DataProvider, QuickSearchComponent {
        static final Object RECENT_LOCATIONS = new Object();
        final JBPopup myPopup;
        final JBList<Object> toolWindows;
        final JBList<FileInfo> files;
        final ToolWindowManager twManager;
        JBCheckBox myShowOnlyEditedFilesCheckBox;
        final JLabel pathLabel = new JLabel(" ");
        final CaptionPanel myTopPanel;
        final JPanel descriptions;
        final Project project;
        final boolean myPinned;
        final Map<String, ToolWindow> twShortcuts;
        final Alarm myAlarm;
        final SwitcherSpeedSearch mySpeedSearch;
        final String myTitle;
        final int myBaseModifier;
        private JBPopup myHint;

        @Nullable
        @Override
        public Object getData(@Nonnull Key dataId) {
            if (Project.KEY == dataId) {
                return this.project;
            }
            if (PlatformDataKeys.SELECTED_ITEM == dataId) {
                List list = getSelectedList().getSelectedValuesList();
                Object o = ContainerUtil.getOnlyItem(list);
                return o instanceof FileInfo fileInfo ? fileInfo.first : null;
            }
            if (VirtualFile.KEY_OF_ARRAY == dataId) {
                List list = getSelectedList().getSelectedValuesList();
                if (!list.isEmpty()) {
                    List<VirtualFile> vFiles = new ArrayList<>();
                    for (Object o : list) {
                        if (o instanceof FileInfo fileInfo) {
                            vFiles.add(fileInfo.first);
                        }
                    }
                    return vFiles.isEmpty() ? null : vFiles.toArray(VirtualFile.EMPTY_ARRAY);
                }
            }
            return null;
        }

        private class MyFocusTraversalPolicy extends FocusTraversalPolicy {
            @Override
            public Component getComponentAfter(Container aContainer, Component aComponent) {
                return aComponent == toolWindows ? files : toolWindows;
            }

            @Override
            public Component getComponentBefore(Container aContainer, Component aComponent) {
                return aComponent == toolWindows ? files : toolWindows;
            }

            @Override
            public Component getFirstComponent(Container aContainer) {
                return toolWindows;
            }

            @Override
            public Component getLastComponent(Container aContainer) {
                return files;
            }

            @Override
            public Component getDefaultComponent(Container aContainer) {
                return files;
            }
        }

        private static void exchangeSelectionState(JBList toClear, JBList toSelect) {
            if (toSelect.getModel().getSize() > 0) {
                int index = Math.min(toClear.getSelectedIndex(), toSelect.getModel().getSize() - 1);
                toSelect.setSelectedIndex(index);
                toSelect.ensureIndexIsVisible(index);
                toClear.clearSelection();
            }
        }

        private class MyToolWindowsListFocusListener extends FocusAdapter {
            @Override
            public void focusGained(FocusEvent e) {
                exchangeSelectionState(files, toolWindows);
            }
        }

        private class MyFilesListFocusListener extends FocusAdapter {
            @Override
            public void focusGained(FocusEvent e) {
                exchangeSelectionState(toolWindows, files);
            }
        }

        ClickListener myClickListener = new ClickListener() {
            @Override
            public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
                if (myPinned && (e.isControlDown() || e.isMetaDown() || e.isShiftDown())) {
                    return false;
                }
                Object source = e.getSource();
                if (source instanceof JList jList) {
                    if (jList.getSelectedIndex() == -1 && jList.getAnchorSelectionIndex() != -1) {
                        jList.setSelectedIndex(jList.getAnchorSelectionIndex());
                    }
                    if (jList.getSelectedIndex() != -1) {
                        navigate(e);
                    }
                }
                return true;
            }
        };

        @SuppressWarnings({"ConstantConditions"})
        @RequiredUIAccess
        SwitcherPanel(@Nonnull Project project, @Nonnull String title, @Nonnull String actionId, boolean onlyEdited, boolean pinned) {
            super(new BorderLayout());

            this.project = project;
            myTitle = title;
            myPinned = pinned;
            mySpeedSearch = pinned ? new SwitcherSpeedSearch(this) : null;

            setBorder(JBUI.Borders.empty());
            pathLabel.setHorizontalAlignment(SwingConstants.LEFT);

            Font font = pathLabel.getFont();
            pathLabel.setFont(font.deriveFont(Math.max(10f, font.getSize() - 4f)));

            descriptions = new JPanel(new BorderLayout());

            pathLabel.setBorder(JBCurrentTheme.Advertiser.border());
            pathLabel.setForeground(UIUtil.getLabelForeground());
            pathLabel.setOpaque(true);

            descriptions.setBorder(new CustomLineBorder(JBColor.border(), JBUI.insetsTop(1)));
            descriptions.add(pathLabel, BorderLayout.CENTER);
            twManager = ToolWindowManager.getInstance(project);
            CollectionListModel<Object> twModel = new CollectionListModel<>();
            List<ActivateToolWindowAction> actions = ToolWindowsGroup.getToolWindowActions(project, true);
            List<ToolWindow> windows = new ArrayList<>();
            for (ActivateToolWindowAction action : actions) {
                ToolWindow tw = twManager.getToolWindow(action.getToolWindowId());
                if (tw.isAvailable()) {
                    windows.add(tw);
                }
            }
            twShortcuts = createShortcuts(windows);
            Map<ToolWindow, String> map = consulo.ide.impl.idea.util.containers.ContainerUtil.reverseMap(twShortcuts);
            Collections.sort(windows, (o1, o2) -> StringUtil.compare(map.get(o1), map.get(o2), false));
            for (ToolWindow window : windows) {
                twModel.add(window);
            }
            twModel.add(RECENT_LOCATIONS);

            toolWindows = createList(twModel, getNamer(), mySpeedSearch, pinned);
            toolWindows.addFocusListener(new MyToolWindowsListFocusListener());
            toolWindows.setPreferredSize(new Dimension(JBUI.scale(200), toolWindows.getPreferredSize().height));

            toolWindows.setBorder(JBUI.Borders.empty(5, 5, 5, 20));
            toolWindows.setSelectionMode(pinned ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
            toolWindows.setCellRenderer(new SwitcherToolWindowsListRenderer(mySpeedSearch, map, myPinned, showEdited()) {
                @Nonnull
                @Override
                public Component getListCellRendererComponent(
                    @Nonnull JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean hasFocus
                ) {
                    JComponent renderer = (JComponent)super.getListCellRendererComponent(list, value, index, selected, selected);
                    if (selected) {
                        return renderer;
                    }
                    Color bgColor = list == mouseMoveSrc && index == mouseMoveListIndex
                        ? ON_MOUSE_OVER_BG_COLOR
                        : list.getBackground();
                    UIUtil.changeBackGround(renderer, bgColor);
                    return renderer;
                }
            });
            toolWindows.addKeyListener(this);
            ScrollingUtil.installActions(toolWindows);
            toolWindows.addMouseListener(this);
            toolWindows.addMouseMotionListener(this);
            ScrollingUtil.ensureSelectionExists(toolWindows);
            myClickListener.installOn(toolWindows);
            toolWindows.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(@Nonnull ListSelectionEvent e) {
                    if (!toolWindows.isSelectionEmpty() && !files.isSelectionEmpty()) {
                        files.clearSelection();
                    }
                }
            });

            Pair<List<FileInfo>, Integer> filesAndSelection =
                getFilesToShowAndSelectionIndex(project, collectFiles(project, onlyEdited), toolWindows.getModel().getSize(), pinned);
            int selectionIndex = filesAndSelection.getSecond();
            CollectionListModel<FileInfo> filesModel = new CollectionListModel<>();
            for (FileInfo editor : filesAndSelection.getFirst()) {
                filesModel.add(editor);
            }

            VirtualFilesRenderer filesRenderer = new VirtualFilesRenderer(this) {
                JPanel myPanel = new NonOpaquePanel(new BorderLayout());

                {
                    myPanel.setBackground(UIUtil.getListBackground());
                }

                @Nonnull
                @Override
                public Component getListCellRendererComponent(
                    @Nonnull JList<? extends FileInfo> list,
                    FileInfo value,
                    int index,
                    boolean selected,
                    boolean hasFocus
                ) {
                    Component c = super.getListCellRendererComponent(list, value, index, selected, selected);
                    myPanel.removeAll();
                    myPanel.add(c, BorderLayout.CENTER);

                    // Note: Name=name rendered in cell, Description=path to file, as displayed in bottom panel
                    myPanel.getAccessibleContext().setAccessibleName(c.getAccessibleContext().getAccessibleName());
                    VirtualFile file = value.first;
                    String presentableUrl = ObjectUtil.notNull(file.getParent(), file).getPresentableUrl();
                    String location = UserHomeFileUtil.getLocationRelativeToUserHome(presentableUrl);
                    myPanel.getAccessibleContext().setAccessibleDescription(location);
                    if (!selected && list == mouseMoveSrc && index == mouseMoveListIndex) {
                        setBackground(ON_MOUSE_OVER_BG_COLOR);
                    }
                    return myPanel;
                }

                @Override
                protected void customizeCellRenderer(
                    @Nonnull JList<? extends FileInfo> list,
                    FileInfo value,
                    int index,
                    boolean selected,
                    boolean hasFocus
                ) {
                    super.customizeCellRenderer(list, value, index, selected, hasFocus);
                }
            };

            ListSelectionListener filesSelectionListener = new ListSelectionListener() {
                @Nullable
                private String getTitle2Text(@Nullable String fullText) {
                    int labelWidth = pathLabel.getWidth();
                    if (fullText == null || fullText.length() == 0) {
                        return " ";
                    }
                    while (pathLabel.getFontMetrics(pathLabel.getFont()).stringWidth(fullText) > labelWidth) {
                        int sep = fullText.indexOf(File.separatorChar, 4);
                        if (sep < 0) {
                            return fullText;
                        }
                        fullText = "..." + fullText.substring(sep);
                    }

                    return fullText;
                }

                @Override
                public void valueChanged(@Nonnull ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        return;
                    }
                    FileInfo selectedInfo = ContainerUtil.getOnlyItem(files.getSelectedValuesList());
                    if (selectedInfo != null) {
                        String presentableUrl = ObjectUtil.notNull(selectedInfo.first.getParent(), selectedInfo.first).getPresentableUrl();
                        pathLabel.setText(getTitle2Text(UserHomeFileUtil.getLocationRelativeToUserHome(presentableUrl)));
                    }
                    else {
                        pathLabel.setText(" ");
                    }
                    PopupUpdateProcessorBase popupUpdater =
                        myHint == null || !myHint.isVisible() ? null : myHint.getUserData(PopupUpdateProcessorBase.class);
                    if (popupUpdater != null) {
                        DataContext dataContext = DataManager.getInstance().getDataContext(SwitcherPanel.this);
                        popupUpdater.updatePopup(dataContext.getData(PsiElement.KEY));
                    }
                }
            };
            files = createList(filesModel, FileInfo::getNameForRendering, mySpeedSearch, pinned);
            files.setSelectionMode(pinned ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
            files.getSelectionModel().addListSelectionListener(e -> {
                if (!files.isSelectionEmpty() && !toolWindows.isSelectionEmpty()) {
                    toolWindows.getSelectionModel().clearSelection();
                }
            });

            files.getSelectionModel().addListSelectionListener(filesSelectionListener);

            files.setCellRenderer(filesRenderer);
            files.setBorder(JBUI.Borders.empty(5));
            files.addKeyListener(this);
            ScrollingUtil.installActions(files);
            files.addMouseListener(this);
            files.addMouseMotionListener(this);
            files.addFocusListener(new MyFilesListFocusListener());
            myClickListener.installOn(files);
            ScrollingUtil.ensureSelectionExists(files);

            myShowOnlyEditedFilesCheckBox =
                new MyCheckBox(ToggleCheckBoxAction.isEnabled() ? TOGGLE_CHECK_BOX_ACTION_ID : actionId, onlyEdited);
            myTopPanel = createTopPanel(
                myShowOnlyEditedFilesCheckBox,
                isCheckboxMode() ? IdeLocalize.titlePopupRecentFiles().get() : title,
                pinned
            );

            UIUtil.putClientProperty(this, CaptionPanel.KEY, myTopPanel);

            if (isCheckboxMode()) {
                myShowOnlyEditedFilesCheckBox.addActionListener(e -> setShowOnlyEditedFiles(myShowOnlyEditedFilesCheckBox.isSelected()));
                myShowOnlyEditedFilesCheckBox.addActionListener(e -> toolWindows.repaint());
            }
            else {
                myShowOnlyEditedFilesCheckBox.setEnabled(false);
                myShowOnlyEditedFilesCheckBox.setVisible(false);
            }

            this.add(myTopPanel, BorderLayout.NORTH);
            this.add(toolWindows, BorderLayout.WEST);
            if (filesModel.getSize() > 0) {
                files.setAlignmentY(1f);
                JScrollPane pane = ScrollPaneFactory.createScrollPane(files, true);
                pane.setPreferredSize(new Dimension(
                    Math.max(
                        myTopPanel.getPreferredSize().width - toolWindows.getPreferredSize().width,
                        files.getPreferredSize().width
                    ),
                    20 * 20
                ));
                pane.setBorder(JBUI.Borders.customLineLeft(JBColor.border()));
                this.add(pane, BorderLayout.CENTER);
                if (selectionIndex > -1) {
                    files.setSelectedIndex(selectionIndex);
                }
            }
            this.add(descriptions, BorderLayout.SOUTH);

            ShortcutSet shortcutSet = ActionManager.getInstance().getAction(IdeActions.ACTION_SWITCHER).getShortcutSet();
            int modifiers = getModifiers(shortcutSet);
            boolean isAlt = (modifiers & Event.ALT_MASK) != 0;
            myBaseModifier = isAlt ? VK_ALT : VK_CONTROL;
            files.addKeyListener(ArrayUtil.getLastElement(getKeyListeners()));
            toolWindows.addKeyListener(ArrayUtil.getLastElement(getKeyListeners()));
            KeymapUtil.reassignAction(toolWindows, getKeyStroke(VK_UP, 0), getKeyStroke(VK_UP, CTRL_DOWN_MASK), WHEN_FOCUSED, false);
            KeymapUtil.reassignAction(toolWindows, getKeyStroke(VK_DOWN, 0), getKeyStroke(VK_DOWN, CTRL_DOWN_MASK), WHEN_FOCUSED, false);
            KeymapUtil.reassignAction(files, getKeyStroke(VK_UP, 0), getKeyStroke(VK_UP, CTRL_DOWN_MASK), WHEN_FOCUSED, false);
            KeymapUtil.reassignAction(files, getKeyStroke(VK_DOWN, 0), getKeyStroke(VK_DOWN, CTRL_DOWN_MASK), WHEN_FOCUSED, false);

            myPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(this, filesModel.getSize() > 0 ? files : toolWindows)
                .setResizable(pinned)
                .setModalContext(false)
                .setFocusable(true)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(true)
                .setCancelOnOtherWindowOpen(true)
                .setCancelOnClickOutside(true)
                .setMovable(pinned)
                .setMinSize(new Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT))
                .setDimensionServiceKey(pinned ? project : null, pinned ? "SwitcherDM" : null, false)
                .setCancelKeyEnabled(false)
                .setCancelCallback(() -> {
                    Container popupFocusAncestor = getPopupFocusAncestor();
                    if (popupFocusAncestor != null) {
                        popupFocusAncestor.setFocusTraversalPolicy(null);
                    }
                    SWITCHER_KEY.set(project, null);
                    return true;
                })
                .createPopup();

            if (isPinnedMode()) {
                new DumbAwareAction(LocalizeValue.empty(), LocalizeValue.empty(), null) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(@Nonnull AnActionEvent e) {
                        if (mySpeedSearch != null && mySpeedSearch.isPopupActive()) {
                            mySpeedSearch.hidePopup();
                            Object[] elements = mySpeedSearch.getAllElements();
                            if (elements != null && elements.length > 0) {
                                mySpeedSearch.selectElement(elements[0], "");
                            }
                        }
                        else {
                            myPopup.cancel();
                        }
                    }
                }.registerCustomShortcutSet(CustomShortcutSet.fromString("ESCAPE"), this, myPopup);
            }
            Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (window == null) {
                window = WindowManager.getInstance().getFrame(project);
            }
            myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, myPopup);
            IdeEventQueueProxy.getInstance().closeAllPopups(false);
            myPopup.showInCenterOf(window);

            Container popupFocusAncestor = getPopupFocusAncestor();
            popupFocusAncestor.setFocusTraversalPolicy(new MyFocusTraversalPolicy());

            addFocusTraversalKeys(popupFocusAncestor, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, "RIGHT");
            addFocusTraversalKeys(popupFocusAncestor, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, "LEFT");
            addFocusTraversalKeys(popupFocusAncestor, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, "control RIGHT");
            addFocusTraversalKeys(popupFocusAncestor, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, "control LEFT");

            fromListToList(toolWindows, files);
            fromListToList(files, toolWindows);

            IdeEventQueueProxy.getInstance().addPostprocessor(
                event -> event instanceof KeyEvent keyEvent && keyEvent(keyEvent),
                myPopup
            );
        }

        @Nonnull
        private Supplier<Boolean> showEdited() {
            return () -> myShowOnlyEditedFilesCheckBox != null && myShowOnlyEditedFilesCheckBox.isSelected();
        }

        @Nonnull
        private Function<? super Object, String> getNamer() {
            return value -> {
                if (value instanceof ToolWindow toolWindow) {
                    return toolWindow.getDisplayName().get();
                }
                if (value == RECENT_LOCATIONS) {
                    return getRecentLocationsLabel(showEdited());
                }

                throw new IllegalStateException();
            };
        }

        static String getRecentLocationsLabel(@Nonnull Supplier<Boolean> showEdited) {
            return showEdited.get()
                ? IdeLocalize.recentLocationsChangedLocations().get()
                : IdeLocalize.recentLocationsPopupTitle().get();
        }

        @RequiredUIAccess
        private boolean keyEvent(KeyEvent event) {
            if (isPinnedMode()) {
                return false;
            }
            if (event.getID() == KEY_RELEASED && event.getKeyCode() == myBaseModifier) {
                event.consume();
                Application application = Application.get();
                application.invokeLater(() -> navigate(null), application.getCurrentModalityState());
                return true;
            }
            if (event.getID() == KEY_PRESSED) {
                ToolWindow tw = twShortcuts.get(String.valueOf((char)event.getKeyCode()));
                if (tw != null) {
                    event.consume();
                    myPopup.closeOk(null);
                    tw.activate(null, true, true);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void registerHint(@Nonnull JBPopup h) {
            if (myHint != null && myHint.isVisible() && myHint != h) {
                myHint.cancel();
            }
            myHint = h;
        }

        @Override
        public void unregisterHint() {
            myHint = null;
        }

        @Nonnull
        private static <T> JBList<T> createList(
            CollectionListModel<T> baseModel,
            Function<? super T, String> namer,
            SwitcherSpeedSearch speedSearch,
            boolean pinned
        ) {
            ListModel<T> listModel;
            if (pinned) {
                listModel = new NameFilteringListModel<>(
                    baseModel,
                    namer,
                    s -> !speedSearch.isPopupActive()
                        || StringUtil.isEmpty(speedSearch.getEnteredPrefix())
                        || speedSearch.getComparator().matchingFragments(speedSearch.getEnteredPrefix(), s) != null,
                    () -> StringUtil.notNullize(speedSearch.getEnteredPrefix())
                );
            }
            else {
                listModel = baseModel;
            }
            return new JBList<>(listModel);
        }

        private static void fromListToList(JBList from, JBList to) {
            AbstractAction action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    to.requestFocus();
                }
            };
            ActionMap map = from.getActionMap();
            map.put(ListActions.Left.ID, action);
            map.put(ListActions.Right.ID, action);
        }

        private Container getPopupFocusAncestor() {
            return myPopup.isDisposed() ? null : myPopup.getContent().getFocusCycleRootAncestor();
        }

        @Nonnull
        static List<VirtualFile> collectFiles(@Nonnull Project project, boolean onlyEdited) {
            return onlyEdited ? Arrays.asList(IdeDocumentHistory.getInstance(project).getChangedFiles()) : getRecentFiles(project);
        }

        @Nonnull
        @RequiredUIAccess
        static Pair<List<FileInfo>, Integer> getFilesToShowAndSelectionIndex(
            @Nonnull Project project,
            @Nonnull List<VirtualFile> filesForInit,
            int toolWindowsCount,
            boolean pinned
        ) {
            int selectionIndex = -1;
            FileEditorManagerImpl editorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(project);
            ArrayList<FileInfo> filesData = new ArrayList<>();
            ArrayList<FileInfo> editors = new ArrayList<>();
            if (!pinned) {
                for (Pair<VirtualFile, FileEditorWindow> pair : editorManager.getSelectionHistory()) {
                    editors.add(new FileInfo(pair.first, pair.second, project));
                }
            }
            if (editors.size() < 2) {
                int maxFiles = Math.max(editors.size(), filesForInit.size());
                int minIndex = pinned ? 0 : (filesForInit.size() - Math.min(toolWindowsCount, maxFiles));
                boolean firstRecentMarked = false;
                List<VirtualFile> selectedFiles = Arrays.asList(editorManager.getSelectedFiles());
                FileEditorWindow currentWindow = editorManager.getCurrentWindow();
                VirtualFile currentFile = currentWindow != null ? currentWindow.getSelectedFile() : null;
                for (int i = filesForInit.size() - 1; i >= minIndex; i--) {
                    if (pinned && UISettings.getInstance().getEditorTabPlacement() != UISettings.PLACEMENT_EDITOR_TAB_NONE
                        && selectedFiles.contains(filesForInit.get(i))) {
                        continue;
                    }

                    FileInfo info = new FileInfo(filesForInit.get(i), null, project);
                    boolean add = true;
                    if (pinned) {
                        for (FileInfo fileInfo : filesData) {
                            if (fileInfo.first.equals(info.first)) {
                                add = false;
                                break;
                            }
                        }
                    }
                    if (add) {
                        filesData.add(info);
                        if (!firstRecentMarked && !info.first.equals(currentFile)) {
                            selectionIndex = filesData.size() - 1;
                            firstRecentMarked = true;
                        }
                    }
                }
                //if (editors.size() == 1) selectionIndex++;
                if (editors.size() == 1 && (filesData.isEmpty() || !editors.get(0).getFirst().equals(filesData.get(0).getFirst()))) {
                    filesData.add(0, editors.get(0));
                }
            }
            else {
                for (int i = 0; i < Math.min(30, editors.size()); i++) {
                    filesData.add(editors.get(i));
                }
            }

            return Pair.create(filesData, selectionIndex);
        }

        @Nonnull
        private static CaptionPanel createTopPanel(
            @Nonnull JBCheckBox showOnlyEditedFilesCheckBox,
            @Nonnull String title,
            boolean isMovable
        ) {
            CaptionPanel topPanel = new CaptionPanel();
            JBLabel titleLabel = new JBLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
            topPanel.add(titleLabel, BorderLayout.WEST);
            topPanel.add(showOnlyEditedFilesCheckBox, BorderLayout.EAST);

            Dimension size = topPanel.getPreferredSize();
            size.height = JBUIScale.scale(29);
            size.width = titleLabel.getPreferredSize().width + showOnlyEditedFilesCheckBox.getPreferredSize().width + JBUIScale.scale(50);
            topPanel.setPreferredSize(size);
            topPanel.setMinimumSize(size);
            topPanel.setBorder(JBUI.Borders.empty(5, 8));

            if (isMovable) {
                WindowMoveListener moveListener = new WindowMoveListener(topPanel);
                topPanel.addMouseListener(moveListener);
                topPanel.addMouseMotionListener(moveListener);
            }

            return topPanel;
        }

        private static void addFocusTraversalKeys(Container focusCycleRoot, int focusTraversalType, String keyStroke) {
            Set<AWTKeyStroke> focusTraversalKeySet = focusCycleRoot.getFocusTraversalKeys(focusTraversalType);

            Set<AWTKeyStroke> set = new HashSet<>(focusTraversalKeySet);
            set.add(getKeyStroke(keyStroke));
            focusCycleRoot.setFocusTraversalKeys(focusTraversalType, set);
        }

        @Nonnull
        private static List<VirtualFile> getRecentFiles(@Nonnull Project project) {
            List<VirtualFile> recentFiles = EditorHistoryManagerImpl.getInstance(project).getFileList();
            VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();

            Set<VirtualFile> recentFilesSet = new HashSet<>(recentFiles);
            Set<VirtualFile> openFilesSet = new HashSet<>(Arrays.asList(openFiles));

            // Add missing FileEditor tabs right after the last one, that is available via "Recent Files"
            int index = 0;
            for (int i = 0; i < recentFiles.size(); i++) {
                if (openFilesSet.contains(recentFiles.get(i))) {
                    index = i;
                    break;
                }
            }

            List<VirtualFile> result = new ArrayList<>(recentFiles);
            result.addAll(index, ContainerUtil.filter(openFiles, it -> !recentFilesSet.contains(it)));
            return result;
        }

        @Nonnull
        @RequiredUIAccess
        private static Map<String, ToolWindow> createShortcuts(@Nonnull List<ToolWindow> windows) {
            Map<String, ToolWindow> keymap = new HashMap<>(windows.size());
            List<ToolWindow> otherTW = new ArrayList<>();
            for (ToolWindow window : windows) {
                int index = ActivateToolWindowAction.getMnemonicForToolWindow(window.getId());
                if (index >= '0' && index <= '9') {
                    keymap.put(getIndexShortcut(index - '0'), window);
                }
                else {
                    otherTW.add(window);
                }
            }
            int i = 0;
            for (ToolWindow window : otherTW) {
                String bestShortcut = getSmartShortcut(window, keymap);
                if (bestShortcut != null) {
                    keymap.put(bestShortcut, window);
                    continue;
                }

                while (keymap.get(getIndexShortcut(i)) != null) {
                    i++;
                }
                keymap.put(getIndexShortcut(i), window);
                i++;
            }
            return keymap;
        }

        @Nullable
        @RequiredUIAccess
        private static String getSmartShortcut(ToolWindow window, Map<String, ToolWindow> keymap) {
            String title = window.getDisplayName().getValue();
            if (StringUtil.isEmpty(title)) {
                return null;
            }
            for (int i = 0; i < title.length(); i++) {
                char c = title.charAt(i);
                if (Character.isUpperCase(c)) {
                    String shortcut = String.valueOf(c);
                    if (keymap.get(shortcut) == null) {
                        return shortcut;
                    }
                }
            }
            return null;
        }

        private static String getIndexShortcut(int index) {
            return StringUtil.toUpperCase(Integer.toString(index, index + 1));
        }

        private static int getModifiers(@Nullable ShortcutSet shortcutSet) {
            return shortcutSet != null
                && shortcutSet.getShortcuts().length != 0
                && shortcutSet.getShortcuts()[0] instanceof KeyboardShortcut keyboardShortcut
                ? keyboardShortcut.getFirstKeyStroke().getModifiers()
                : Event.CTRL_MASK;
        }

        @Override
        public void keyTyped(@Nonnull KeyEvent e) {
        }

        @Override
        public void keyReleased(@Nonnull KeyEvent e) {
            boolean ctrl = e.getKeyCode() == myBaseModifier;
            if ((ctrl && isAutoHide())) {
                navigate(e);
            }
        }

        KeyEvent lastEvent;

        @Override
        @RequiredUIAccess
        public void keyPressed(@Nonnull KeyEvent e) {
            if (mySpeedSearch != null && mySpeedSearch.isPopupActive() || lastEvent == e) {
                return;
            }
            lastEvent = e;
            switch (e.getKeyCode()) {
                case VK_DELETE:
                case VK_BACK_SPACE: // Mac users
                case VK_Q:
                    closeTabOrToolWindow();
                    break;
                case VK_ESCAPE:
                    cancel();
                    break;
                case VK_ENTER:
                    if (mySpeedSearch == null) {
                        navigate(e);
                    }
                    break;
            }
        }

        @RequiredUIAccess
        private void closeTabOrToolWindow() {
            JBList selectedList = getSelectedList();
            int[] selected = selectedList.getSelectedIndices();
            Arrays.sort(selected);
            int selectedIndex = 0;
            for (int i = selected.length - 1; i >= 0; i--) {
                selectedIndex = selected[i];
                Object value = selectedList.getModel().getElementAt(selectedIndex);
                if (value instanceof FileInfo info) {
                    VirtualFile virtualFile = info.first;
                    FileEditorManagerImpl editorManager = (FileEditorManagerImpl)FileEditorManager.getInstance(project);
                    JList jList = getSelectedList();
                    FileEditorWindow wnd = findAppropriateWindow(info);
                    if (wnd == null) {
                        editorManager.closeFile(virtualFile, false, false);
                    }
                    else {
                        editorManager.closeFile(virtualFile, wnd, false);
                    }

                    IdeFocusManager focusManager = ProjectIdeFocusManager.getInstance(project);
                    myAlarm.cancelAllRequests();
                    myAlarm.addRequest(() -> {
                        JComponent focusTarget = selectedList;
                        if (selectedList.getModel().getSize() == 0) {
                            focusTarget = selectedList == files ? toolWindows : files;
                        }
                        focusManager.requestFocus(focusTarget, true);
                    }, 300);
                    if (jList.getModel().getSize() == 1) {
                        removeElementAt(jList, selectedIndex);
                        this.remove(jList);
                        Dimension size = toolWindows.getSize();
                        myPopup.setSize(new Dimension(size.width, myPopup.getSize().height));
                    }
                    else {
                        removeElementAt(jList, selectedIndex);
                        jList.setSize(jList.getPreferredSize());
                    }
                    if (isPinnedMode()) {
                        EditorHistoryManagerImpl.getInstance(project).removeFile(virtualFile);
                    }
                }
                else if (value instanceof ToolWindow toolWindow) {
                    if (twManager instanceof ToolWindowManagerEx manager) {
                        manager.hideToolWindow(toolWindow.getId(), false, false);
                    }
                    else {
                        toolWindow.hide(null);
                    }
                }
            }
            pack();
            myPopup.getContent().revalidate();
            myPopup.getContent().repaint();
            if (getSelectedList().getModel().getSize() > selectedIndex) {
                getSelectedList().setSelectedIndex(selectedIndex);
                getSelectedList().ensureIndexIsVisible(selectedIndex);
            }
        }

        private static void removeElementAt(@Nonnull JList<?> jList, int index) {
            ListUtil.removeItem(jList.getModel(), index);
        }

        private void pack() {
            this.setSize(this.getPreferredSize());
            JRootPane rootPane = SwingUtilities.getRootPane(this);
            Container container = this;
            do {
                container = container.getParent();
                container.setSize(container.getPreferredSize());
            }
            while (container != rootPane);
            container.getParent().setSize(container.getPreferredSize());
        }

        private boolean isFilesSelected() {
            return getSelectedList() == files;
        }

        private boolean isFilesVisible() {
            return files.getModel().getSize() > 0;
        }

        private void cancel() {
            myPopup.cancel();
        }

        public void go(boolean forward) {
            JBList selected = getSelectedList();
            JList list = selected;
            int index = list.getSelectedIndex();
            if (forward) {
                index++;
            }
            else {
                index--;
            }
            if ((forward && index >= list.getModel().getSize()) || (!forward && index < 0)) {
                if (isFilesVisible()) {
                    list = isFilesSelected() ? toolWindows : files;
                }
                index = forward ? 0 : list.getModel().getSize() - 1;
            }
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
            if (selected != list) {
                IdeFocusManager.findInstanceByComponent(list).requestFocus(list, true);
            }
        }

        public void goForward() {
            go(true);
        }

        public void goBack() {
            go(false);
        }

        public JBList<?> getSelectedList() {
            return getSelectedList(files);
        }

        @Nullable
        JBList getSelectedList(@Nullable JBList preferable) {
            return files.hasFocus() ? files : toolWindows.hasFocus() ? toolWindows : preferable;
        }

        boolean isCheckboxMode() {
            return isPinnedMode();
        }

        void toggleShowEditedFiles() {
            myShowOnlyEditedFilesCheckBox.doClick();
        }

        @RequiredUIAccess
        void setShowOnlyEditedFiles(boolean onlyEdited) {
            if (myShowOnlyEditedFilesCheckBox.isSelected() != onlyEdited) {
                myShowOnlyEditedFilesCheckBox.setSelected(onlyEdited);
            }

            boolean listWasSelected = files.getSelectedIndex() != -1;

            Pair<List<FileInfo>, Integer> filesAndSelection = getFilesToShowAndSelectionIndex(
                project,
                collectFiles(project, onlyEdited),
                toolWindows.getModel().getSize(),
                isPinnedMode()
            );
            int selectionIndex = filesAndSelection.getSecond();

            ListModel<FileInfo> model = files.getModel();
            files.clearSelection(); // workaround JDK-7108280
            ListUtil.removeAllItems(model);
            ListUtil.addAllItems(model, filesAndSelection.getFirst());

            if (selectionIndex > -1 && listWasSelected) {
                files.setSelectedIndex(selectionIndex);
            }
            files.revalidate();
            files.repaint();
        }

        void navigate(InputEvent e) {
            boolean openInNewWindow = e != null && e.isShiftDown() && e instanceof KeyEvent keyEvent && keyEvent.getKeyCode() == VK_ENTER;
            List<?> values = getSelectedList().getSelectedValuesList();
            String searchQuery = mySpeedSearch != null ? mySpeedSearch.getEnteredPrefix() : null;
            myPopup.cancel(null);
            if (values.isEmpty()) {
                tryToOpenFileSearch(e, searchQuery);
            }
            else if (values.get(0) == RECENT_LOCATIONS) {
                RecentLocationsAction.showPopup(project, myShowOnlyEditedFilesCheckBox.isSelected());

            }
            else if (values.get(0) instanceof ToolWindow toolWindow) {
                ProjectIdeFocusManager.getInstance(project).doWhenFocusSettlesDown(
                    () -> toolWindow.activate(null, true, true),
                    Application.get().getCurrentModalityState()
                );
            }
            else {
                ProjectIdeFocusManager.getInstance(project).doWhenFocusSettlesDown(
                    () -> {
                        FileEditorManagerImpl manager = (FileEditorManagerImpl)FileEditorManager.getInstance(project);
                        for (Object value : values) {
                            if (value instanceof FileInfo info) {
                                VirtualFile file = info.first;
                                if (openInNewWindow) {
                                    manager.openFileInNewWindow(file);
                                }
                                else if (info.second != null) {
                                    FileEditorWindow wnd = findAppropriateWindow(info);
                                    if (wnd != null) {
                                        manager.openFileImpl2(UIAccess.current(), wnd, file, true);
                                        manager.addSelectionRecord(file, wnd);
                                    }
                                }
                                else {
                                    UISettings settings = UISettings.getInstance().getState();
                                    boolean oldValue = settings.getReuseNotModifiedTabs();
                                    settings.setReuseNotModifiedTabs(false);
                                    manager.openFile(file, true, true);
                                    if (oldValue) {
                                        CommandProcessor.getInstance().newCommand()
                                            .project(project)
                                            .run(() -> settings.setReuseNotModifiedTabs(true));
                                    }
                                }
                            }
                        }
                    },
                    Application.get().getCurrentModalityState()
                );
            }
        }

        private void tryToOpenFileSearch(InputEvent e, String fileName) {
            AnAction gotoFile = ActionManager.getInstance().getAction("GotoFile");
            if (gotoFile == null || StringUtil.isEmpty(fileName)) {
                return;
            }
            myPopup.cancel();
            Application.get().invokeLater(
                () -> DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(fromFocus -> {
                    DataContext dataContext =
                        SimpleDataContext.getSimpleContext(PlatformDataKeys.PREDEFINED_TEXT, fileName, fromFocus);
                    AnActionEvent event = AnActionEvent.createFromAnAction(gotoFile, e, ActionPlaces.EDITOR_POPUP, dataContext);
                    gotoFile.actionPerformed(event);
                }),
                Application.get().getCurrentModalityState()
            );
        }

        @Nullable
        private static FileEditorWindow findAppropriateWindow(@Nonnull FileInfo info) {
            if (info.second == null) {
                return null;
            }
            FileEditorWindow[] windows = info.second.getOwner().getWindows();
            return ArrayUtil.contains(info.second, windows) ? info.second : windows.length > 0 ? windows[0] : null;
        }

        @Override
        public void mouseClicked(@Nonnull MouseEvent e) {
        }

        private boolean mouseMovedFirstTime = true;
        private JList mouseMoveSrc = null;
        private int mouseMoveListIndex = -1;

        @Override
        public void mouseMoved(@Nonnull MouseEvent e) {
            if (mouseMovedFirstTime) {
                mouseMovedFirstTime = false;
                return;
            }
            Object source = e.getSource();
            boolean changed = false;
            if (source instanceof JList list) {
                int index = list.locationToIndex(e.getPoint());
                if (0 <= index && index < list.getModel().getSize()) {
                    mouseMoveSrc = list;
                    mouseMoveListIndex = index;
                    changed = true;
                }
            }
            if (!changed) {
                mouseMoveSrc = null;
                mouseMoveListIndex = -1;
            }

            repaintLists();
        }

        private void repaintLists() {
            toolWindows.repaint();
            files.repaint();
        }

        @Override
        public void mousePressed(@Nonnull MouseEvent e) {
        }

        @Override
        public void mouseReleased(@Nonnull MouseEvent e) {
        }

        @Override
        public void mouseEntered(@Nonnull MouseEvent e) {
        }

        @Override
        public void mouseExited(@Nonnull MouseEvent e) {
            mouseMoveSrc = null;
            mouseMoveListIndex = -1;
            repaintLists();
        }

        @Override
        public void mouseDragged(@Nonnull MouseEvent e) {
        }

        private static class SwitcherSpeedSearch extends SpeedSearchBase<SwitcherPanel> {

            SwitcherSpeedSearch(@Nonnull SwitcherPanel switcher) {
                super(switcher);
                setComparator(new SpeedSearchComparator(false, true));
            }

            @Override
            protected void processKeyEvent(@Nonnull KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == VK_ENTER) {
                    myComponent.navigate(e);
                    e.consume();
                    return;
                }
                if (keyCode == VK_LEFT || keyCode == VK_RIGHT) {
                    return;
                }
                super.processKeyEvent(e);
            }

            @Override
            protected int getSelectedIndex() {
                return myComponent.isFilesSelected() ? myComponent.files.getSelectedIndex() : myComponent.files.getModel()
                    .getSize() + myComponent.toolWindows.getSelectedIndex();
            }

            @Nonnull
            @Override
            protected Object[] getAllElements() {
                ListModel filesModel = myComponent.files.getModel();
                Object[] files = new Object[filesModel.getSize()];
                for (int i = 0; i < files.length; i++) {
                    files[i] = filesModel.getElementAt(i);
                }

                ListModel twModel = myComponent.toolWindows.getModel();
                Object[] toolWindows = new Object[twModel.getSize()];
                for (int i = 0; i < toolWindows.length; i++) {
                    toolWindows[i] = twModel.getElementAt(i);
                }

                Object[] elements = new Object[files.length + toolWindows.length];
                System.arraycopy(files, 0, elements, 0, files.length);
                System.arraycopy(toolWindows, 0, elements, files.length, toolWindows.length);

                return elements;
            }

            @Override
            @RequiredUIAccess
            protected String getElementText(Object element) {
                if (element instanceof ToolWindow toolWindow) {
                    return toolWindow.getDisplayName().getValue();
                }
                else if (element instanceof FileInfo fileInfo) {
                    return fileInfo.getNameForRendering();
                }
                return "";
            }

            @Override
            protected void selectElement(Object element, String selectedText) {
                if (element instanceof FileInfo) {
                    if (!myComponent.toolWindows.isSelectionEmpty()) {
                        myComponent.toolWindows.clearSelection();
                    }
                    myComponent.files.clearSelection();
                    myComponent.files.setSelectedValue(element, true);
                    myComponent.files.requestFocusInWindow();
                }
                else {
                    if (!myComponent.files.isSelectionEmpty()) {
                        myComponent.files.clearSelection();
                    }
                    myComponent.toolWindows.clearSelection();
                    myComponent.toolWindows.setSelectedValue(element, true);
                    myComponent.toolWindows.requestFocusInWindow();
                }
            }

            @Nullable
            @Override
            protected Object findElement(String s) {
                List<SpeedSearchObjectWithWeight> elements = SpeedSearchObjectWithWeight.findElement(s, this);
                return elements.isEmpty() ? null : elements.get(0).node;
            }

            @Override
            protected void onSearchFieldUpdated(String pattern) {
                if (myComponent.project.isDisposed()) {
                    myComponent.myPopup.cancel();
                    return;
                }
                ((NameFilteringListModel)myComponent.files.getModel()).refilter();
                ((NameFilteringListModel)myComponent.toolWindows.getModel()).refilter();
                if (myComponent.files.getModel().getSize() + myComponent.toolWindows.getModel().getSize() == 0) {
                    myComponent.toolWindows.getEmptyText().setText(LocalizeValue.empty());
                    myComponent.files.getEmptyText().setText("Press 'Enter' to search in Project");
                }
                else {
                    myComponent.files.getEmptyText().setText(StatusText.DEFAULT_EMPTY_LOC_TEXT);
                    myComponent.toolWindows.getEmptyText().setText(StatusText.DEFAULT_EMPTY_LOC_TEXT);
                }
                refreshSelection();
            }
        }

        public boolean isAutoHide() {
            return !myPinned;
        }

        public boolean isPinnedMode() {
            return myPinned;
        }
    }

    private static class MyCheckBox extends JBCheckBox {
        private MyCheckBox(@Nonnull String actionId, boolean selected) {
            super(layoutText(actionId), selected);
            setOpaque(false);
            setFocusable(false);
        }

        private static String layoutText(@Nonnull String actionId) {
            ShortcutSet shortcuts = getActiveKeymapShortcuts(actionId);
            return "<html>" +
                IdeLocalize.recentFilesCheckboxLabel() +
                " <font color=\"" +
                RecentLocationsAction.getShortcutHexColor() +
                "\">" +
                KeymapUtil.getShortcutsText(shortcuts.getShortcuts()) +
                "</font>" +
                "</html>";
        }
    }

    private static class VirtualFilesRenderer extends ColoredListCellRenderer<FileInfo> {
        private final SwitcherPanel mySwitcherPanel;
        boolean open;

        VirtualFilesRenderer(@Nonnull SwitcherPanel switcherPanel) {
            mySwitcherPanel = switcherPanel;
        }

        @Override
        protected void customizeCellRenderer(
            @Nonnull JList<? extends FileInfo> list,
            FileInfo value,
            int index,
            boolean selected,
            boolean hasFocus
        ) {
            setBorder(JBCurrentTheme.listCellBorderFull());
            Project project = mySwitcherPanel.project;
            VirtualFile virtualFile = value.getFirst();
            String renderedName = value.getNameForRendering();
            setIcon(VfsIconUtil.getIcon(virtualFile, Iconable.ICON_FLAG_READ_STATUS, project));

            FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(virtualFile);
            open = FileEditorManager.getInstance(project).isFileOpen(virtualFile);

            boolean hasProblem = WolfTheProblemSolver.getInstance(project).isProblemFile(virtualFile);
            TextAttributes attributes = new TextAttributes(
                fileStatus.getColor(),
                null,
                hasProblem ? StandardColors.RED : null,
                EffectType.WAVE_UNDERSCORE,
                Font.PLAIN
            );
            append(renderedName, TextAttributesUtil.fromTextAttributes(attributes));

            // calc color the same way editor tabs do this, i.e. including EPs
            ColorValue color = EditorTabPresentationUtil.getFileBackgroundColor(project, virtualFile);

            if (!selected && color != null) {
                setBackground(TargetAWT.to(color));
            }
            SpeedSearchUtil.applySpeedSearchHighlighting(mySwitcherPanel, this, false, selected);

            IdeDocumentHistoryImpl.appendTimestamp(project, this, virtualFile);
        }
    }

    static class FileInfo extends Pair<VirtualFile, FileEditorWindow> {
        private final Project myProject;
        private String myNameForRendering;

        FileInfo(VirtualFile first, FileEditorWindow second, Project project) {
            super(first, second);
            myProject = project;
        }

        String getNameForRendering() {
            if (myNameForRendering == null) {
                // Recently changed files would also be taken into account (not only open 'visible' files)
                myNameForRendering = EditorTabPresentationUtil.getUniqueEditorTabTitle(myProject, first);
            }
            return myNameForRendering;
        }
    }
}
