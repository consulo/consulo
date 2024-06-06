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
import consulo.configurable.*;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.TreeExpander;
import consulo.ide.impl.idea.openapi.actionSystem.AbbreviationManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickListsManager;
import consulo.ui.ex.internal.ActionToolbarsHolder;
import consulo.ui.ex.keymap.KeyMapBundle;
import consulo.ide.impl.idea.openapi.keymap.KeyboardSettingsExternalizable;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.keymap.ex.KeymapManagerEx;
import consulo.ide.impl.idea.openapi.keymap.impl.*;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.FixedComboBoxEditor;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.packageDependencies.ui.TreeExpansionMonitor;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.FilterComponent;
import consulo.ui.ex.awt.FormBuilder;
import consulo.application.AllIcons;
import consulo.application.CommonBundle;
import consulo.application.ui.wm.FocusableFrame;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.image.ImageEffects;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

  private KeymapImpl mySelectedKeymap;

  private JButton myCopyButton;
  private JButton myDeleteButton;
  private JButton myResetToDefault;
  private JCheckBox myNonEnglishKeyboardSupportOption;

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

  @Inject
  public KeymapPanel() {
    myAncestor = evt -> {
      if (evt.getPropertyName().equals("ancestor") && evt.getNewValue() != null && evt.getOldValue() == null && myQuickListsModified) {
        processCurrentKeymapChanged(getCurrentQuickListIds());
        myQuickListsModified = false;
      }
    };
  }

  @Override
  public void quickListRenamed(final QuickList oldQuickList, final QuickList newQuickList) {
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
    myKeymapList.setRenderer(new ColoredListCellRenderer<Keymap>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList<? extends Keymap> list, Keymap keymap, int index, boolean selected, boolean hasFocus) {
        if (keymap != null) {
          String name = keymap.getPresentableName();
          if (name == null) {
            name = KeyMapBundle.message("keymap.noname.presentable.name");
          }
          append(name);
        }
      }
    });
    JLabel keymapLabel = new JLabel(KeyMapBundle.message("keymaps.border.factory.title"));
    keymapLabel.setLabelFor(myKeymapList);
    panel.add(keymapLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0));
    panel.add(myKeymapList, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insetsLeft(4), 0, 0));

    panel.add(createKeymapButtonsPanel(), new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0));
    myKeymapList.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myKeymapListModel.getSelectedItem() != mySelectedKeymap) processCurrentKeymapChanged(getCurrentQuickListIds());
      }
    });
    panel.add(createKeymapNamePanel(), new GridBagConstraints(3, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insetsLeft(10), 0, 0));
    return panel;
  }

  @Override
  public Runnable enableSearch(final String option) {
    return () -> showOption(option);
  }

  @Override
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
      myBaseKeymapLabel.setText(KeyMapBundle.message("based.on.keymap.label", parent.getPresentableName()));
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
    List<Keymap> result = new ArrayList<Keymap>();
    for (int i = 0; i < model.getSize(); i++) {
      result.add((Keymap)model.getElementAt(i));
    }
    return result;
  }


  private JPanel createKeymapButtonsPanel() {
    final JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    panel.setLayout(new GridBagLayout());
    myCopyButton = new JButton(KeyMapBundle.message("copy.keymap.button"));
    Insets insets = JBUI.insets(2);
    myCopyButton.setMargin(insets);
    final GridBagConstraints gc = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insetsLeft(5), 0, 0);
    panel.add(myCopyButton, gc);
    myResetToDefault = new JButton(CommonBundle.message("button.reset"));
    myResetToDefault.setMargin(insets);
    panel.add(myResetToDefault, gc);
    myDeleteButton = new JButton(KeyMapBundle.message("delete.keymap.button"));
    myDeleteButton.setMargin(insets);
    gc.weightx = 1;
    panel.add(myDeleteButton, gc);

    FocusableFrame ideFrame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
    if (ideFrame != null && KeyboardSettingsExternalizable.isSupportedKeyboardLayout(ideFrame.getComponent())) {
      String displayLanguage = ideFrame.getComponent().getInputContext().getLocale().getDisplayLanguage();
      myNonEnglishKeyboardSupportOption = new JCheckBox(new AbstractAction(displayLanguage + " " + KeyMapBundle.message("use.non.english.keyboard.layout.support")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          KeyboardSettingsExternalizable.getInstance().setNonEnglishKeyboardSupportEnabled(myNonEnglishKeyboardSupportOption.isSelected());
        }
      });
      myNonEnglishKeyboardSupportOption.setSelected(KeyboardSettingsExternalizable.getInstance().isNonEnglishKeyboardSupportEnabled());
      panel.add(myNonEnglishKeyboardSupportOption, gc);
    }

    myCopyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        copyKeymap();
      }
    });

    myResetToDefault.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        resetKeymap();
      }

    });

    myDeleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        deleteKeymap();
      }
    });

    return panel;
  }

  private JPanel createKeymapSettingsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    myActionsTree = new ActionsTree(myUIDisposable);

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
    return panel;
  }

  private JPanel createToolbarPanel() {
    final JPanel panel = new JPanel(new GridBagLayout());
    DefaultActionGroup group = new DefaultActionGroup();
    ActionToolbar firstToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
    firstToolbar.setTargetComponent(panel);
    final JComponent toolbar = firstToolbar.getComponent();
    final CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
    final TreeExpander treeExpander = new TreeExpander() {
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

    group.add(new DumbAwareAction("Edit Shortcut", "Edit Shortcut", ImageEffects.layered(PlatformIconGroup.actionsEdit(), PlatformIconGroup.generalDropdown())) {
      {
        registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), myActionsTree.getTree());
      }

      @RequiredUIAccess
      @Override
      public void update(@Nonnull AnActionEvent e) {
        final String actionId = myActionsTree.getSelectedActionId();
        e.getPresentation().setEnabled(actionId != null);
      }

      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        editSelection(e.getInputEvent());
      }
    });

    panel.add(toolbar, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insetsTop(8), 0, 0));
    group = new DefaultActionGroup();
    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);
    actionToolbar.setTargetComponent(panel);
    final JComponent searchToolbar = actionToolbar.getComponent();
    final Alarm alarm = new Alarm();
    myFilterComponent = new FilterComponent("KEYMAP", 5) {
      @Override
      public void filter() {
        alarm.cancelAllRequests();
        alarm.addRequest(new Runnable() {
          @Override
          public void run() {
            if (!myFilterComponent.isShowing()) return;
            if (!myTreeExpansionMonitor.isFreeze()) myTreeExpansionMonitor.freeze();
            final String filter = getFilter();
            myActionsTree.filter(filter, getCurrentQuickListIds());
            final JTree tree = myActionsTree.getTree();
            TreeUtil.expandAll(tree);
            if (filter == null || filter.length() == 0) {
              TreeUtil.collapseAll(tree, 0);
              myTreeExpansionMonitor.restore();
            }
          }
        }, 300);
      }
    };
    myFilterComponent.reset();

    panel.add(myFilterComponent, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insetsTop(8), 0, 0));

    group.add(new DumbAwareAction(KeyMapBundle.message("filter.shortcut.action.text"), KeyMapBundle.message("filter.shortcut.action.text"), AllIcons.Actions.ShortcutFilter) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        myFilterComponent.reset();
        if (myPopup == null || myPopup.getContent() == null) {
          myPopup = JBPopupFactory.getInstance().createComponentPopupBuilder(createFilteringPanel(), null).setRequestFocus(true).setTitle(KeyMapBundle.message("filter.settings.popup.title"))
                  .setCancelKeyEnabled(false).setMovable(true).createPopup();
        }
        myPopup.showUnderneathOf(searchToolbar);
      }
    });
    group.add(new DumbAwareAction(KeyMapBundle.message("filter.clear.action.text"), KeyMapBundle.message("filter.clear.action.text"), AllIcons.Actions.GC) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        myActionsTree.filter(null, getCurrentQuickListIds()); //clear filtering
        TreeUtil.collapseAll(myActionsTree.getTree(), 0);
        myTreeExpansionMonitor.restore();
      }
    });

    panel.add(searchToolbar, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insetsTop(8), 0, 0));
    return panel;
  }

  private JPanel createKeymapNamePanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    myBaseKeymapLabel = new JLabel(KeyMapBundle.message("parent.keymap.label"));
    panel.add(myBaseKeymapLabel, new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insetsLeft(16), 0, 0));
    return panel;
  }

  private JPanel createFilteringPanel() {
    myActionsTree.reset(getSelectedKeymap(), getCurrentQuickListIds());

    final JLabel firstLabel = new JLabel(KeyMapBundle.message("filter.first.stroke.input"));
    final JCheckBox enable2Shortcut = new JCheckBox(KeyMapBundle.message("filter.second.stroke.input"));
    final ShortcutTextField firstShortcut = new ShortcutTextField();
    final ShortcutTextField secondShortcut = new ShortcutTextField();

    enable2Shortcut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        secondShortcut.setEnabled(enable2Shortcut.isSelected());
        if (enable2Shortcut.isSelected()) {
          secondShortcut.requestFocusInWindow();
        }
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

    JPanel filterComponent =
            FormBuilder.createFormBuilder().addLabeledComponent(firstLabel, firstShortcut, true).addComponent(enable2Shortcut).setVerticalGap(0).setIndent(5).addComponent(secondShortcut).getPanel();

    filterComponent.setBorder(new EmptyBorder(UIUtil.PANEL_SMALL_INSETS));

    enable2Shortcut.setSelected(false);
    secondShortcut.setEnabled(false);
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(firstShortcut);
    return filterComponent;
  }

  private void filterTreeByShortcut(final ShortcutTextField firstShortcut, final JCheckBox enable2Shortcut, final ShortcutTextField secondShortcut) {
    final KeyStroke keyStroke = firstShortcut.getKeyStroke();
    if (keyStroke != null) {
      if (!myTreeExpansionMonitor.isFreeze()) myTreeExpansionMonitor.freeze();
      myActionsTree.filterTree(new KeyboardShortcut(keyStroke, enable2Shortcut.isSelected() ? secondShortcut.getKeyStroke() : null), getCurrentQuickListIds());
      final JTree tree = myActionsTree.getTree();
      TreeUtil.expandAll(tree);
    }
  }

  public void showOption(String option) {
    createFilteringPanel();
    myFilterComponent.setFilter(option);
    myActionsTree.filter(option, getCurrentQuickListIds());
  }

  public static void addKeyboardShortcut(@Nonnull String actionId,
                                         @Nonnull ShortcutRestrictions restrictions,
                                         @Nonnull Keymap keymapSelected,
                                         @Nonnull Component parent,
                                         @Nonnull QuickList... quickLists) {
    addKeyboardShortcut(actionId, restrictions, keymapSelected, parent, null, null, quickLists);
  }

  public static void addKeyboardShortcut(@Nonnull String actionId,
                                         @Nonnull ShortcutRestrictions restrictions,
                                         @Nonnull Keymap keymapSelected,
                                         @Nonnull Component parent,
                                         @Nullable KeyboardShortcut selectedShortcut,
                                         @Nullable SystemShortcuts systemShortcuts,
                                         @Nonnull QuickList... quickLists) {
    if (!restrictions.allowKeyboardShortcut) return;
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

  private void addKeyboardShortcut(Shortcut shortcut) {
    String actionId = myActionsTree.getSelectedActionId();
    if (actionId == null) {
      return;
    }

    if (!createKeymapCopyIfNeeded()) return;

    KeyboardShortcutDialog dialog = new KeyboardShortcutDialog(myRootPanel, actionId, getCurrentQuickListIds());


    KeyboardShortcut selectedKeyboardShortcut = shortcut instanceof KeyboardShortcut ? (KeyboardShortcut)shortcut : null;

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
      int result = Messages.showYesNoCancelDialog(myRootPanel, KeyMapBundle.message("conflict.shortcut.dialog.message"), KeyMapBundle.message("conflict.shortcut.dialog.title"),
                                                  KeyMapBundle.message("conflict.shortcut.dialog.remove.button"), KeyMapBundle.message("conflict.shortcut.dialog.leave.button"),
                                                  KeyMapBundle.message("conflict.shortcut.dialog.cancel.button"), Messages.getWarningIcon());

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
      mySelectedKeymap.addShortcut(KeyMapBundle.message("editor.shortcut", actionId.substring(1)), keyboardShortcut);
    }

    repaintLists();
    processCurrentKeymapChanged(getCurrentQuickListIds());
  }

  private QuickList[] getCurrentQuickListIds() {
    return myQuickLists;
  }

  private void addMouseShortcut(Shortcut shortcut, ShortcutRestrictions restrictions) {
    String actionId = myActionsTree.getSelectedActionId();
    if (actionId == null) {
      return;
    }

    if (!createKeymapCopyIfNeeded()) return;

    MouseShortcut mouseShortcut = shortcut instanceof MouseShortcut ? (MouseShortcut)shortcut : null;

    MouseShortcutDialog dialog = new MouseShortcutDialog(myRootPanel, mouseShortcut, mySelectedKeymap, actionId, myActionsTree.getMainGroup(), restrictions);
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
      int result = Messages.showYesNoCancelDialog(myRootPanel, KeyMapBundle.message("conflict.shortcut.dialog.message"), KeyMapBundle.message("conflict.shortcut.dialog.title"),
                                                  KeyMapBundle.message("conflict.shortcut.dialog.remove.button"), KeyMapBundle.message("conflict.shortcut.dialog.leave.button"),
                                                  KeyMapBundle.message("conflict.shortcut.dialog.cancel.button"), Messages.getWarningIcon());

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
      mySelectedKeymap.addShortcut(KeyMapBundle.message("editor.shortcut", actionId.substring(1)), mouseShortcut);
    }

    repaintLists();
    processCurrentKeymapChanged(getCurrentQuickListIds());
  }

  private void repaintLists() {
    myActionsTree.getComponent().repaint();
  }

  private boolean createKeymapCopyIfNeeded() {
    if (mySelectedKeymap.canModify()) return true;

    final KeymapImpl selectedKeymap = getSelectedKeymap();
    if (selectedKeymap == null) {
      return false;
    }

    KeymapImpl newKeymap = selectedKeymap.deriveKeymap();

    String newKeymapName = KeyMapBundle.message("new.keymap.name", selectedKeymap.getPresentableName());
    if (!tryNewKeymapName(newKeymapName)) {
      for (int i = 0; ; i++) {
        newKeymapName = KeyMapBundle.message("new.indexed.keymap.name", selectedKeymap.getPresentableName(), i);
        if (tryNewKeymapName(newKeymapName)) {
          break;
        }
      }
    }

    newKeymap.setName(newKeymapName);
    newKeymap.setCanModify(true);

    final int indexOf = myKeymapListModel.getIndexOf(selectedKeymap);
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

    if (!createKeymapCopyIfNeeded()) return;

    if (shortcut == null) return;

    mySelectedKeymap.removeShortcut(actionId, shortcut);
    if (StringUtil.startsWithChar(actionId, '$')) {
      mySelectedKeymap.removeShortcut(KeyMapBundle.message("editor.shortcut", actionId.substring(1)), shortcut);
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

    String newKeymapName = KeyMapBundle.message("new.keymap.name", keymap.getPresentableName());
    if (!tryNewKeymapName(newKeymapName)) {
      for (int i = 0; ; i++) {
        newKeymapName = KeyMapBundle.message("new.indexed.keymap.name", keymap.getPresentableName(), i);
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

  private void deleteKeymap() {
    Keymap keymap = getSelectedKeymap();
    if (keymap == null) {
      return;
    }
    int result = Messages.showYesNoDialog(myRootPanel, KeyMapBundle.message("delete.keymap.dialog.message"), KeyMapBundle.message("delete.keymap.dialog.title"), Messages.getWarningIcon());
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
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    if (myNonEnglishKeyboardSupportOption != null) {
      KeyboardSettingsExternalizable.getInstance().setNonEnglishKeyboardSupportEnabled(false);
      myNonEnglishKeyboardSupportOption.setSelected(KeyboardSettingsExternalizable.getInstance().isNonEnglishKeyboardSupportEnabled());
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
      keymap.setName(KeyMapBundle.message("keymap.no.name"));
      myKeymapListModel.addElement(keymap);
    }

    myKeymapList.setSelectedItem(mySelectedKeymap);
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    ensureNonEmptyKeymapNames();
    ensureUniqueKeymapNames();
    final KeymapManagerImpl keymapManager = (KeymapManagerImpl)KeymapManager.getInstance();
    keymapManager.removeAllKeymapsExceptUnmodifiable();
    for (int i = 0; i < myKeymapListModel.getSize(); i++) {
      final Keymap modelKeymap = (Keymap)myKeymapListModel.getElementAt(i);
      if (modelKeymap.canModify()) {
        final KeymapImpl keymapToAdd = ((KeymapImpl)modelKeymap).copy(true);
        keymapManager.addKeymap(keymapToAdd);
      }
    }
    keymapManager.setActiveKeymap(mySelectedKeymap);
    ActionToolbarsHolder.updateAllToolbarsImmediately();
  }

  private void ensureNonEmptyKeymapNames() throws ConfigurationException {
    for (int i = 0; i < myKeymapListModel.getSize(); i++) {
      final Keymap modelKeymap = (Keymap)myKeymapListModel.getElementAt(i);
      if (StringUtil.isEmptyOrSpaces(modelKeymap.getName())) {
        throw new ConfigurationException(KeyMapBundle.message("configuration.all.keymaps.should.have.non.empty.names.error.message"));
      }
    }
  }

  private void ensureUniqueKeymapNames() throws ConfigurationException {
    final Set<String> keymapNames = new HashSet<String>();
    for (int i = 0; i < myKeymapListModel.getSize(); i++) {
      final Keymap modelKeymap = (Keymap)myKeymapListModel.getElementAt(i);
      String name = modelKeymap.getName();
      if (keymapNames.contains(name)) {
        throw new ConfigurationException(KeyMapBundle.message("configuration.all.keymaps.should.have.unique.names.error.message"));
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
    Keymap[] managerKeymaps = keymapManager.getAllKeymaps();
    Keymap[] panelKeymaps = new Keymap[myKeymapListModel.getSize()];
    for (int i = 0; i < myKeymapListModel.getSize(); i++) {
      panelKeymaps[i] = (Keymap)myKeymapListModel.getElementAt(i);
    }
    return !Comparing.equal(managerKeymaps, panelKeymaps);
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

  @Override
  @Nls
  public String getDisplayName() {
    return KeyMapBundle.message("keymap.display.name");
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    if(myRootPanel == null) {
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

    if(myUIDisposable != null) {
      Disposer.dispose(myUIDisposable);
      myUIDisposable = null;
    }

    myActionsTree = null;
    myKeymapList = null;
    myRootPanel = null;
  }

  private void editSelection(InputEvent e) {
    final String actionId = myActionsTree.getSelectedActionId();
    if (actionId == null) return;

    DefaultActionGroup group = new DefaultActionGroup();

    final Shortcut[] shortcuts = mySelectedKeymap.getShortcuts(actionId);
    final Set<String> abbreviations = AbbreviationManager.getInstance().getAbbreviations(actionId);

    final ShortcutRestrictions restrictions = ActionShortcutRestrictions.getInstance().getForActionId(actionId);

    if (restrictions.allowKeyboardShortcut) {
      group.add(new DumbAwareAction("Add Keyboard Shortcut") {
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
      group.add(new DumbAwareAction("Add Mouse Shortcut") {
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
      group.add(new DumbAwareAction("Add Abbreviation") {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          final String abbr = Messages.showInputDialog("Enter new abbreviation:", "Abbreviation", null);
          if (abbr != null) {
            String actionId = myActionsTree.getSelectedActionId();
            AbbreviationManager.getInstance().register(abbr, actionId);
            repaintLists();
          }
        }

        @RequiredUIAccess
        @Override
        public void update(@Nonnull AnActionEvent e) {
          final boolean enabled = myActionsTree.getSelectedActionId() != null;
          e.getPresentation().setEnabledAndVisible(enabled);
        }
      });
    }

    group.addSeparator();

    for (final Shortcut shortcut : shortcuts) {
      group.add(new DumbAwareAction("Remove " + KeymapUtil.getShortcutText(shortcut)) {
        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          removeShortcut(shortcut);
        }
      });
    }

    for (final String abbreviation : abbreviations) {
      group.addAction(new DumbAwareAction("Remove Abbreviation '" + abbreviation + "'") {
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

    if (e instanceof MouseEvent && ((MouseEvent)e).isPopupTrigger()) {
      final ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
      popupMenu.getComponent().show(e.getComponent(), ((MouseEvent)e).getX(), ((MouseEvent)e).getY());
    }
    else {
      final DataContext dataContext = DataManager.getInstance().getDataContext(myRootPanel);
      final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup("Edit Shortcuts", group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);

      if (e instanceof MouseEvent) {
        popup.show(new RelativePoint((MouseEvent)e));
      }
      else {
        popup.showInBestPositionFor(dataContext);
      }
    }
  }
}
