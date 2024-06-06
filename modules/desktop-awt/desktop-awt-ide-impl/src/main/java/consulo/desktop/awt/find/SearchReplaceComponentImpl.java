// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.find;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.find.FindBundle;
import consulo.find.FindInProjectSettings;
import consulo.ide.impl.idea.find.SearchReplaceComponent;
import consulo.ide.impl.idea.find.editorHeaderActions.*;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.editor.impl.EditorHeaderComponent;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.ide.impl.idea.ui.ListFocusTraversalPolicy;
import consulo.ide.impl.idea.ui.mac.TouchbarDataKeys;
import consulo.ide.impl.idea.util.BooleanFunction;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.internal.ActionToolbarEx;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.META_DOWN_MASK;

public class SearchReplaceComponentImpl extends EditorHeaderComponent implements SearchReplaceComponent {
  private final EventDispatcher<Listener> myEventDispatcher = EventDispatcher.create(Listener.class);

  private final MyTextComponentWrapper mySearchFieldWrapper;
  private JTextComponent mySearchTextComponent;

  private final MyTextComponentWrapper myReplaceFieldWrapper;
  private JTextComponent myReplaceTextComponent;

  private final JPanel myLeftPanel;
  private final JPanel myRightPanel;

  private final DefaultActionGroup mySearchFieldActions;
  private final ActionToolbar mySearchActionsToolbar;
  private final List<AnAction> myEmbeddedSearchActions = new ArrayList<>();
  private final List<Component> myExtraSearchButtons = new ArrayList<>();
  @Nonnull
  private final ActionToolbarEx.PopupStateModifier mySearchToolbar1PopupStateModifier;

  private final DefaultActionGroup myReplaceFieldActions;
  private final ActionToolbar myReplaceActionsToolbar;
  private final List<AnAction> myEmbeddedReplaceActions = new ArrayList<>();
  private final List<Component> myExtraReplaceButtons = new ArrayList<>();

  private final JPanel myReplaceToolbarWrapper;

  private final Project myProject;
  private final JComponent myTargetComponent;

  private final Runnable myCloseAction;
  private final Runnable myReplaceAction;

  private final DataProvider myDataProviderDelegate;

  private boolean myMultilineMode;
  @Nonnull
  private String myStatusText = "";
  @Nonnull
  private Color myStatusColor = UIUtil.getLabelForeground();
  private DefaultActionGroup myTouchbarActions;

  SearchReplaceComponentImpl(@Nullable Project project,
                             @Nonnull JComponent targetComponent,
                             @Nonnull DefaultActionGroup searchToolbar1Actions,
                             @Nonnull final BooleanGetter searchToolbar1ModifiedFlagGetter,
                             @Nonnull DefaultActionGroup searchToolbar2Actions,
                             @Nonnull DefaultActionGroup searchFieldActions,
                             @Nonnull DefaultActionGroup replaceToolbar1Actions,
                             @Nonnull DefaultActionGroup replaceToolbar2Actions,
                             @Nonnull DefaultActionGroup replaceFieldActions,
                             @Nullable Runnable replaceAction,
                             @Nullable Runnable closeAction,
                             @Nullable DataProvider dataProvider) {
    myProject = project;
    myTargetComponent = targetComponent;
    mySearchFieldActions = searchFieldActions;
    myReplaceFieldActions = replaceFieldActions;
    myReplaceAction = replaceAction;
    myCloseAction = closeAction;

    for (AnAction child : searchToolbar2Actions.getChildren(null)) {
      if (child instanceof Embeddable) {
        myEmbeddedSearchActions.add(child);
        ShortcutSet shortcutSet = ActionUtil.getMnemonicAsShortcut(child);
        if (shortcutSet != null) child.registerCustomShortcutSet(shortcutSet, this);
      }
    }
    for (AnAction action : myEmbeddedSearchActions) {
      searchToolbar2Actions.remove(action);
    }
    for (AnAction child : replaceToolbar2Actions.getChildren(null)) {
      if (child instanceof Embeddable) {
        myEmbeddedReplaceActions.add(child);
        ShortcutSet shortcutSet = ActionUtil.getMnemonicAsShortcut(child);
        if (shortcutSet != null) child.registerCustomShortcutSet(shortcutSet, this);
      }
    }
    for (AnAction action : myEmbeddedReplaceActions) {
      replaceToolbar2Actions.remove(action);
    }

    mySearchToolbar1PopupStateModifier = new ActionToolbarEx.PopupStateModifier() {
      @Override
      public int getModifiedPopupState() {
        return ActionButtonComponent.PUSHED;
      }

      @Override
      public boolean willModify() {
        return searchToolbar1ModifiedFlagGetter.get();
      }
    };

    mySearchFieldWrapper = new MyTextComponentWrapper() {
      @Override
      public void setContent(JComponent wrapped) {
        super.setContent(wrapped);
        mySearchTextComponent = unwrapTextComponent(wrapped);
      }
    };
    myReplaceFieldWrapper = new MyTextComponentWrapper() {
      @Override
      public void setContent(JComponent wrapped) {
        super.setContent(wrapped);
        myReplaceTextComponent = unwrapTextComponent(wrapped);
      }
    };
    myReplaceFieldWrapper.setBorder(JBUI.Borders.emptyTop(1));

    myLeftPanel = new JPanel(new GridBagLayout());
    myLeftPanel.setBackground(JBColor.border());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1;
    constraints.weighty = 1;
    myLeftPanel.add(mySearchFieldWrapper, constraints);
    constraints.gridy++;
    myLeftPanel.add(myReplaceFieldWrapper, constraints);
    myLeftPanel.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1));

    searchToolbar1Actions.addAll(searchToolbar2Actions.getChildren(null));
    replaceToolbar1Actions.addAll(replaceToolbar2Actions.getChildren(null));

    mySearchActionsToolbar = createSearchToolbar1(searchToolbar1Actions);
    mySearchActionsToolbar.setForceShowFirstComponent(true);
    JPanel searchPair = new NonOpaquePanel(new BorderLayout());
    searchPair.add(mySearchActionsToolbar.getComponent(), BorderLayout.CENTER);

    myReplaceActionsToolbar = createReplaceToolbar1(replaceToolbar1Actions);
    myReplaceActionsToolbar.getComponent().setBorder(JBUI.Borders.empty());
    Wrapper replaceToolbarWrapper1 = new Wrapper(myReplaceActionsToolbar.getComponent());
    myReplaceToolbarWrapper = new NonOpaquePanel(new BorderLayout());
    myReplaceToolbarWrapper.add(replaceToolbarWrapper1, BorderLayout.WEST);
    myReplaceToolbarWrapper.setBorder(JBUI.Borders.emptyTop(3));

    JLabel closeLabel = new JBLabel(null, AllIcons.Actions.Close, SwingConstants.RIGHT);
    closeLabel.setBorder(JBUI.Borders.empty(2));
    closeLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        close();
      }
    });
    closeLabel.setToolTipText(FindBundle.message("tooltip.close.search.bar.escape"));
    searchPair.add(new Wrapper(closeLabel), BorderLayout.EAST);

    myRightPanel = new NonOpaquePanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
    myRightPanel.add(searchPair);
    myRightPanel.add(myReplaceToolbarWrapper);

    OnePixelSplitter splitter = new OnePixelSplitter(false, .33F);
    myRightPanel.setBorder(JBUI.Borders.emptyLeft(6));
    splitter.setFirstComponent(myLeftPanel);
    splitter.setSecondComponent(myRightPanel);
    splitter.setHonorComponentsMinimumSize(true);
    splitter.setLackOfSpaceStrategy(Splitter.LackOfSpaceStrategy.HONOR_THE_SECOND_MIN_SIZE);
    splitter.setAndLoadSplitterProportionKey("FindSplitterProportion");
    splitter.setOpaque(false);
    splitter.getDivider().setOpaque(false);
    add(splitter, BorderLayout.CENTER);

    update("", "", false, false);

    // it's assigned after all action updates so that actions don't get access to uninitialized components
    myDataProviderDelegate = dataProvider;
    // A workaround to suppress editor-specific TabAction
    new DumbAwareAction() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        Component focusOwner = ProjectIdeFocusManager.getInstance(myProject).getFocusOwner();
        if (UIUtil.isAncestor(SearchReplaceComponentImpl.this, focusOwner)) focusOwner.transferFocus();
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), this);
    new DumbAwareAction() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        Component focusOwner = ProjectIdeFocusManager.getInstance(myProject).getFocusOwner();
        if (UIUtil.isAncestor(SearchReplaceComponentImpl.this, focusOwner)) focusOwner.transferFocusBackward();
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)), this);
  }

  @Override
  public void resetUndoRedoActions() {
    UIUtil.resetUndoRedoActions(mySearchTextComponent);
    UIUtil.resetUndoRedoActions(myReplaceTextComponent);
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void removeNotify() {
    super.removeNotify();

    addTextToRecent(mySearchTextComponent);
    if (myReplaceTextComponent != null) {
      addTextToRecent(myReplaceTextComponent);
    }
  }

  @Override
  public void requestFocusInTheSearchFieldAndSelectContent(Project project) {
    mySearchTextComponent.selectAll();
    ProjectIdeFocusManager.getInstance(project).requestFocus(mySearchTextComponent, true);
    if (myReplaceTextComponent != null) {
      myReplaceTextComponent.selectAll();
    }
  }

  @Override
  public void setStatusText(@Nonnull String status) {
    myStatusText = status;
  }

  @Override
  @Nonnull
  public String getStatusText() {
    return myStatusText;
  }

  @Override
  @Nonnull
  public Color getStatusColor() {
    return myStatusColor;
  }

  public void replace() {
    if (myReplaceAction != null) {
      myReplaceAction.run();
    }
  }

  public void close() {
    if (myCloseAction != null) {
      myCloseAction.run();
    }
  }

  @Override
  public void setRegularBackground() {
    mySearchTextComponent.setBackground(UIUtil.getTextFieldBackground());
    myStatusColor = UIUtil.getLabelForeground();
  }

  @Override
  public void setNotFoundBackground() {
    mySearchTextComponent.setBackground(LightColors.RED);
    myStatusColor = UIUtil.getErrorForeground();
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key dataId) {
    if (SpeedSearchSupply.SPEED_SEARCH_CURRENT_QUERY == dataId) {
      return mySearchTextComponent.getText();
    }
    if (TouchbarDataKeys.ACTIONS_KEY == dataId) {
      if (myTouchbarActions == null) {
        myTouchbarActions = new DefaultActionGroup();
        myTouchbarActions.add(new PrevOccurrenceAction());
        myTouchbarActions.add(new NextOccurrenceAction());
      }
      return myTouchbarActions;
    }
    return myDataProviderDelegate != null ? myDataProviderDelegate.getData(dataId) : null;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public String getSearchText() {
    return getSearchTextComponent().getText();
  }

  @Override
  public void setSearchText(String text) {
    getSearchTextComponent().setText(text);
  }

  @Override
  public void setReplaceText(String text) {
    getReplaceTextComponent().setText(text);
  }

  @Override
  public void selectSearchAll() {
    getSearchTextComponent().selectAll();
  }

  @Override
  public String getReplaceText() {
    return getReplaceTextComponent().getText();
  }

  @Override
  public void addListener(@Nonnull Listener listener) {
    myEventDispatcher.addListener(listener);
  }

  @Override
  public boolean isMultiline() {
    return myMultilineMode;
  }

  private void setMultilineInternal(boolean multiline) {
    boolean stateChanged = multiline != myMultilineMode;
    myMultilineMode = multiline;
    if (stateChanged) {
      myEventDispatcher.getMulticaster().multilineStateChanged();
    }
  }

  @Nonnull
  public JTextComponent getSearchTextComponent() {
    return mySearchTextComponent;
  }

  @Nonnull
  public JTextComponent getReplaceTextComponent() {
    return myReplaceTextComponent;
  }


  private void updateSearchComponent(@Nonnull String textToSet) {
    if (!updateTextComponent(true)) {
      replaceTextInTextComponentEnsuringSelection(textToSet, mySearchTextComponent);
      return;
    }

    mySearchTextComponent.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@Nonnull DocumentEvent e) {
        ApplicationManager.getApplication().invokeLater(() -> myEventDispatcher.getMulticaster().searchFieldDocumentChanged());
      }
    });

    mySearchTextComponent.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (StringUtil.isEmpty(mySearchTextComponent.getText())) {
          close();
        }
        else {
          ProjectIdeFocusManager.getInstance(myProject).requestFocus(myTargetComponent, true);
          addTextToRecent(mySearchTextComponent);
        }
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Platform.current().os().isMac() ? META_DOWN_MASK : CTRL_DOWN_MASK), JComponent.WHEN_FOCUSED);
    // make sure Enter is consumed by search text field, even if 'next occurrence' action is disabled
    // this is needed to e.g. avoid triggering a default button in containing dialog (see IDEA-128057)
    mySearchTextComponent.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), WHEN_FOCUSED);

    new VariantsCompletionAction(mySearchTextComponent); // It registers a shortcut set automatically on construction
  }

  private static void replaceTextInTextComponentEnsuringSelection(@Nonnull String textToSet, JTextComponent component) {
    String existingText = component.getText();
    if (!existingText.equals(textToSet)) {
      component.setText(textToSet);
      // textToSet should be selected even if we have no selection before (if we have the selection then setText will remain it)
      if (component.getSelectionStart() == component.getSelectionEnd()) component.selectAll();
    }
  }

  private void updateReplaceComponent(@Nonnull String textToSet) {
    if (!updateTextComponent(false)) {
      replaceTextInTextComponentEnsuringSelection(textToSet, myReplaceTextComponent);
      return;
    }
    myReplaceTextComponent.setText(textToSet);

    myReplaceTextComponent.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@Nonnull DocumentEvent e) {
        ApplicationManager.getApplication().invokeLater(() -> myEventDispatcher.getMulticaster().replaceFieldDocumentChanged());
      }
    });

    if (!isMultiline()) {
      installReplaceOnEnterAction(myReplaceTextComponent);
    }

    new VariantsCompletionAction(myReplaceTextComponent);
    myReplaceFieldWrapper.revalidate();
    myReplaceFieldWrapper.repaint();
  }

  @Override
  public void update(@Nonnull String findText, @Nonnull String replaceText, boolean replaceMode, boolean multiline) {
    setMultilineInternal(multiline);
    boolean needToResetSearchFocus = mySearchTextComponent != null && mySearchTextComponent.hasFocus();
    boolean needToResetReplaceFocus = myReplaceTextComponent != null && myReplaceTextComponent.hasFocus();
    updateSearchComponent(findText);
    updateReplaceComponent(replaceText);
    myReplaceFieldWrapper.setVisible(replaceMode);
    myReplaceToolbarWrapper.setVisible(replaceMode);
    if (needToResetReplaceFocus) myReplaceTextComponent.requestFocusInWindow();
    if (needToResetSearchFocus) mySearchTextComponent.requestFocusInWindow();
    updateBindings();
    updateActions();
    List<Component> focusOrder = new ArrayList<>();
    focusOrder.add(mySearchTextComponent);
    focusOrder.add(myReplaceTextComponent);
    focusOrder.addAll(myExtraSearchButtons);
    focusOrder.addAll(myExtraReplaceButtons);
    setFocusCycleRoot(true);
    setFocusTraversalPolicy(new ListFocusTraversalPolicy(focusOrder));
    revalidate();
    repaint();
  }

  @Override
  public void updateActions() {
    mySearchActionsToolbar.updateActionsImmediately();
    myReplaceActionsToolbar.updateActionsImmediately();
    JComponent textComponent = mySearchFieldWrapper.getTargetComponent();
    if (textComponent instanceof SearchTextArea) ((SearchTextArea)textComponent).updateExtraActions();
    textComponent = myReplaceFieldWrapper.getTargetComponent();
    if (textComponent instanceof SearchTextArea) ((SearchTextArea)textComponent).updateExtraActions();
  }

  public void addTextToRecent(@Nonnull JTextComponent textField) {
    addTextToRecent(textField.getText(), textField == mySearchTextComponent);
  }

  public void addTextToRecent(@Nonnull String text, boolean search) {
    if (text.length() > 0) {
      FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(myProject);
      if (search) {
        findInProjectSettings.addStringToFind(text);
        if (mySearchFieldWrapper.getTargetComponent() instanceof SearchTextField) {
          ((SearchTextField)mySearchFieldWrapper.getTargetComponent()).addCurrentTextToHistory();
        }
      }
      else {
        findInProjectSettings.addStringToReplace(text);
        if (myReplaceFieldWrapper.getTargetComponent() instanceof SearchTextField) {
          ((SearchTextField)myReplaceFieldWrapper.getTargetComponent()).addCurrentTextToHistory();
        }
      }
    }
  }

  @Override
  public void updateEmptyText(Supplier<String> textSupplier) {
    if (getSearchTextComponent() instanceof ComponentWithEmptyText) {
      ((ComponentWithEmptyText)getSearchTextComponent()).getEmptyText().setText(textSupplier.get());
    }
  }

  @Override
  public boolean isJustClearedSearch() {
    return UIUtil.isClientPropertyTrue(getSearchTextComponent(), SearchTextArea.JUST_CLEARED_KEY);
  }

  private boolean updateTextComponent(boolean search) {
    JTextComponent oldComponent = search ? mySearchTextComponent : myReplaceTextComponent;
    if (oldComponent != null) return false;
    final MyTextComponentWrapper wrapper = search ? mySearchFieldWrapper : myReplaceFieldWrapper;

    final JBTextArea textComponent = new JBTextArea();
    textComponent.setRows(isMultiline() ? 2 : 1);
    textComponent.setColumns(12);
    if (search) {
      textComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.search.accessible.name"));
    }
    else {
      textComponent.getAccessibleContext().setAccessibleName(FindBundle.message("find.replace.accessible.name"));
    }
    SearchTextArea textArea = new SearchTextArea(textComponent, search);
    if (search) {
      myExtraSearchButtons.clear();
      myExtraSearchButtons.addAll(textArea.setExtraActions(myEmbeddedSearchActions.toArray(AnAction.EMPTY_ARRAY)));
    }
    else {
      myExtraReplaceButtons.clear();
      myExtraReplaceButtons.addAll(textArea.setExtraActions(myEmbeddedReplaceActions.toArray(AnAction.EMPTY_ARRAY)));
    }
    // Display empty text only when focused
    textComponent.putClientProperty("StatusVisibleFunction",
                                    (BooleanFunction<JTextComponent>)(c -> c.getText().isEmpty() && c.isFocusOwner()));

    wrapper.setContent(textArea);

    UIUtil.addUndoRedoActions(textComponent);

    textComponent.putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, Boolean.TRUE);
    textComponent.setBackground(UIUtil.getTextFieldBackground());
    textComponent.addFocusListener(new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {
        textComponent.repaint();
      }

      @Override
      public void focusLost(final FocusEvent e) {
        textComponent.repaint();
      }
    });
    new CloseAction() {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        close();
      }
    }.registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_EDITOR_ESCAPE), textArea);
    return true;
  }

  private abstract static class CloseAction extends DumbAwareAction {
  }

  private void installReplaceOnEnterAction(@Nonnull JTextComponent c) {
    ActionListener action = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        replace();
      }
    };
    c.registerKeyboardAction(action, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
  }

  private void updateBindings() {
    updateBindings(mySearchFieldActions, mySearchFieldWrapper);
    updateBindings(mySearchActionsToolbar, mySearchFieldWrapper);

    updateBindings(myReplaceFieldActions, myReplaceFieldWrapper);
    updateBindings(myReplaceActionsToolbar, myReplaceToolbarWrapper);
  }

  private void updateBindings(@Nonnull DefaultActionGroup group, @Nonnull JComponent shortcutHolder) {
    updateBindings(ContainerUtil.immutableList(group.getChildActionsOrStubs()), shortcutHolder);
  }

  private void updateBindings(@Nonnull ActionToolbar toolbar, @Nonnull JComponent shortcutHolder) {
    updateBindings(toolbar.getActions(), shortcutHolder);
  }

  private void updateBindings(@Nonnull List<? extends AnAction> actions, @Nonnull JComponent shortcutHolder) {
    DataContext context = DataManager.getInstance().getDataContext(this);
    for (AnAction action : actions) {
      ShortcutSet shortcut = null;
      if (action instanceof ContextAwareShortcutProvider) {
        shortcut = ((ContextAwareShortcutProvider)action).getShortcut(context);
      }
      else if (action instanceof ShortcutProvider) {
        shortcut = ((ShortcutProvider)action).getShortcut();
      }
      if (shortcut != null) {
        action.registerCustomShortcutSet(shortcut, shortcutHolder);
      }
    }
  }


  @Nonnull
  private ActionToolbar createSearchToolbar1(@Nonnull DefaultActionGroup group) {
    ActionToolbarEx toolbar = createToolbar(group);
    toolbar.setSecondaryButtonPopupStateModifier(mySearchToolbar1PopupStateModifier);
    toolbar.setSecondaryActionsTooltip(FindBundle.message("find.popup.show.filter.popup"));
    toolbar.setSecondaryActionsIcon(AllIcons.General.Filter);
    toolbar.setNoGapMode();

    KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
    if (keyboardShortcut != null) {
      toolbar.setSecondaryActionsShortcut(KeymapUtil.getShortcutText(keyboardShortcut));
    }

    new ShowMoreOptions(toolbar, mySearchFieldWrapper);
    return toolbar;
  }

  @Nonnull
  private ActionToolbar createReplaceToolbar1(@Nonnull DefaultActionGroup group) {
    ActionToolbar toolbar = createToolbar(group);
    toolbar.setForceMinimumSize(true);
    toolbar.setReservePlaceAutoPopupIcon(false);
    return toolbar;
  }

  @Nonnull
  private ActionToolbarEx createToolbar(@Nonnull ActionGroup group) {
    ActionToolbarEx toolbar = (ActionToolbarEx)ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, group, true);
    toolbar.setTargetComponent(this);
    toolbar.setLayoutPolicy(ActionToolbar.AUTO_LAYOUT_POLICY);
    Utils.setSmallerFontForChildren(toolbar.getComponent());
    return toolbar;
  }

  @SuppressWarnings("HardCodedStringLiteral")


  private static class MyTextComponentWrapper extends Wrapper {
    @Nullable
    public JTextComponent getTextComponent() {
      JComponent wrapped = getTargetComponent();
      return wrapped != null ? unwrapTextComponent(wrapped) : null;
    }

    @Nonnull
    protected static JTextComponent unwrapTextComponent(@Nonnull JComponent wrapped) {
      if (wrapped instanceof SearchTextField) {
        return ((SearchTextField)wrapped).getTextEditor();
      }
      if (wrapped instanceof SearchTextArea) {
        return ((SearchTextArea)wrapped).getTextArea();
      }
      throw new AssertionError();
    }
  }
}
