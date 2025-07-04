// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.find;

import consulo.application.AllIcons;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.EditorCopyPasteHelper;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.find.FindInProjectSettings;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.editorHeaderActions.Utils;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.awt.event.InputEvent.*;
import static javax.swing.ScrollPaneConstants.*;

public class SearchTextArea extends JPanel implements PropertyChangeListener {
    public static final String JUST_CLEARED_KEY = "JUST_CLEARED";
    public static final KeyStroke NEW_LINE_KEYSTROKE =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (Platform.current().os().isMac() ? META_DOWN_MASK : CTRL_DOWN_MASK) | SHIFT_DOWN_MASK);
    private final JTextArea myTextArea;
    private final boolean mySearchMode;

    private final JBScrollPane myScrollPane;
    private boolean myMultilineEnabled = true;

    private final DefaultActionGroup mySuffixToolbarGroup;
    private final ActionToolbar myPrefixToolbar;
    private final ActionToolbar mySuffixToolbar;

    public SearchTextArea(@Nonnull JTextArea textArea, boolean searchMode) {
        super(new BorderLayout());
        myTextArea = textArea;
        mySearchMode = searchMode;
        updateFont();

        myTextArea.addPropertyChangeListener("background", this);
        myTextArea.addPropertyChangeListener("font", this);
        DumbAwareAction.create(event -> myTextArea.transferFocus())
            .registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), myTextArea);
        DumbAwareAction.create(event -> myTextArea.transferFocusBackward())
            .registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, SHIFT_DOWN_MASK)), myTextArea);
        KeymapUtil.reassignAction(myTextArea, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), NEW_LINE_KEYSTROKE, WHEN_FOCUSED);
        myTextArea.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                if (getProperty("filterNewlines") == Boolean.TRUE && str.indexOf('\n') >= 0) {
                    str = StringUtil.replace(str, "\n", " ");
                }
                if (!StringUtil.isEmpty(str)) {
                    super.insertString(offs, str, a);
                }
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
                updateScrollBar();
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

        myTextArea.setBorder(JBUI.Borders.empty(4, 0, 0, 0));

        myScrollPane.getViewport().setBorder(null);
        myScrollPane.getViewport().setOpaque(false);
        myScrollPane.setOpaque(false);

        ActionManager actionManager = ActionManager.getInstance();
        mySuffixToolbarGroup = new DefaultActionGroup();
        mySuffixToolbarGroup.add(new ClearAction(), actionManager);
        mySuffixToolbarGroup.add(new NewLineAction(), actionManager);

        myPrefixToolbar = ActionManager.getInstance().createActionToolbar("SearchPrefixHistoryToolbar",
            ActionGroup.newImmutableBuilder().add(new ShowHistoryAction()).build(),
            true
        );
        myPrefixToolbar.setTargetComponent(this);

        mySuffixToolbar = ActionManager.getInstance().createActionToolbar("SearchSuffixHistoryToolbar",
            mySuffixToolbarGroup,
            true
        );
        mySuffixToolbar.setTargetComponent(this);

        setBorder(JBUI.Borders.empty());

        JComponent historyComponent = myPrefixToolbar.getComponent();
        historyComponent.setOpaque(false);

        add(historyComponent, BorderLayout.WEST);
        add(myScrollPane, BorderLayout.CENTER);

        JComponent suffixComponent = mySuffixToolbar.getComponent();
        suffixComponent.setOpaque(false);

        add(suffixComponent, BorderLayout.EAST);

        updateScrollBar();
    }

    @RequiredUIAccess
    @Nonnull
    public CompletableFuture<?> updateAllAsync() {
        return CompletableFuture.allOf(myPrefixToolbar.updateActionsAsync(), mySuffixToolbar.updateActionsAsync());
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

    private void updateScrollBar() {
        boolean multiline = StringUtil.getLineBreakCount(myTextArea.getText()) > 0;

        myScrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_AS_NEEDED);
        myScrollPane.setVerticalScrollBarPolicy(multiline ? VERTICAL_SCROLLBAR_AS_NEEDED : VERTICAL_SCROLLBAR_NEVER);
        myScrollPane.getHorizontalScrollBar().setVisible(multiline);
        myScrollPane.revalidate();
    }

    @RequiredUIAccess
    public void setSuffixActions(List<AnAction> actions) {
        mySuffixToolbarGroup.addAll(actions, ActionManager.getInstance());

        mySuffixToolbar.updateActionsAsync();
    }

    @RequiredUIAccess
    public void updateExtraActions() {
        mySuffixToolbar.updateActionsAsync();
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
        if (myMultilineEnabled == enabled) {
            return;
        }

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
        updateScrollBar();
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
        else if ("font".equals(evt.getPropertyName())) {
            updateScrollBar();
        }
    }

    private class ShowHistoryAction extends DumbAwareAction {

        ShowHistoryAction() {
            super(
                mySearchMode ? FindLocalize.findSearchHistory() : FindLocalize.findReplaceHistory(),
                mySearchMode ? FindLocalize.findSearchHistory() : FindLocalize.findReplaceHistory(),
                AllIcons.Actions.SearchWithHistory
            );
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
            super(PlatformIconGroup.actionsCancel());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myTextArea.putClientProperty(JUST_CLEARED_KEY, !myTextArea.getText().isEmpty());
            myTextArea.setText("");
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(!StringUtil.isEmpty(myTextArea.getText()));
        }
    }

    private class NewLineAction extends DumbAwareAction {
        NewLineAction() {
            super(FindLocalize.findNewLine(), LocalizeValue.empty(), AllIcons.Actions.SearchNewLine);
            setShortcutSet(new CustomShortcutSet(NEW_LINE_KEYSTROKE));
            getTemplatePresentation().setHoveredIcon(AllIcons.Actions.SearchNewLineHover);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            new DefaultEditorKit.InsertBreakAction().actionPerformed(new ActionEvent(myTextArea, 0, "action"));
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myMultilineEnabled);
        }
    }
}
