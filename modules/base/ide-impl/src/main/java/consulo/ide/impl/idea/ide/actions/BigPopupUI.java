// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ui.WindowMoveListener;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.ui.awt.EditorAWTUtil;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.project.Project;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public abstract class BigPopupUI extends BorderLayoutPanel implements Disposable {
    private static final int MINIMAL_SUGGESTIONS_LIST_HEIGHT = 100;

    protected final Project myProject;
    protected TextBoxWithExtensions mySearchField;
    protected JPanel suggestionsPanel;
    protected JBList<Object> myResultsList;
    protected JBPopup myHint;
    protected Runnable searchFinishedHandler = () -> {
    };
    protected final List<ViewTypeListener> myViewTypeListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    protected ViewType myViewType = ViewType.SHORT;
    protected JLabel myHintLabel;

    public BigPopupUI(Project project) {
        myProject = project;
    }

    @Nonnull
    public abstract JBList<Object> createList();

    @Nonnull
    protected abstract ListCellRenderer<Object> createCellRenderer();

    @Nonnull
    protected abstract JPanel createTopLeftPanel();

    @Nonnull
    protected abstract JComponent createSettingsPanel();

    @Nonnull
    protected abstract String getInitialHint();

    protected void installScrollingActions() {
        ScrollingUtil.installActions(myResultsList, (JComponent) TargetAWT.to(getSearchField()));
    }

    @Nonnull
    protected TextBoxWithExtensions createSearchField() {
        return TextBoxWithExtensions.create();
    }

    public void init() {
        withBackground(JBCurrentTheme.BigPopup.headerBackground());

        myResultsList = createList();

        JPanel topLeftPanel = createTopLeftPanel();
        JComponent settingsPanel = createSettingsPanel();
        mySearchField = createSearchField();
        mySearchField.addBorders(BorderStyle.EMPTY, null, 4);
        (TargetAWT.to(mySearchField)).setFocusTraversalKeysEnabled(false);
        suggestionsPanel = createSuggestionsPanel();

        myResultsList.setFocusable(false);
        myResultsList.setCellRenderer(createCellRenderer());

        if (Registry.is("new.search.everywhere.use.editor.font")) {
            Font editorFont = EditorAWTUtil.getEditorFont();
            myResultsList.setFont(editorFont);
        }

        installScrollingActions();

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(topLeftPanel, BorderLayout.WEST);
        top.add(settingsPanel, BorderLayout.EAST);
        top.setBorder(JBUI.Borders.customLine(JBCurrentTheme.BigPopup.searchFieldBorderColor(), 0, 0, 1, 0));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(top, BorderLayout.NORTH);
        topPanel.add(TargetAWT.to(mySearchField), BorderLayout.SOUTH);

        WindowMoveListener moveListener = new WindowMoveListener(this);
        topPanel.addMouseListener(moveListener);
        topPanel.addMouseMotionListener(moveListener);

        addToTop(topPanel);
        addToCenter(suggestionsPanel);

        MnemonicHelper.init(this);
    }

    protected void addListDataListener(@Nonnull AbstractListModel<Object> model) {
        model.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                updateViewType(ViewType.FULL);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                if (myResultsList.isEmpty() && getSearchPattern().isEmpty()) {
                    updateViewType(ViewType.SHORT);
                }
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                updateViewType(myResultsList.isEmpty() && getSearchPattern().isEmpty() ? ViewType.SHORT : ViewType.FULL);
            }
        });
    }

    @Nonnull
    protected String getSearchPattern() {
        return Optional.ofNullable(mySearchField).map(TextBoxWithExtensions::getValue).orElse("");
    }

    protected void updateViewType(@Nonnull ViewType viewType) {
        if (myViewType != viewType) {
            myViewType = viewType;
            myViewTypeListeners.forEach(listener -> listener.suggestionsShown(viewType));
        }
    }

    private JPanel createSuggestionsPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setBorder(JBUI.Borders.customLine(JBCurrentTheme.BigPopup.searchFieldBorderColor(), 1, 0, 0, 0));

        JScrollPane resultsScroll = new JBScrollPane(myResultsList);
        resultsScroll.setBorder(null);
        resultsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        UIUtil.putClientProperty(resultsScroll.getVerticalScrollBar(), JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, true);

        resultsScroll.setPreferredSize(JBUI.size(670, JBCurrentTheme.BigPopup.maxListHeight()));
        pnl.add(resultsScroll, BorderLayout.CENTER);

        myHintLabel = createHint();
        pnl.add(myHintLabel, BorderLayout.SOUTH);

        return pnl;
    }

    @Nonnull
    private JLabel createHint() {
        String hint = getInitialHint();
        JLabel hintLabel = HintUtil.createAdComponent(hint, JBCurrentTheme.BigPopup.advertiserBorder(), SwingConstants.LEFT);
        hintLabel.setForeground(JBCurrentTheme.BigPopup.advertiserForeground());
        hintLabel.setBackground(JBCurrentTheme.BigPopup.advertiserBackground());
        hintLabel.setOpaque(true);
        Dimension size = hintLabel.getPreferredSize();
        size.height = JBUIScale.scale(17);
        hintLabel.setPreferredSize(size);
        return hintLabel;
    }

    @Nonnull
    public TextBoxWithExtensions getSearchField() {
        return mySearchField;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size = calcPrefSize(ViewType.SHORT);
        if (getViewType() == ViewType.FULL) {
            size.height += MINIMAL_SUGGESTIONS_LIST_HEIGHT;
        }
        return size;
    }

    @Override
    public Dimension getPreferredSize() {
        return calcPrefSize(myViewType);
    }

    public Dimension getExpandedSize() {
        return calcPrefSize(ViewType.FULL);
    }

    private Dimension calcPrefSize(ViewType viewType) {
        Dimension size = super.getPreferredSize();
        if (viewType == ViewType.SHORT) {
            size.height -= suggestionsPanel.getPreferredSize().height;
        }
        return size;
    }

    public void setSearchFinishedHandler(@Nonnull Runnable searchFinishedHandler) {
        this.searchFinishedHandler = searchFinishedHandler;
    }

    public ViewType getViewType() {
        return myViewType;
    }

    public enum ViewType {
        FULL,
        SHORT
    }

    public interface ViewTypeListener {
        void suggestionsShown(@Nonnull ViewType viewType);
    }

    public void addViewTypeListener(ViewTypeListener listener) {
        myViewTypeListeners.add(listener);
    }

    public void removeViewTypeListener(ViewTypeListener listener) {
        myViewTypeListeners.remove(listener);
    }
}