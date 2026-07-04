/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 *
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.navbar.ui;

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.ide.impl.idea.ui.popup.PopupOwner;
import consulo.navigationBar.model.*;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyPasteSupport;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.PasteProvider;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import org.jspecify.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Navigation bar panel: renders {@link NavBarVm} state via {@link NavBarVmListener} callbacks
 * and installs the keyboard actions directly on the panel.
 */
public final class NewNavBarPanel extends JPanel implements UiDataProvider, PopupOwner, NavBarVmListener {
    private static final Logger LOG = Logger.getInstance(NewNavBarPanel.class);

    private final NavBarVm myVm;
    private final Project myProject;
    private final boolean myFloating;
    private final boolean myInstallPopupHandler;

    private final List<NavBarItemComponent> myItemComponents = new ArrayList<>();

    private @Nullable Runnable myOnSizeChange;
    private @Nullable Runnable myOnFloatingCancel;

    private @Nullable JBPopup myPopup;

    @RequiredUIAccess
    public NewNavBarPanel(NavBarVm vm, Project project, boolean isFloating) {
        this(vm, project, isFloating, true);
    }

    /**
     * @param installPopupHandler Disables popup which appears on right click containing nav bar actions
     */
    @RequiredUIAccess
    public NewNavBarPanel(NavBarVm vm, Project project, boolean isFloating, boolean installPopupHandler) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        myVm = vm;
        myProject = project;
        myFloating = isFloating;
        myInstallPopupHandler = installPopupHandler;

        setOpaque(false);

        if (!isFloating) {
            addFocusListener(new NavBarDialogFocusListener(this));
        }
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                updateItemComponents();
            }

            @Override
            public void focusLost(FocusEvent e) {
                updateItemComponents();
            }
        });

        installKeyboardActions();

        myVm.addListener(this);
        rebuild(myVm.getItems());
    }

    public NavBarVm getVm() {
        return myVm;
    }

    public Project getProject() {
        return myProject;
    }

    public boolean isFloating() {
        return myFloating;
    }

    @RequiredUIAccess
    public void setOnSizeChange(@Nullable Runnable onSizeChange) {
        myOnSizeChange = onSizeChange;
    }

    @RequiredUIAccess
    public void setOnFloatingCancel(@Nullable Runnable onFloatingCancel) {
        myOnFloatingCancel = onFloatingCancel;
    }

    @RequiredUIAccess
    public void disconnect() {
        myVm.removeListener(this);
        hidePopupHint();
    }

    // region NavBarVmListener

    @Override
    public void itemsChanged(List<? extends NavBarItemVm> items) {
        rebuild(items);
    }

    @Override
    public void selectedIndexChanged(int selectedIndex) {
        if (selectedIndex >= 0 && selectedIndex < myItemComponents.size()) {
            NavBarItemComponent itemComponent = myItemComponents.get(selectedIndex);
            scrollRectToVisible(itemComponent.getBounds());
            itemComponent.focusItem();
        }
        updateItemComponents();
    }

    @Override
    public void itemSelectionChanged(NavBarItemVm item, boolean selected) {
        for (NavBarItemComponent itemComponent : myItemComponents) {
            if (itemComponent.getVm() == item) {
                itemComponent.update();
                break;
            }
        }
    }

    @Override
    public void popupChanged(@Nullable NavBarPopupVm<?> popup) {
        hidePopupHint();
        if (popup != null) {
            showPopup(myVm.getSelectedIndex(), popup);
        }
        // popup visibility affects the focused state of the items
        updateItemComponents();
    }

    // endregion

    private void updateItemComponents() {
        for (NavBarItemComponent itemComponent : myItemComponents) {
            itemComponent.update();
        }
    }

    @RequiredUIAccess
    private void rebuild(List<? extends NavBarItemVm> items) {
        removeAll();
        myItemComponents.clear();

        for (NavBarItemVm item : items) {
            NavBarItemComponent itemComponent = new NavBarItemComponent(item, this, myInstallPopupHandler);
            itemComponent.update();
            add(itemComponent);
            myItemComponents.add(itemComponent);
        }

        revalidate();
        repaint();

        if (myOnSizeChange != null) {
            myOnSizeChange.run();
        }
        if (!myItemComponents.isEmpty()) {
            scrollRectToVisible(myItemComponents.get(myItemComponents.size() - 1).getBounds());
        }
    }

    private <T extends NavBarPopupItem> void showPopup(int itemComponentIndex, NavBarPopupVm<T> popupVm) {
        if (itemComponentIndex < 0 || itemComponentIndex >= myItemComponents.size()) {
            return;
        }
        NavBarItemComponent itemComponent = myItemComponents.get(itemComponentIndex);
        if (!isShowing()) {
            LOG.warn("Navigation bar panel is not showing => cannot show child popup");
            return;
        }

        List<T> items = popupVm.getItems();
        var builder = JBPopupFactory.getInstance().createPopupChooserBuilder(items)
            .setRenderer(new NavBarPopupListCellRenderer(myFloating))
            .setNamerForFiltering(item -> {
                String popupText = item.getPresentation().popupText();
                return popupText != null ? popupText : item.getPresentation().text();
            })
            .setAutoselectOnMouseMove(true)
            .setRequestFocus(true)
            .setVisibleRowCount(Math.min(items.size(), 15))
            .setAccessibleName(itemComponent.getText())
            .setItemSelectedCallback(item -> popupVm.itemsSelected(item == null ? List.of() : List.of(item)))
            .setItemsChosenCallback(chosenItems -> {
                if (!chosenItems.isEmpty()) {
                    popupVm.itemsSelected(new ArrayList<>(chosenItems));
                    popupVm.complete();
                }
            });
        int selectedItemIndex = popupVm.getInitialSelectedItemIndex();
        if (selectedItemIndex >= 0 && selectedItemIndex < items.size()) {
            builder.setSelectedValue(items.get(selectedItemIndex), true);
        }
        JBPopup popup = builder.createPopup();
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                // the cancel callback of the builder is consulted on EVERY close, including the item choice,
                // and would kill the result before the chosen callback completes it - so cancel here instead,
                // only when the popup was closed without a choice
                if (!event.isOk()) {
                    popupVm.cancel();
                }
            }
        });
        popup.pack(true, true);
        myPopup = popup;
        int offsetX = NavBarUi.navBarPopupOffset(itemComponentIndex == 0);
        Point point = getItemPopupLocation(itemComponent);
        popup.show(new RelativePoint(this, new Point(point.x - offsetX, point.y)));
    }

    private void hidePopupHint() {
        JBPopup popup = myPopup;
        if (popup != null) {
            myPopup = null;
            popup.cancel();
        }
    }

    private Point getItemPopupLocation(Component itemComponent) {
        int relativeY = itemComponent.getHeight();
        RelativePoint relativePoint = new RelativePoint(itemComponent, new Point(0, relativeY));
        return relativePoint.getPoint(this);
    }

    public boolean isItemFocused() {
        if (myVm.getPopup() != null) {
            // the child popup owns the focus, but the bar selection must stay visible (legacy behavior)
            return true;
        }
        if (NavBarItemComponent.isItemComponentFocusable()) {
            return UIUtil.isFocusAncestor(this);
        }
        return hasFocus();
    }

    @Override
    public @Nullable Point getBestPopupPosition() {
        int selectedIndex = myVm.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= myItemComponents.size()) {
            return null;
        }
        NavBarItemComponent itemComponent = myItemComponents.get(selectedIndex);
        return new Point(itemComponent.getX(), itemComponent.getY() + itemComponent.getHeight());
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        if (myProject.isDisposed()) {
            return;
        }
        sink.set(Project.KEY, myProject);
        sink.set(NavBarVmItem.SELECTED_ITEMS, myVm.selection());

        CopyPasteSupport copyPasteSupport = getCopyPasteDelegator();
        sink.set(CutProvider.KEY, copyPasteSupport.getCutProvider());
        sink.set(CopyProvider.KEY, copyPasteSupport.getCopyProvider());
        sink.set(PasteProvider.KEY, copyPasteSupport.getPasteProvider());
    }

    private CopyPasteSupport getCopyPasteDelegator() {
        String key = "NavBarPanel.copyPasteDelegator";
        Object result = getClientProperty(key);
        if (!(result instanceof CopyPasteSupport)) {
            result = new CopyPasteDelegator(myProject, this);
            putClientProperty(key, result);
        }
        return (CopyPasteSupport) result;
    }

    private void installKeyboardActions() {
        registerKey(KeyEvent.VK_LEFT, e -> myVm.shiftSelectionTo(NavBarVm.SelectionShift.PREV));
        registerKey(KeyEvent.VK_RIGHT, e -> myVm.shiftSelectionTo(NavBarVm.SelectionShift.NEXT));
        registerKey(KeyEvent.VK_HOME, e -> myVm.shiftSelectionTo(NavBarVm.SelectionShift.FIRST));
        registerKey(KeyEvent.VK_END, e -> myVm.shiftSelectionTo(NavBarVm.SelectionShift.LAST));
        registerKey(KeyEvent.VK_DOWN, e -> myVm.showPopup());
        registerKey(KeyEvent.VK_UP, e -> myVm.showPopup());
        registerKey(KeyEvent.VK_ENTER, e -> enter());
        registerKey(KeyEvent.VK_ESCAPE, e -> escape());
    }

    private void registerKey(int keyCode, ActionListener action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, 0);
        registerKeyboardAction(action, keyStroke, WHEN_FOCUSED);
        registerKeyboardAction(action, keyStroke, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void enter() {
        NavBarPopupVm<?> popup = myVm.getPopup();
        if (popup != null) {
            popup.complete();
        }
        else {
            myVm.showPopup();
        }
    }

    private void escape() {
        NavBarPopupVm<?> popup = myVm.getPopup();
        if (popup != null) {
            popup.cancel();
            return;
        }
        if (myFloating) {
            if (myOnFloatingCancel != null) {
                myOnFloatingCancel.run();
            }
            return;
        }
        ToolWindowManager.getInstance(myProject).activateEditorComponent();
    }
}
