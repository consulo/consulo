// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.find;

import consulo.application.AllIcons;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.EditorCopyPasteHelper;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.action.ActionButtonImpl;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.find.FindBundle;
import consulo.find.FindInProjectSettings;
import consulo.ide.impl.idea.find.editorHeaderActions.Utils;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import static java.awt.event.InputEvent.*;
import static javax.swing.ScrollPaneConstants.*;

public class SearchTextArea extends JPanel implements PropertyChangeListener {
  public static final String JUST_CLEARED_KEY = "JUST_CLEARED";
  public static final KeyStroke NEW_LINE_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (Platform.current().os().isMac() ? META_DOWN_MASK : CTRL_DOWN_MASK) | SHIFT_DOWN_MASK);
  private final JTextArea myTextArea;
  private final boolean mySearchMode;
  private final JPanel myIconsPanel = new NonOpaquePanel();
  private final ActionButtonImpl myNewLineButton;
  private final ActionButtonImpl myClearButton;
  private final NonOpaquePanel myExtraActionsPanel = new NonOpaquePanel();
  private final JBScrollPane myScrollPane;
  private final ActionButtonImpl myHistoryPopupButton;
  private boolean myMultilineEnabled = true;

  @Deprecated
  public SearchTextArea(boolean searchMode) {
    this(new JBTextArea(), searchMode);
  }

  @Deprecated
  public SearchTextArea(@Nonnull JTextArea textArea, boolean searchMode, boolean infoMode) {
    this(textArea, searchMode);
  }

  @Deprecated
  public SearchTextArea(@Nonnull JTextArea textArea, boolean searchMode, boolean infoMode, boolean allowInsertTabInMultiline) {
    this(textArea, searchMode);
  }

  public SearchTextArea(@Nonnull JTextArea textArea, boolean searchMode) {
    myTextArea = textArea;
    mySearchMode = searchMode;
    updateFont();

    myTextArea.addPropertyChangeListener("background", this);
    myTextArea.addPropertyChangeListener("font", this);
    DumbAwareAction.create(event -> myTextArea.transferFocus()).registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), myTextArea);
    DumbAwareAction.create(event -> myTextArea.transferFocusBackward()).registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, SHIFT_DOWN_MASK)), myTextArea);
    KeymapUtil.reassignAction(myTextArea, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), NEW_LINE_KEYSTROKE, WHEN_FOCUSED);
    myTextArea.setDocument(new PlainDocument() {
      @Override
      public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if (getProperty("filterNewlines") == Boolean.TRUE && str.indexOf('\n') >= 0) {
          str = StringUtil.replace(str, "\n", " ");
        }
        if (!StringUtil.isEmpty(str)) super.insertString(offs, str, a);
      }
    });
    if (Registry.is("ide.find.field.trims.pasted.text", false)) {
      myTextArea.getDocument().putProperty(EditorCopyPasteHelper.TRIM_TEXT_ON_PASTE_KEY, Boolean.TRUE);
    }
    myTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@Nonnull DocumentEvent e) {
        if (e.getType() == DocumentEvent.EventType.INSERT) {
          myTextArea.putClientProperty(JUST_CLEARED_KEY, null);
        }
        int rows = Math.min(Registry.get("ide.find.max.rows").asInteger(), myTextArea.getLineCount());
        myTextArea.setRows(Math.max(1, Math.min(25, rows)));
        updateIconsLayout();
      }
    });
    myTextArea.setOpaque(false);
    myScrollPane = new JBScrollPane(myTextArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
      @Override
      protected void setupCorners() {
        super.setupCorners();
        setBorder(JBUI.Borders.empty(2, 0, 2, 2));
      }

      @Override
      public void updateUI() {
        super.updateUI();
        setBorder(JBUI.Borders.empty(2, 0, 2, 2));
      }
    };
    myTextArea.setBorder(new Border() {
      @Override
      public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      }

      @Override
      public Insets getBorderInsets(Component c) {
        if (Platform.current().os().isMac()) {
          return new JBInsets(3, 0, 2, 0);
        }
        else {
          int bottom = (StringUtil.getLineBreakCount(myTextArea.getText()) > 0) ? 2 : UIUtil.isUnderDarcula() ? 1 : 0;
          int top = myTextArea.getFontMetrics(myTextArea.getFont()).getHeight() <= 16 ? 2 : 1;
          if (JBUIScale.isUsrHiDPI()) {
            bottom = 0;
            top = 2;
          }
          return new JBInsets(top, 0, bottom, 0);
        }
      }

      @Override
      public boolean isBorderOpaque() {
        return false;
      }
    });
    myScrollPane.getViewport().setBorder(null);
    myScrollPane.getViewport().setOpaque(false);
    myScrollPane.setOpaque(false);

    myHistoryPopupButton = new MyActionButton(new ShowHistoryAction(), false);
    myClearButton = new MyActionButton(new ClearAction(), false);
    myNewLineButton = new MyActionButton(new NewLineAction(), false);

    updateLayout();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    updateFont();
    setBackground(UIUtil.getTextFieldBackground());
  }

  private void updateFont() {
    if (myTextArea != null) {
      if (Registry.is("ide.find.use.editor.font", false)) {
        myTextArea.setFont(EditorUtil.getEditorFont());
      }
      else {
        myTextArea.setFont(UIManager.getFont("TextField.font"));
      }
    }
  }

  protected void updateLayout() {
    JPanel historyButtonWrapper = new NonOpaquePanel(new BorderLayout());
    historyButtonWrapper.setBorder(JBUI.Borders.emptyTop(1));
    historyButtonWrapper.add(myHistoryPopupButton, BorderLayout.NORTH);
    JPanel iconsPanelWrapper = new NonOpaquePanel(new BorderLayout());
    iconsPanelWrapper.setBorder(JBUI.Borders.emptyTop(1));
    JPanel p = new NonOpaquePanel(new BorderLayout());
    p.add(myIconsPanel, BorderLayout.NORTH);
    iconsPanelWrapper.add(p, BorderLayout.WEST);
    iconsPanelWrapper.add(myExtraActionsPanel, BorderLayout.CENTER);

    removeAll();
    setLayout(new BorderLayout(JBUIScale.scale(3), 0));
    setBorder(JBUI.Borders.empty(Platform.current().os().isLinux() ? JBUI.scale(2) : JBUI.scale(1)));
    add(historyButtonWrapper, BorderLayout.WEST);
    add(myScrollPane, BorderLayout.CENTER);
    add(iconsPanelWrapper, BorderLayout.EAST);
    updateIconsLayout();
  }

  private void updateIconsLayout() {
    if (myIconsPanel.getParent() == null) {
      return;
    }

    boolean showClearIcon = !StringUtil.isEmpty(myTextArea.getText());
    boolean showNewLine = myMultilineEnabled;
    boolean wrongVisibility = ((myClearButton.getParent() == null) == showClearIcon) || ((myNewLineButton.getParent() == null) == showNewLine);

    boolean multiline = StringUtil.getLineBreakCount(myTextArea.getText()) > 0;
    if (wrongVisibility) {
      myIconsPanel.removeAll();
      myIconsPanel.setLayout(new BorderLayout());
      myIconsPanel.add(myClearButton, BorderLayout.CENTER);
      myIconsPanel.add(myNewLineButton, BorderLayout.EAST);
      myIconsPanel.setPreferredSize(myIconsPanel.getPreferredSize());
      if (!showClearIcon) myIconsPanel.remove(myClearButton);
      if (!showNewLine) myIconsPanel.remove(myNewLineButton);
      myIconsPanel.revalidate();
      myIconsPanel.repaint();
    }
    myScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
    myScrollPane.setVerticalScrollBarPolicy(multiline ? VERTICAL_SCROLLBAR_AS_NEEDED : VERTICAL_SCROLLBAR_NEVER);
    myScrollPane.getHorizontalScrollBar().setVisible(multiline);
    myScrollPane.revalidate();
    doLayout();
  }

  public List<Component> setExtraActions(AnAction... actions) {
    myExtraActionsPanel.removeAll();
    myExtraActionsPanel.setBorder(JBUI.Borders.empty());
    ArrayList<Component> addedButtons = new ArrayList<>();
    if (actions != null && actions.length > 0) {
      JPanel buttonsGrid = new NonOpaquePanel(new GridLayout(1, actions.length, 0, 0));
      for (AnAction action : actions) {
        ActionButtonImpl button = new MyActionButton(action, true);
        addedButtons.add(button);
        buttonsGrid.add(button);
      }
      myExtraActionsPanel.setLayout(new BorderLayout());
      myExtraActionsPanel.add(buttonsGrid, BorderLayout.NORTH);
      myExtraActionsPanel.setBorder(new CompoundBorder(JBUI.Borders.customLine(JBCurrentTheme.CustomFrameDecorations.separatorForeground(), 0, 1, 0, 0), JBUI.Borders.emptyLeft(4)));
    }
    return addedButtons;
  }

  public void updateExtraActions() {
    for (ActionButtonImpl button : UIUtil.findComponentsOfType(myExtraActionsPanel, ActionButtonImpl.class)) {
      button.update();
    }
  }

  private final KeyAdapter myEnterRedispatcher = new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER && SearchTextArea.this.getParent() != null) {
        SearchTextArea.this.getParent().dispatchEvent(e);
      }
    }
  };

  public void setMultilineEnabled(boolean enabled) {
    if (myMultilineEnabled == enabled) return;

    myMultilineEnabled = enabled;
    myTextArea.getDocument().putProperty("filterNewlines", myMultilineEnabled ? null : Boolean.TRUE);
    if (!myMultilineEnabled) {
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift UP"), "selection-begin-line");
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift DOWN"), "selection-end-line");
      myTextArea.addKeyListener(myEnterRedispatcher);
    }
    else {
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift UP"), "selection-up");
      myTextArea.getInputMap().put(KeyStroke.getKeyStroke("shift DOWN"), "selection-down");
      myTextArea.removeKeyListener(myEnterRedispatcher);
    }
    updateIconsLayout();
  }

  @Nonnull
  public JTextArea getTextArea() {
    return myTextArea;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if ("background".equals(evt.getPropertyName())) {
      repaint();
    }
    if ("font".equals(evt.getPropertyName())) {
      updateLayout();
    }
  }

  private class ShowHistoryAction extends DumbAwareAction {

    ShowHistoryAction() {
      super(FindBundle.message(mySearchMode ? "find.search.history" : "find.replace.history"), FindBundle.message(mySearchMode ? "find.search.history" : "find.replace.history"),
            AllIcons.Actions.SearchWithHistory);
      registerCustomShortcutSet(KeymapUtil.getActiveKeymapShortcuts("ShowSearchHistory"), myTextArea);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed("find.recent.search");
      FindInProjectSettings findInProjectSettings = FindInProjectSettings.getInstance(e.getData(Project.KEY));
      String[] recent = mySearchMode ? findInProjectSettings.getRecentFindStrings() : findInProjectSettings.getRecentReplaceStrings();
      JBList<String> historyList = new JBList<>(ArrayUtil.reverseArray(recent));
      Utils.showCompletionPopup(SearchTextArea.this, historyList, null, myTextArea, null);
    }
  }

  private class ClearAction extends DumbAwareAction {
    ClearAction() {
      super(AllIcons.Actions.Close);
      getTemplatePresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      myTextArea.putClientProperty(JUST_CLEARED_KEY, !myTextArea.getText().isEmpty());
      myTextArea.setText("");
    }
  }

  private class NewLineAction extends DumbAwareAction {
    NewLineAction() {
      super(FindBundle.message("find.new.line"), null, AllIcons.Actions.SearchNewLine);
      setShortcutSet(new CustomShortcutSet(NEW_LINE_KEYSTROKE));
      getTemplatePresentation().setHoveredIcon(AllIcons.Actions.SearchNewLineHover);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
      new DefaultEditorKit.InsertBreakAction().actionPerformed(new ActionEvent(myTextArea, 0, "action"));
    }
  }

  private static class MyActionButton extends ActionButtonImpl {

    private MyActionButton(@Nonnull AnAction action, boolean focusable) {
      super(action, action.getTemplatePresentation().clone(), ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);

      setFocusable(focusable);
      updateIcon();

      setDecorateButtons(false);
      setMinimalMode(true);
    }

    @Override
    protected DataContext getDataContext() {
      return DataManager.getInstance().getDataContext(this);
    }

    @Override
    public int getPopState() {
      return isSelected() ? SELECTED : super.getPopState();
    }

    @Override
    public Image getIcon() {
      if (isEnabled() && isSelected()) {
        Image selectedIcon = myPresentation.getSelectedIcon();
        if (selectedIcon != null) return selectedIcon;
      }
      return super.getIcon();
    }
  }
}
