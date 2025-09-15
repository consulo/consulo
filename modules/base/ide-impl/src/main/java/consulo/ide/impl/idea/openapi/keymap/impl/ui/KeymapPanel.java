/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.configurable.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.actionSystem.AbbreviationManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickListsManager;
import consulo.ide.impl.idea.openapi.keymap.KeyboardSettingsExternalizable;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.ide.impl.idea.openapi.keymap.impl.*;
import consulo.ide.impl.idea.packageDependencies.ui.TreeExpansionMonitor;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.internal.KeyMapSetting;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.ImageEffects;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

@ExtensionImpl
public class KeymapPanel implements SearchableConfigurable, Configurable.NoScroll, KeymapListener, ApplicationConfigurable {
    private final PropertyChangeListener myAncestor;

    private final DefaultComboBoxModel myKeymapListModel = new DefaultComboBoxModel();
    @Nonnull
    private final Provider<KeyMapSetting> myKeyMapSettingProvider;

    private KeymapImpl mySelectedKeymap;

    private Button myCopyButton;
    private Button myDeleteButton;
    private Button myResetToDefault;
    private CheckBox myNonEnglishKeyboardSupportOption;

    private JLabel myBaseKeymapLabel;

    private ActionsTree myActionsTree;
    private FilterComponent myFilterComponent;
    private JBPopup myPopup = null;
    private TreeExpansionMonitor myTreeExpansionMonitor;

    private boolean myQuickListsModified = false;
    private QuickList[] myQuickLists = QuickListsManager.getInstance().getAllQuickLists();

    private JComboBox<Keymap> myKeymapList;
    private JPanel myRootPanel;
    private Disposable myUIDisposable;

    private ThreeStateCheckBox myUseUnicodeCharactersInShortcutsBox;
    private JCheckBox myDoublePressShortcutsBox;

    @Inject
    public KeymapPanel(@Nonnull Provider<KeyMapSetting> keyMapSettingProvider) {
        myKeyMapSettingProvider = keyMapSettingProvider;
        myAncestor = evt -> {
            if (evt.getPropertyName().equals("ancestor")
                && evt.getNewValue() != null && evt.getOldValue() == null
                && myQuickListsModified) {
                processCurrentKeymapChanged(getCurrentQuickListIds());
                myQuickListsModified = false;
            }
        };
    }

    @Override
    public void quickListRenamed(QuickList oldQuickList, QuickList newQuickList) {
        for (Keymap keymap : getAllKeymaps()) {
            KeymapImpl impl = (KeymapImpl)keymap;

            String actionId = oldQuickList.getActionId();
            String newActionId = newQuickList.getActionId();

            Shortcut[] shortcuts = impl.getShortcuts(actionId);

            if (shortcuts != null) {
                for (Shortcut shortcut : shortcuts) {
                    impl.removeShortcut(actionId, shortcut);
                    impl.addShortcut(newActionId, shortcut);
                }
            }
        }

        myQuickListsModified = true;
    }

    private JPanel createKeymapRootPanel() {
        myUIDisposable = Disposable.newDisposable();

        JPanel panel = new JPanel(new BorderLayout());
        JPanel keymapPanel = new JPanel(new BorderLayout());
        keymapPanel.add(createKeymapListPanel(), BorderLayout.NORTH);
        keymapPanel.add(createKeymapSettingsPanel(), BorderLayout.CENTER);
        panel.add(keymapPanel, BorderLayout.CENTER);

        panel.addPropertyChangeListener(myAncestor);

        return panel;
    }

    private JPanel createKeymapListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        myKeymapList = new ComboBox<>(myKeymapListModel);
        myKeymapList.setEditor(new MyEditor());
        myKeymapList.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(
                @Nonnull JList<? extends Keymap> list,
                Keymap keymap,
                int index,
                boolean selected,
                boolean hasFocus
            ) {
                if (keymap != null) {
                    String name = keymap.getPresentableName();
                    if (name == null) {
                        name = KeyMapLocalize.keymapNonamePresentableName().get();
                    }
                    append(name);
                }
            }
        });
        JLabel keymapLabel = new JLabel(KeyMapLocalize.keymapsBorderFactoryTitle().get());
        keymapLabel.setLabelFor(myKeymapList);
        panel.add(
            keymapLabel,
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0)
        );
        panel.add(
            myKeymapList,
            new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insetsLeft(4), 0, 0)
        );

        panel.add(
            createKeymapButtonsPanel(),
            new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0)
        );
        myKeymapList.addActionListener(e -> {
            if (myKeymapListModel.getSelectedItem() != mySelectedKeymap) {
                processCurrentKeymapChanged(getCurrentQuickListIds());
            }
        });
        panel.add(
            createKeymapNamePanel(),
            new GridBagConstraints(3, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insetsLeft(10), 0, 0)
        );
        return panel;
    }

    @Override
    public Runnable enableSearch(String option) {
        return () -> showOption(option);
    }

    @Override
    @RequiredUIAccess
    public void processCurrentKeymapChanged(QuickList[] ids) {
        myQuickLists = ids;
        myCopyButton.setEnabled(false);
        myDeleteButton.setEnabled(false);
        myResetToDefault.setEnabled(false);
        KeymapImpl selectedKeymap = getSelectedKeymap();
        mySelectedKeymap = selectedKeymap;
        if (selectedKeymap == null) {
            myActionsTree.reset(new KeymapImpl(), getCurrentQuickListIds());
            return;
        }
        myKeymapList.setEditable(mySelectedKeymap.canModify());

        myCopyButton.setEnabled(true);
        myBaseKeymapLabel.setText("");
        Keymap parent = mySelectedKeymap.getParent();
        if (parent != null && mySelectedKeymap.canModify()) {
            myBaseKeymapLabel.setText(KeyMapLocalize.basedOnKeymapLabel(parent.getPresentableName()).get());
            if (mySelectedKeymap.canModify() && mySelectedKeymap.getOwnActionIds().length > 0) {
                myResetToDefault.setEnabled(true);
            }
        }
        if (mySelectedKeymap.canModify()) {
            myDeleteButton.setEnabled(true);
        }

        myActionsTree.reset(mySelectedKeymap, getCurrentQuickListIds());
    }

    private KeymapImpl getSelectedKeymap() {
        return (KeymapImpl)myKeymapList.getSelectedItem();
    }

    List<Keymap> getAllKeymaps() {
        ListModel model = myKeymapList.getModel();
        List<Keymap> result = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            result.add((Keymap)model.getElementAt(i));
        }
        return result;
    }

    private JPanel createKeymapButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        panel.setLayout(new GridBagLayout());
        myCopyButton = Button.create(KeyMapLocalize.copyKeymapButton());
        GridBagConstraints gc = new GridBagConstraints(
            GridBagConstraints.RELATIVE,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            JBUI.insetsLeft(5),
            0,
            0
        );
        panel.add(TargetAWT.to(myCopyButton), gc);
        myResetToDefault = Button.create(CommonLocalize.buttonReset());
        panel.add(TargetAWT.to(myResetToDefault), gc);
        myDeleteButton = Button.create(KeyMapLocalize.deleteKeymapButton());
        gc.weightx = 1;
        panel.add(TargetAWT.to(myDeleteButton), gc);

        FocusableFrame ideFrame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
        if (ideFrame != null && KeyboardSettingsExternalizable.isSupportedKeyboardLayout(ideFrame.getComponent())) {
            String displayLanguage = ideFrame.getComponent().getInputContext().getLocale().getDisplayLanguage();
            myNonEnglishKeyboardSupportOption = CheckBox.create(displayLanguage + " " + KeyMapLocalize.useNonEnglishKeyboardLayoutSupport().get());
            myNonEnglishKeyboardSupportOption.setValue(
                KeyboardSettingsExternalizable.getInstance().isNonEnglishKeyboardSupportEnabled()
            );
            
            myNonEnglishKeyboardSupportOption.addValueListener(event -> {
                KeyboardSettingsExternalizable.getInstance()
                    .setNonEnglishKeyboardSupportEnabled(myNonEnglishKeyboardSupportOption.getValue());
            });
            panel.add(TargetAWT.to(myNonEnglishKeyboardSupportOption), gc);
        }

        myCopyButton.addClickListener(e -> copyKeymap());

        myResetToDefault.addClickListener(e -> resetKeymap());

        myDeleteButton.addClickListener(e -> deleteKeymap());

        return panel;
    }

    private JPanel createKeymapSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        myActionsTree = new ActionsTree(myUIDisposable, () -> {
            switch (myUseUnicodeCharactersInShortcutsBox.getState()) {
                case SELECTED:
                    return true;
                case NOT_SELECTED:
                    return false;
                default:
                    return Platform.current().os().isMac();
            }
        });

        panel.add(createToolbarPanel(), BorderLayout.NORTH);
        panel.add(myActionsTree.getComponent(), BorderLayout.CENTER);

        myTreeExpansionMonitor = TreeExpansionMonitor.install(myActionsTree.getTree());

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                editSelection(e);
                return true;
            }
        }.installOn(myActionsTree.getTree());


        myActionsTree.getTree().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    editSelection(e);
                    e.consume();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    editSelection(e);
                    e.consume();
                }
            }
        });

        panel.add(createKeymapBottomPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createKeymapBottomPanel() {
        JPanel bottomPanel = new JPanel(new VerticalFlowLayout());

        myUseUnicodeCharactersInShortcutsBox = new ThreeStateCheckBox("Use unicode characters instead text in shortcuts (⇧ instead SHIFT)");

        myUseUnicodeCharactersInShortcutsBox.addActionListener(e -> {
            myActionsTree.updateTree();
        });

        bottomPanel.add(myUseUnicodeCharactersInShortcutsBox);

        myDoublePressShortcutsBox = new JCheckBox("Enable double modifier key shortcuts (Shift-Shift for Search Everywhere, Ctrl-Ctrl for Run Anything)");

        bottomPanel.add(myDoublePressShortcutsBox);

        return bottomPanel;
    }

    private JPanel createToolbarPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        DefaultActionGroup group = new DefaultActionGroup();
        ActionToolbar firstToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        firstToolbar.setTargetComponent(panel);
        JComponent toolbar = firstToolbar.getComponent();
        CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
        TreeExpander treeExpander = new TreeExpander() {
            @Override
            public void expandAll() {
                TreeUtil.expandAll(myActionsTree.getTree());
            }

            @Override
            public boolean canExpand() {
                return true;
            }

            @Override
            public void collapseAll() {
                TreeUtil.collapseAll(myActionsTree.getTree(), 0);
            }

            @Override
            public boolean canCollapse() {
                return true;
            }
        };
        group.add(commonActionsManager.createExpandAllAction(treeExpander, myActionsTree.getTree()));
        group.add(commonActionsManager.createCollapseAllAction(treeExpander, myActionsTree.getTree()));

        group.add(new DumbAwareAction(
            KeyMapLocalize.editShortcutActionText(),
            KeyMapLocalize.editShortcutActionDescription(),
            ImageEffects.layered(PlatformIconGroup.actionsEdit(), PlatformIconGroup.generalDropdown())
        ) {
            {
                registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), myActionsTree.getTree());
            }

            @RequiredUIAccess
            @Override
            public void update(@Nonnull AnActionEvent e) {
                String actionId = myActionsTree.getSelectedActionId();
                e.getPresentation().setEnabled(actionId != null);
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                editSelection(e.getInputEvent());
            }
        });

        panel.add(
            toolbar,
            new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insetsTop(8), 0, 0)
        );
        group = new DefaultActionGroup();
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
        actionToolbar.setTargetComponent(panel);
        final JComponent searchToolbar = actionToolbar.getComponent();
        final Alarm alarm = new Alarm();
        myFilterComponent = new FilterComponent("KEYMAP", 5) {
            @Override
            public void filter() {
                alarm.cancelAllRequests();
                alarm.addRequest(() -> {
                    if (!myFilterComponent.isShowing()) {
                        return;
                    }
                    if (!myTreeExpansionMonitor.isFreeze()) {
                        myTreeExpansionMonitor.freeze();
                    }
                    String filter = getFilter();
                    myActionsTree.filter(filter, getCurrentQuickListIds());
                    JTree tree = myActionsTree.getTree();
                    TreeUtil.expandAll(tree);
                    if (filter == null || filter.length() == 0) {
                        TreeUtil.collapseAll(tree, 0);
                        myTreeExpansionMonitor.restore();
                    }
                }, 300);
            }
        };
        myFilterComponent.reset();

        panel.add(
            myFilterComponent,
            new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insetsTop(8), 0, 0)
        );

        group.add(new DumbAwareAction(
            KeyMapLocalize.filterShortcutActionText(),
            KeyMapLocalize.filterShortcutActionText(),
            AllIcons.Actions.ShortcutFilter
        ) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                myFilterComponent.reset();
                if (myPopup == null || myPopup.getContent() == null) {
                    myPopup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(createFilteringPanel(), null)
                        .setRequestFocus(true)
                        .setTitle(KeyMapLocalize.filterSettingsPopupTitle())
                        .setCancelKeyEnabled(false)
                        .setMovable(true)
                        .createPopup();
                }
                myPopup.showUnderneathOf(searchToolbar);
            }
        });
        group.add(new DumbAwareAction(
            KeyMapLocalize.filterClearActionText(),
            KeyMapLocalize.filterClearActionText(),
            AllIcons.Actions.GC
        ) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                myActionsTree.filter(null, getCurrentQuickListIds()); //clear filtering
                TreeUtil.collapseAll(myActionsTree.getTree(), 0);
                myTreeExpansionMonitor.restore();
            }
        });

        panel.add(searchToolbar, new GridBagConstraints(
            2,
            0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            JBUI.insetsTop(8),
            0,
            0
        ));
        return panel;
    }

    private JPanel createKeymapNamePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        myBaseKeymapLabel = new JLabel(KeyMapLocalize.parentKeymapLabel().get());
        panel.add(myBaseKeymapLabel, new GridBagConstraints(
            0,
            0,
            1,
            1,
            1,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            JBUI.insetsLeft(16),
            0,
            0
        ));
        return panel;
    }

    private JPanel createFilteringPanel() {
        myActionsTree.reset(getSelectedKeymap(), getCurrentQuickListIds());

        JLabel firstLabel = new JLabel(KeyMapLocalize.filterFirstStrokeInput().get());
        final JCheckBox enable2Shortcut = new JCheckBox(KeyMapLocalize.filterSecondStrokeInput().get());
        final ShortcutTextField firstShortcut = new ShortcutTextField();
        final ShortcutTextField secondShortcut = new ShortcutTextField();

        enable2Shortcut.addActionListener(e -> {
            secondShortcut.setEnabled(enable2Shortcut.isSelected());
            if (enable2Shortcut.isSelected()) {
                secondShortcut.requestFocusInWindow();
            }
        });

        firstShortcut.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                filterTreeByShortcut(firstShortcut, enable2Shortcut, secondShortcut);
            }
        });

        secondShortcut.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                filterTreeByShortcut(firstShortcut, enable2Shortcut, secondShortcut);
            }
        });

        JPanel filterComponent = FormBuilder.createFormBuilder()
            .addLabeledComponent(firstLabel, firstShortcut, true)
            .addComponent(enable2Shortcut)
            .setVerticalGap(0)
            .setIndent(5)
            .addComponent(secondShortcut)
            .getPanel();

        filterComponent.setBorder(new EmptyBorder(UIUtil.PANEL_SMALL_INSETS));

        enable2Shortcut.setSelected(false);
        secondShortcut.setEnabled(false);
        IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(firstShortcut);
        return filterComponent;
    }

    private void filterTreeByShortcut(
        ShortcutTextField firstShortcut,
        JCheckBox enable2Shortcut,
        ShortcutTextField secondShortcut
    ) {
        KeyStroke keyStroke = firstShortcut.getKeyStroke();
        if (keyStroke != null) {
            if (!myTreeExpansionMonitor.isFreeze()) {
                myTreeExpansionMonitor.freeze();
            }
            myActionsTree.filterTree(
                new KeyboardShortcut(keyStroke, enable2Shortcut.isSelected() ? secondShortcut.getKeyStroke() : null),
                getCurrentQuickListIds()
            );
            JTree tree = myActionsTree.getTree();
            TreeUtil.expandAll(tree);
        }
    }

    public void showOption(String option) {
        createFilteringPanel();
        myFilterComponent.setFilter(option);
        myActionsTree.filter(option, getCurrentQuickListIds());
    }

    public static void addKeyboardShortcut(
        @Nonnull String actionId,
        @Nonnull ShortcutRestrictions restrictions,
        @Nonnull Keymap keymapSelected,
        @Nonnull Component parent,
        @Nonnull QuickList... quickLists
    ) {
        addKeyboardShortcut(actionId, restrictions, keymapSelected, parent, null, null, quickLists);
    }

    public static void addKeyboardShortcut(
        @Nonnull String actionId,
        @Nonnull ShortcutRestrictions restrictions,
        @Nonnull Keymap keymapSelected,
        @Nonnull Component parent,
        @Nullable KeyboardShortcut selectedShortcut,
        @Nullable SystemShortcuts systemShortcuts,
        @Nonnull QuickList... quickLists
    ) {
        if (!restrictions.allowKeyboardShortcut) {
            return;
        }
        //KeyboardShortcutDialog dialog = new KeyboardShortcutDialog(parent, restrictions.allowKeyboardSecondStroke, systemShortcuts == null ? null : systemShortcuts.createKeystroke2SysShortcutMap());
        //KeyboardShortcut keyboardShortcut = dialog.showAndGet(actionId, keymapSelected, selectedShortcut, quickLists);
        //if (keyboardShortcut == null) return;
        //
        //SafeKeymapAccessor accessor = new SafeKeymapAccessor(parent, keymapSelected);
        //if (dialog.hasConflicts()) {
        //  int result = showConfirmationDialog(parent);
        //  if (result == Messages.YES) {
        //    Keymap keymap = accessor.keymap();
        //    Map<String, List<KeyboardShortcut>> conflicts = keymap.getConflicts(actionId, keyboardShortcut);
        //    for (String id : conflicts.keySet()) {
        //      for (KeyboardShortcut s : conflicts.get(id)) {
        //        keymap.removeShortcut(id, s);
        //      }
        //    }
        //  }
        //  else if (result != Messages.NO) {
        //    return;
        //  }
        //}
        //if (systemShortcuts != null) { // check conflicts with system shortcuts
        //  final Keymap keymap = accessor.keymap();
        //  final Map<KeyboardShortcut, String> kscs = systemShortcuts.calculateConflicts(keymap, actionId);
        //  if (kscs != null && !kscs.isEmpty()) {
        //    for (KeyboardShortcut ksc : kscs.keySet()) {
        //      final int result = Messages.showYesNoCancelDialog(parent, "Action shortcut " + ksc + " is already assigned to system action '" + kscs.get(ksc) + "' . Do you want to remove this shortcut?",
        //                                                        KeyMapBundle.message("conflict.shortcut.dialog.title"), KeyMapBundle.message("conflict.shortcut.dialog.remove.button"), KeyMapBundle.message("conflict.shortcut.dialog.leave.button"),
        //                                                        KeyMapBundle.message("conflict.shortcut.dialog.cancel.button"), Messages.getWarningIcon());
        //      if (result == Messages.YES) {
        //        keymap.removeShortcut(actionId, ksc);
        //      }
        //    }
        //  }
        //}
        //accessor.add(actionId, keyboardShortcut);
        //if (systemShortcuts != null) systemShortcuts.updateKeymapConflicts(accessor.keymap());
    }

    @RequiredUIAccess
    private void addKeyboardShortcut(Shortcut shortcut) {
        String actionId = myActionsTree.getSelectedActionId();
        if (actionId == null) {
            return;
        }

        if (!createKeymapCopyIfNeeded()) {
            return;
        }

        KeyboardShortcutDialog dialog = new KeyboardShortcutDialog(myRootPanel, actionId, getCurrentQuickListIds());
        KeyboardShortcut selectedKeyboardShortcut = shortcut instanceof KeyboardShortcut keyboardShortcut ? keyboardShortcut : null;

        dialog.setData(mySelectedKeymap, selectedKeyboardShortcut);
        dialog.show();
        if (!dialog.isOK()) {
            return;
        }

        KeyboardShortcut keyboardShortcut = dialog.getKeyboardShortcut();

        if (keyboardShortcut == null) {
            return;
        }

        HashMap<String, ArrayList<KeyboardShortcut>> conflicts = mySelectedKeymap.getConflicts(actionId, keyboardShortcut);
        if (conflicts.size() > 0) {
            int result = Messages.showYesNoCancelDialog(
                myRootPanel,
                KeyMapLocalize.conflictShortcutDialogMessage().get(),
                KeyMapLocalize.conflictShortcutDialogTitle().get(),
                KeyMapLocalize.conflictShortcutDialogRemoveButton().get(),
                KeyMapLocalize.conflictShortcutDialogLeaveButton().get(),
                KeyMapLocalize.conflictShortcutDialogCancelButton().get(),
                UIUtil.getWarningIcon()
            );

            if (result == Messages.YES) {
                for (String id : conflicts.keySet()) {
                    for (KeyboardShortcut s : conflicts.get(id)) {
                        mySelectedKeymap.removeShortcut(id, s);
                    }
                }
            }
            else if (result != Messages.NO) {
                return;
            }
        }

        // if shortcut is already registered to this action, just select it in the list
        Shortcut[] shortcuts = mySelectedKeymap.getShortcuts(actionId);
        for (Shortcut s : shortcuts) {
            if (s.equals(keyboardShortcut)) {
                return;
            }
        }

        mySelectedKeymap.addShortcut(actionId, keyboardShortcut);
        if (StringUtil.startsWithChar(actionId, '$')) {
            mySelectedKeymap.addShortcut(KeyMapLocalize.editorShortcut(actionId.substring(1)).get(), keyboardShortcut);
        }

        repaintLists();
        processCurrentKeymapChanged(getCurrentQuickListIds());
    }

    private QuickList[] getCurrentQuickListIds() {
        return myQuickLists;
    }

    @RequiredUIAccess
    private void addMouseShortcut(Shortcut shortcut, ShortcutRestrictions restrictions) {
        String actionId = myActionsTree.getSelectedActionId();
        if (actionId == null) {
            return;
        }

        if (!createKeymapCopyIfNeeded()) {
            return;
        }

        MouseShortcut mouseShortcut = shortcut instanceof MouseShortcut ? (MouseShortcut)shortcut : null;

        MouseShortcutDialog dialog = new MouseShortcutDialog(
            myRootPanel,
            mouseShortcut,
            mySelectedKeymap,
            actionId,
            myActionsTree.getMainGroup(),
            restrictions
        );
        dialog.show();
        if (!dialog.isOK()) {
            return;
        }

        mouseShortcut = dialog.getMouseShortcut();

        if (mouseShortcut == null) {
            return;
        }

        String[] actionIds = mySelectedKeymap.getActionIds(mouseShortcut);
        if (actionIds.length > 1 || (actionIds.length == 1 && !actionId.equals(actionIds[0]))) {
            int result = Messages.showYesNoCancelDialog(
                myRootPanel,
                KeyMapLocalize.conflictShortcutDialogMessage().get(),
                KeyMapLocalize.conflictShortcutDialogTitle().get(),
                KeyMapLocalize.conflictShortcutDialogRemoveButton().get(),
                KeyMapLocalize.conflictShortcutDialogLeaveButton().get(),
                KeyMapLocalize.conflictShortcutDialogCancelButton().get(),
                UIUtil.getWarningIcon()
            );

            if (result == Messages.YES) {
                for (String id : actionIds) {
                    mySelectedKeymap.removeShortcut(id, mouseShortcut);
                }
            }
            else if (result != Messages.NO) {
                return;
            }
        }

        // if shortcut is aleady registered to this action, just select it in the list

        Shortcut[] shortcuts = mySelectedKeymap.getShortcuts(actionId);
        for (Shortcut shortcut1 : shortcuts) {
            if (shortcut1.equals(mouseShortcut)) {
                return;
            }
        }

        mySelectedKeymap.addShortcut(actionId, mouseShortcut);
        if (StringUtil.startsWithChar(actionId, '$')) {
            mySelectedKeymap.addShortcut(KeyMapLocalize.editorShortcut(actionId.substring(1)).get(), mouseShortcut);
        }

        repaintLists();
        processCurrentKeymapChanged(getCurrentQuickListIds());
    }

    private void repaintLists() {
        myActionsTree.getComponent().repaint();
    }

    @RequiredUIAccess
    private boolean createKeymapCopyIfNeeded() {
        if (mySelectedKeymap.canModify()) {
            return true;
        }

        KeymapImpl selectedKeymap = getSelectedKeymap();
        if (selectedKeymap == null) {
            return false;
        }

        KeymapImpl newKeymap = selectedKeymap.deriveKeymap();

        String newKeymapName = KeyMapLocalize.newKeymapName(selectedKeymap.getPresentableName()).get();
        if (!tryNewKeymapName(newKeymapName)) {
            for (int i = 0; ; i++) {
                newKeymapName = KeyMapLocalize.newIndexedKeymapName(selectedKeymap.getPresentableName(), i).get();
                if (tryNewKeymapName(newKeymapName)) {
                    break;
                }
            }
        }

        newKeymap.setName(newKeymapName);
        newKeymap.setCanModify(true);

        int indexOf = myKeymapListModel.getIndexOf(selectedKeymap);
        if (indexOf >= 0) {
            myKeymapListModel.insertElementAt(newKeymap, indexOf + 1);
        }
        else {
            myKeymapListModel.addElement(newKeymap);
        }

        myKeymapList.setSelectedItem(newKeymap);
        processCurrentKeymapChanged(getCurrentQuickListIds());

        return true;
    }

    private void removeShortcut(Shortcut shortcut) {
        String actionId = myActionsTree.getSelectedActionId();
        if (actionId == null) {
            return;
        }

        if (!createKeymapCopyIfNeeded()) {
            return;
        }

        if (shortcut == null) {
            return;
        }

        mySelectedKeymap.removeShortcut(actionId, shortcut);
        if (StringUtil.startsWithChar(actionId, '$')) {
            mySelectedKeymap.removeShortcut(KeyMapLocalize.editorShortcut(actionId.substring(1)).get(), shortcut);
        }

        repaintLists();
        processCurrentKeymapChanged(getCurrentQuickListIds());
    }

    private void copyKeymap() {
        KeymapImpl keymap = getSelectedKeymap();
        if (keymap == null) {
            return;
        }
        KeymapImpl newKeymap = keymap.deriveKeymap();

        String newKeymapName = KeyMapLocalize.newKeymapName(keymap.getPresentableName()).get();
        if (!tryNewKeymapName(newKeymapName)) {
            for (int i = 0; ; i++) {
                newKeymapName = KeyMapLocalize.newIndexedKeymapName(keymap.getPresentableName(), i).get();
                if (tryNewKeymapName(newKeymapName)) {
                    break;
                }
            }
        }
        newKeymap.setName(newKeymapName);
        newKeymap.setCanModify(true);
        myKeymapListModel.addElement(newKeymap);
        myKeymapList.setSelectedItem(newKeymap);
        myKeymapList.getEditor().selectAll();
        processCurrentKeymapChanged(getCurrentQuickListIds());
    }

    private boolean tryNewKeymapName(String name) {
        for (int i = 0; i < myKeymapListModel.getSize(); i++) {
            Keymap k = (Keymap)myKeymapListModel.getElementAt(i);
            if (name.equals(k.getPresentableName())) {
                return false;
            }
        }

        return true;
    }

    @RequiredUIAccess
    private void deleteKeymap() {
        Keymap keymap = getSelectedKeymap();
        if (keymap == null) {
            return;
        }
        int result = Messages.showYesNoDialog(
            myRootPanel,
            KeyMapLocalize.deleteKeymapDialogMessage().get(),
            KeyMapLocalize.deleteKeymapDialogTitle().get(),
            UIUtil.getWarningIcon()
        );
        if (result != Messages.YES) {
            return;
        }
        myKeymapListModel.removeElement(myKeymapList.getSelectedItem());
        processCurrentKeymapChanged(getCurrentQuickListIds());
    }

    private void resetKeymap() {
        Keymap keymap = getSelectedKeymap();
        if (keymap == null) {
            return;
        }
        ((KeymapImpl)keymap).clearOwnActionsIds();
        processCurrentKeymapChanged(getCurrentQuickListIds());
    }

    @Override
    @Nonnull
    public String getId() {
        return "preferences.keymap";
    }

    @Nullable
    @Override
    public String getParentId() {
        return null;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        KeyMapSetting setting = myKeyMapSettingProvider.get();

        myUseUnicodeCharactersInShortcutsBox.setState(ThreeStateCheckBox.State.fromBoolean(setting.isUseUnicodeShortcuts()));

        myDoublePressShortcutsBox.setSelected(setting.isEnabledDoublePressShortcuts());

        if (myNonEnglishKeyboardSupportOption != null) {
            KeyboardSettingsExternalizable.getInstance().setNonEnglishKeyboardSupportEnabled(false);
            myNonEnglishKeyboardSupportOption.setValue(KeyboardSettingsExternalizable.getInstance()
                .isNonEnglishKeyboardSupportEnabled());
        }

        myKeymapListModel.removeAllElements();
        KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
        Keymap[] keymaps = keymapManager.getAllKeymaps();
        for (Keymap keymap1 : keymaps) {
            KeymapImpl keymap = (KeymapImpl)keymap1;
            if (keymap.canModify()) {
                keymap = keymap.copy(true);
            }

            myKeymapListModel.addElement(keymap);
            if (Comparing.equal(keymapManager.getActiveKeymap(), keymap1)) {
                mySelectedKeymap = keymap;
            }
        }

        if (myKeymapListModel.getSize() == 0) {
            KeymapImpl keymap = new KeymapImpl();
            keymap.setName(KeyMapLocalize.keymapNoName().get());
            myKeymapListModel.addElement(keymap);
        }

        myKeymapList.setSelectedItem(mySelectedKeymap);
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {
        KeyMapSetting setting = myKeyMapSettingProvider.get();

        switch (myUseUnicodeCharactersInShortcutsBox.getState()) {
            case SELECTED:
                setting.setUseUnicodeShortcuts(true);
                break;
            case NOT_SELECTED:
                setting.setUseUnicodeShortcuts(false);
                break;
            case DONT_CARE:
                setting.setUseUnicodeShortcuts(null);
                break;
        }

        setting.setEnabledDoublePressShortcuts(myDoublePressShortcutsBox.isSelected());

        ensureNonEmptyKeymapNames();
        ensureUniqueKeymapNames();
        KeymapManagerImpl keymapManager = (KeymapManagerImpl)KeymapManager.getInstance();
        keymapManager.removeAllKeymapsExceptUnmodifiable();
        for (int i = 0; i < myKeymapListModel.getSize(); i++) {
            Keymap modelKeymap = (Keymap)myKeymapListModel.getElementAt(i);
            if (modelKeymap.canModify()) {
                KeymapImpl keymapToAdd = ((KeymapImpl)modelKeymap).copy(true);
                keymapManager.addKeymap(keymapToAdd);
            }
        }
        keymapManager.setActiveKeymap(mySelectedKeymap);
        ActionToolbarsHolder.updateAllToolbarsImmediately();
    }

    private void ensureNonEmptyKeymapNames() throws ConfigurationException {
        for (int i = 0; i < myKeymapListModel.getSize(); i++) {
            Keymap modelKeymap = (Keymap)myKeymapListModel.getElementAt(i);
            if (StringUtil.isEmptyOrSpaces(modelKeymap.getName())) {
                throw new ConfigurationException(KeyMapLocalize.configurationAllKeymapsShouldHaveNonEmptyNamesErrorMessage());
            }
        }
    }

    private void ensureUniqueKeymapNames() throws ConfigurationException {
        Set<String> keymapNames = new HashSet<>();
        for (int i = 0; i < myKeymapListModel.getSize(); i++) {
            Keymap modelKeymap = (Keymap)myKeymapListModel.getElementAt(i);
            String name = modelKeymap.getName();
            if (keymapNames.contains(name)) {
                throw new ConfigurationException(KeyMapLocalize.configurationAllKeymapsShouldHaveUniqueNamesErrorMessage());
            }
            keymapNames.add(name);
        }
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
        if (!Comparing.equal(mySelectedKeymap, keymapManager.getActiveKeymap())) {
            return true;
        }

        KeyMapSetting setting = myKeyMapSettingProvider.get();

        ThreeStateCheckBox.State currentState = ThreeStateCheckBox.State.fromBoolean(setting.isUseUnicodeShortcuts());
        if (!Objects.equals(myUseUnicodeCharactersInShortcutsBox.getState(), currentState)) {
            return true;
        }

        if (myDoublePressShortcutsBox.isSelected() != setting.isEnabledDoublePressShortcuts()) {
            return true;
        }

        Keymap[] managerKeymaps = keymapManager.getAllKeymaps();
        Keymap[] panelKeymaps = new Keymap[myKeymapListModel.getSize()];
        for (int i = 0; i < myKeymapListModel.getSize(); i++) {
            panelKeymaps[i] = (Keymap)myKeymapListModel.getElementAt(i);
        }
        return !Arrays.equals(managerKeymaps, panelKeymaps);
    }

    public void selectAction(String actionId) {
        myActionsTree.selectAction(actionId);
    }

    private static class MyEditor extends FixedComboBoxEditor {
        private KeymapImpl myKeymap = null;

        public MyEditor() {
            getField().getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent e) {
                    if (myKeymap != null && myKeymap.canModify()) {
                        myKeymap.setName(getField().getText());
                    }
                }
            });
        }

        @Override
        public void setItem(Object anObject) {
            if (anObject instanceof KeymapImpl) {
                myKeymap = (KeymapImpl)anObject;
                getField().setText(myKeymap.getPresentableName());
            }
        }

        @Override
        public Object getItem() {
            return myKeymap;
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return KeyMapLocalize.keymapDisplayName();
    }

    @RequiredUIAccess
    @Override
    public JComponent createComponent() {
        if (myRootPanel == null) {
            myRootPanel = createKeymapRootPanel();
        }
        return myRootPanel;
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        if (myPopup != null && myPopup.isVisible()) {
            myPopup.cancel();
        }
        if (myFilterComponent != null) {
            myFilterComponent.dispose();
        }

        if (myUIDisposable != null) {
            Disposer.dispose(myUIDisposable);
            myUIDisposable = null;
        }

        myActionsTree = null;
        myKeymapList = null;
        myRootPanel = null;
    }

    private void editSelection(InputEvent e) {
        final String actionId = myActionsTree.getSelectedActionId();
        if (actionId == null) {
            return;
        }

        DefaultActionGroup group = new DefaultActionGroup();

        final Shortcut[] shortcuts = mySelectedKeymap.getShortcuts(actionId);
        Set<String> abbreviations = AbbreviationManager.getInstance().getAbbreviations(actionId);

        final ShortcutRestrictions restrictions = ActionShortcutRestrictions.getInstance().getForActionId(actionId);

        if (restrictions.allowKeyboardShortcut) {
            group.add(new DumbAwareAction(IdeLocalize.actionAnonymousTextAddKeyboardShortcut()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    Shortcut firstKeyboard = null;
                    for (Shortcut shortcut : shortcuts) {
                        if (shortcut instanceof KeyboardShortcut) {
                            firstKeyboard = shortcut;
                            break;
                        }
                    }

                    addKeyboardShortcut(firstKeyboard);
                }
            });
        }

        if (restrictions.allowMouseShortcut) {
            group.add(new DumbAwareAction(IdeLocalize.actionAnonymousTextAddMouseShortcut()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    Shortcut firstMouse = null;
                    for (Shortcut shortcut : shortcuts) {
                        if (shortcut instanceof MouseShortcut) {
                            firstMouse = shortcut;
                            break;
                        }
                    }
                    addMouseShortcut(firstMouse, restrictions);
                }
            });
        }

        if (restrictions.allowAbbreviation) {
            group.add(new DumbAwareAction(IdeLocalize.actionAnonymousTextAddAbbreviation()) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    String abbr = Messages.showInputDialog(
                        IdeLocalize.labelEnterNewAbbreviation().get(),
                        IdeLocalize.dialogTitleAbbreviation().get(),
                        null
                    );
                    if (abbr != null) {
                        String actionId = myActionsTree.getSelectedActionId();
                        AbbreviationManager.getInstance().register(abbr, actionId);
                        repaintLists();
                    }
                }

                @RequiredUIAccess
                @Override
                public void update(@Nonnull AnActionEvent e) {
                    boolean enabled = myActionsTree.getSelectedActionId() != null;
                    e.getPresentation().setEnabledAndVisible(enabled);
                }
            });
        }

        group.addSeparator();

        for (final Shortcut shortcut : shortcuts) {
            group.add(new DumbAwareAction(IdeLocalize.actionTextRemove0(KeymapUtil.getShortcutText(shortcut))) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    removeShortcut(shortcut);
                }
            });
        }

        for (final String abbreviation : abbreviations) {
            group.addAction(new DumbAwareAction(IdeLocalize.actionTextRemoveAbbreviation0(abbreviation)) {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    AbbreviationManager.getInstance().remove(abbreviation, actionId);
                    repaintLists();
                }

                @RequiredUIAccess
                @Override
                public void update(@Nonnull AnActionEvent e) {
                    super.update(e);
                }
            });
        }

        if (e instanceof MouseEvent mouseEvent && mouseEvent.isPopupTrigger()) {
            ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
            popupMenu.getComponent().show(e.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }
        else {
            DataContext dataContext = DataManager.getInstance().getDataContext(myRootPanel);
            ListPopup popup = JBPopupFactory.getInstance()
                .createActionGroupPopup(IdeLocalize.popupTitleEditShortcuts().get(), group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);

            if (e instanceof MouseEvent mouseEvent) {
                popup.show(new RelativePoint(mouseEvent));
            }
            else {
                popup.showInBestPositionFor(dataContext);
            }
        }
    }
}
