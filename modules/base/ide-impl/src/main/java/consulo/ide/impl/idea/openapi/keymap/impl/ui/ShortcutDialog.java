// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.keymap.impl.ui;

import consulo.application.AllIcons;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.QuickList;
import consulo.ide.impl.idea.ui.components.GradientViewport;
import consulo.language.editor.CommonDataKeys;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.Shortcut;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapGroup;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

abstract class ShortcutDialog<T extends Shortcut> extends DialogWrapper {
    private final SimpleColoredComponent myAction = new SimpleColoredComponent();
    private final JBPanel myConflictsContainer = new JBPanel(new VerticalLayout(0));
    private final JBPanel myConflictsPanel = new JBPanel(new BorderLayout())
        .withBorder(JBUI.Borders.empty(10, 10, 0, 10))
        .withPreferredHeight(64)
        .withMinimumHeight(64);

    protected final ShortcutPanel<T> myShortcutPanel;
    private final Project myProject;
    private String myActionId;
    private Keymap myKeymap;
    private KeymapGroup myGroup;

    ShortcutDialog(Component parent, LocalizeValue titleKey, ShortcutPanel<T> panel) {
        super(parent, true);
        myShortcutPanel = panel;
        myProject = DataManager.getInstance().getDataContext(parent).getData(CommonDataKeys.PROJECT);
        setTitle(titleKey);
    }

    String getActionPath(String actionId) {
        return myGroup == null ? null : myGroup.getActionQualifiedPath(actionId, true);
    }

    boolean hasConflicts() {
        return myConflictsPanel.isVisible();
    }

    abstract @Nonnull Collection<String> getConflicts(T shortcut, String actionId, Keymap keymap);

    abstract T toShortcut(Object value);

    void setShortcut(T shortcut) {
        setOKActionEnabled(shortcut != null);
        if (!equal(shortcut, myShortcutPanel.getShortcut())) {
            myShortcutPanel.setShortcut(shortcut);
        }
        myConflictsContainer.removeAll();
        if (shortcut != null) {
            for (String id : getConflicts(shortcut, myActionId, myKeymap)) {
                String path = id.equals(myActionId) ? null : getActionPath(id);
                if (path != null) {
                    SimpleColoredComponent component = new SimpleColoredComponent();
                    fill(component, id, path);
                    if (ScreenReader.isActive()) {
                        // Supports TAB/Shift-TAB navigation
                        component.setFocusable(true);
                    }
                    myConflictsContainer.add(VerticalLayout.TOP, component);
                }
            }
            myConflictsPanel.revalidate();
            myConflictsPanel.repaint();
        }
        myConflictsPanel.setVisible(0 < myConflictsContainer.getComponentCount());
    }

    T showAndGet(String id, Keymap keymap, QuickList... lists) {
        return showAndGet(id, keymap, null, lists);
    }

    T showAndGet(String id, Keymap keymap, @Nullable T selectedShortcut, QuickList... lists) {
        myActionId = id;
        myKeymap = keymap;
        myGroup = ActionsTreeUtil.createMainGroup(myProject, keymap, lists, null, false, null);
        addSystemActionsIfPresented(myGroup);
        fill(myAction, id, getActionPath(id));
        if (selectedShortcut == null) {
            for (Shortcut shortcut : keymap.getShortcuts(id)) {
                selectedShortcut = toShortcut(shortcut);
                if (selectedShortcut != null) break;
            }
        }
        setShortcut(selectedShortcut);
        return showAndGet() ? myShortcutPanel.getShortcut() : null;
    }

    protected void addSystemActionsIfPresented(KeymapGroup group) {
    }

    @Override
    protected @Nullable Border createContentPaneBorder() {
        return JBUI.Borders.empty();
    }

    @Override
    protected @Nullable JComponent createSouthPanel() {
        JComponent panel = super.createSouthPanel();
        if (panel != null) {
            panel.setBorder(JBUI.Borders.empty(8, 12));
        }
        return panel;
    }

    @Override
    protected @Nullable JComponent createNorthPanel() {
        myAction.setIpad(JBUI.insets(10, 10, 5, 10));
        myShortcutPanel.addPropertyChangeListener("shortcut", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                setShortcut(toShortcut(event.getNewValue()));
            }
        });
        JBPanel result = new JBPanel(new BorderLayout()).withPreferredWidth(300).withMinimumWidth(200);
        result.add(BorderLayout.NORTH, myAction);
        result.add(BorderLayout.SOUTH, myShortcutPanel);
        return result;
    }

    @Override
    protected JComponent createCenterPanel() {
        JLabel icon = new JLabel(TargetAWT.to(AllIcons.General.BalloonWarning));
        icon.setVerticalAlignment(SwingConstants.TOP);

        JLabel label = new JLabel(KeyMapLocalize.dialogConflictsText().get());
        label.setBorder(JBUI.Borders.emptyLeft(2));
        if (ScreenReader.isActive()) {
            // Supports TAB/Shift-TAB navigation
            label.setFocusable(true);
        }

        JScrollPane scroll = ScrollPaneFactory.createScrollPane(null, true);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setViewport(new GradientViewport(myConflictsContainer, JBUI.insets(5), false));
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        JBPanel panel = new JBPanel(new BorderLayout());
        panel.add(BorderLayout.NORTH, label);
        panel.add(BorderLayout.CENTER, scroll);

        myConflictsPanel.add(BorderLayout.WEST, icon);
        myConflictsPanel.add(BorderLayout.CENTER, panel);
        myConflictsContainer.setOpaque(false);
        return myConflictsPanel;
    }

    private static boolean equal(Shortcut newShortcut, Shortcut oldShortcut) {
        return newShortcut == null ? oldShortcut == null : newShortcut.equals(oldShortcut);
    }

    private static void fill(SimpleColoredComponent component, String id, String path) {
        if (path == null) {
            component.append(id, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES);
        }
        else {
            int index = path.lastIndexOf(" | ");
            if (index < 0) {
                component.append(path, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
            else {
                component.append(path.substring(index + 3), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                component.append(" " + KeyMapLocalize.shortcutInGroupText(path.substring(0, index)).get(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }
    }
}
