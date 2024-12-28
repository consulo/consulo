// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.ui.impl;

import consulo.annotation.DeprecationInfo;
import consulo.application.util.function.Computable;
import consulo.application.util.function.Processor;
import consulo.ide.impl.idea.ui.popup.PopupListAdapter;
import consulo.ide.impl.idea.ui.popup.PopupTableAdapter;
import consulo.ide.impl.idea.ui.popup.PopupTreeAdapter;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.popup.AWTPopupChooserBuilder;
import consulo.ui.ex.popup.*;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author max
 */
@Deprecated
@DeprecationInfo("Use consulo.ide.ui.popup.JBPopupFactory#createPopupChooserBuilder")
public class PopupChooserBuilder<T> implements IPopupChooserBuilder<T>, AWTPopupChooserBuilder<T> {
    private final PopupComponentAdapter<T> myChooserComponent;
    private String myTitle;
    private final ArrayList<KeyStroke> myAdditionalKeystrokes = new ArrayList<>();
    private Runnable myItemChosenRunnable;
    private JComponent mySouthComponent;
    private JComponent myEastComponent;

    private JBPopup myPopup;

    private boolean myRequestFocus = true;
    private boolean myForceResizable;
    private boolean myForceMovable;
    private String myDimensionServiceKey;
    private Computable<Boolean> myCancelCallback;
    private boolean myAutoselect = true;
    private float myAlpha;
    private Component[] myFocusOwners = new Component[0];
    private boolean myCancelKeyEnabled = true;

    private final List<JBPopupListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private String myAd;
    private Dimension myMinSize;
    private final List<Pair<ActionListener, KeyStroke>> myKeyboardActions = new ArrayList<>();
    private boolean myAutoselectOnMouseMove = true;

    private Function<Object, String> myItemsNamer;
    private boolean myMayBeParent;
    private int myAdAlignment = SwingConstants.LEFT;
    private boolean myModalContext;
    private boolean myCloseOnEnter = true;
    private boolean myCancelOnWindowDeactivation = true;
    private boolean myUseForXYLocation;
    @Nullable
    private Predicate<? super JBPopup> myCouldPin;
    private int myVisibleRowCount = 15;
    private boolean myAutoPackHeightOnFiltering = true;
    private List<AnAction> myHeaderLeftActions = List.of();
    private List<AnAction> myHeaderRightActions = List.of();

    public interface PopupComponentAdapter<T> {
        JComponent getComponent();

        default void setRenderer(ListCellRenderer renderer) {
        }

        void setItemChosenCallback(Consumer<? super T> callback);

        void setItemsChosenCallback(Consumer<? super Set<T>> callback);

        JScrollPane createScrollPane();

        default boolean hasOwnScrollPane() {
            return false;
        }

        default void addMouseListener(MouseListener listener) {
            getComponent().addMouseListener(listener);
        }

        default void autoSelect() {
        }

        default void setSelectionMode(int selection) {
        }

        default ListComponentUpdater<T> getBackgroundUpdater() {
            return null;
        }

        default void setSelectedValue(T preselection, boolean shouldScroll) {
            throw new UnsupportedOperationException("Not supported for this popup type");
        }

        default void setItemSelectedCallback(Consumer<? super T> c) {
            throw new UnsupportedOperationException("Not supported for this popup type");
        }

        default boolean checkResetFilter() {
            return false;
        }

        @Nullable
        default Predicate<KeyEvent> getKeyEventHandler() {
            return null;
        }

        default void setFont(Font f) {
            getComponent().setFont(f);
        }

        default JComponent buildFinalComponent() {
            return getComponent();
        }
    }

    @Override
    public PopupChooserBuilder<T> setCancelOnClickOutside(boolean cancelOnClickOutside) {
        myCancelOnClickOutside = cancelOnClickOutside;
        return this;
    }

    private boolean myCancelOnClickOutside = true;

    public JScrollPane getScrollPane() {
        return myScrollPane;
    }

    private JScrollPane myScrollPane;

    public PopupChooserBuilder(@Nonnull JList list) {
        myChooserComponent = new PopupListAdapter<>(this, list);
    }

    public PopupChooserBuilder(@Nonnull JTable table) {
        myChooserComponent = new PopupTableAdapter<>(this, table);
    }

    public PopupChooserBuilder(@Nonnull JTree tree) {
        myChooserComponent = new PopupTreeAdapter<>(this, tree);
    }

    @Override
    @Nonnull
    public PopupChooserBuilder<T> setTitle(@Nonnull @Nls(capitalization = Nls.Capitalization.Title) String title) {
        myTitle = title;
        return this;
    }

    @Nonnull
    public PopupChooserBuilder<T> addAdditionalChooseKeystroke(@Nullable KeyStroke keyStroke) {
        if (keyStroke != null) {
            myAdditionalKeystrokes.add(keyStroke);
        }
        return this;
    }

    @Override
    public IPopupChooserBuilder<T> setRenderer(ListCellRenderer renderer) {
        myChooserComponent.setRenderer(renderer);
        return this;
    }

    public JComponent getChooserComponent() {
        return myChooserComponent.getComponent();
    }

    @Nonnull
    @Override
    public IPopupChooserBuilder<T> setItemChosenCallback(@Nonnull Consumer<? super T> callback) {
        myChooserComponent.setItemChosenCallback(callback);
        return this;
    }

    @Nonnull
    @Override
    public IPopupChooserBuilder<T> setItemsChosenCallback(@Nonnull Consumer<? super Set<T>> callback) {
        myChooserComponent.setItemsChosenCallback(callback);
        return this;
    }

    @Override
    @Nonnull
    public PopupChooserBuilder<T> setItemChoosenCallback(@Nonnull Runnable runnable) {
        myItemChosenRunnable = runnable;
        return this;
    }

    @Override
    @Nonnull
    public PopupChooserBuilder<T> setSouthComponent(@Nonnull JComponent cmp) {
        mySouthComponent = cmp;
        return this;
    }

    @Override
    @Nonnull
    public PopupChooserBuilder<T> setCouldPin(@Nullable Processor<? super JBPopup> callback) {
        myCouldPin = callback;
        return this;
    }

    @Nonnull
    public PopupChooserBuilder<T> setEastComponent(@Nonnull JComponent cmp) {
        myEastComponent = cmp;
        return this;
    }


    @Override
    public PopupChooserBuilder<T> setRequestFocus(final boolean requestFocus) {
        myRequestFocus = requestFocus;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setResizable(final boolean forceResizable) {
        myForceResizable = forceResizable;
        return this;
    }


    @Override
    public PopupChooserBuilder<T> setMovable(final boolean forceMovable) {
        myForceMovable = forceMovable;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setDimensionServiceKey(@NonNls String key) {
        myDimensionServiceKey = key;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setUseDimensionServiceForXYLocation(boolean use) {
        myUseForXYLocation = use;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setCancelCallback(Supplier<Boolean> callback) {
        addCancelCallback(callback);
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setAlpha(final float alpha) {
        myAlpha = alpha;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setAutoselectOnMouseMove(final boolean doAutoSelect) {
        myAutoselectOnMouseMove = doAutoSelect;
        return this;
    }

    public boolean isAutoselectOnMouseMove() {
        return myAutoselectOnMouseMove;
    }

    public PopupChooserBuilder<T> setFilteringEnabled(Function<Object, String> namer) {
        myItemsNamer = namer;
        return this;
    }


    @Override
    public PopupChooserBuilder<T> setNamerForFiltering(Function<? super T, String> namer) {
        myItemsNamer = (Function<Object, String>) namer;
        return this;
    }

    public Function<Object, String> getItemsNamer() {
        return myItemsNamer;
    }

    @Override
    public IPopupChooserBuilder<T> setAutoPackHeightOnFiltering(boolean autoPackHeightOnFiltering) {
        myAutoPackHeightOnFiltering = autoPackHeightOnFiltering;
        return this;
    }

    public boolean isAutoPackHeightOnFiltering() {
        return myAutoPackHeightOnFiltering;
    }

    @Override
    public PopupChooserBuilder<T> setModalContext(boolean modalContext) {
        myModalContext = modalContext;
        return this;
    }

    @Override
    @Nonnull
    public JBPopup createPopup() {
        JPanel contentPane = new JPanel(new BorderLayout());
        if (!myForceMovable && myTitle != null) {
            JLabel label = new JLabel(myTitle);
            label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            contentPane.add(label, BorderLayout.NORTH);
        }

        if (myAutoselect) {
            myChooserComponent.autoSelect();
        }

        myChooserComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED) && !UIUtil.isSelectionButtonDown(e) && !e.isConsumed()) {
                    if (myCloseOnEnter) {
                        closePopup(e, true);
                    }
                    else {
                        myItemChosenRunnable.run();
                    }
                }
            }
        });

        registerClosePopupKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), false);
        if (myCloseOnEnter) {
            registerClosePopupKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), true);
        }
        else {
            registerKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), __ -> myItemChosenRunnable.run());
        }
        for (KeyStroke keystroke : myAdditionalKeystrokes) {
            registerClosePopupKeyboardAction(keystroke, true);
        }

        JComponent finalComponent = myChooserComponent.buildFinalComponent();
        myScrollPane = myChooserComponent.createScrollPane();

        myScrollPane.getViewport().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Insets viewportPadding = UIUtil.getListViewportPadding();
        ((JComponent) myScrollPane.getViewport().getView()).setBorder(BorderFactory.createEmptyBorder(viewportPadding.top, viewportPadding.left, viewportPadding.bottom, viewportPadding.right));

        if (myChooserComponent.hasOwnScrollPane()) {
            addCenterComponentToContentPane(contentPane, finalComponent);
        }
        else {
            addCenterComponentToContentPane(contentPane, myScrollPane);
        }

        if (mySouthComponent != null) {
            addSouthComponentToContentPane(contentPane, mySouthComponent);
        }

        if (myEastComponent != null) {
            addEastComponentToContentPane(contentPane, myEastComponent);
        }

        ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(contentPane, finalComponent);
        for (JBPopupListener each : myListeners) {
            builder.addListener(each);
        }

        builder.setDimensionServiceKey(null, myDimensionServiceKey, myUseForXYLocation).setRequestFocus(myRequestFocus).setResizable(myForceResizable).setMovable(myForceMovable)
            .setTitle(myForceMovable ? myTitle : null).setAlpha(myAlpha).setFocusOwners(myFocusOwners).setCancelKeyEnabled(myCancelKeyEnabled).setAdText(myAd, myAdAlignment)
            .setKeyboardActions(myKeyboardActions).setMayBeParent(myMayBeParent).setLocateWithinScreenBounds(true).setCancelOnOtherWindowOpen(true).setModalContext(myModalContext)
            .setCancelOnWindowDeactivation(myCancelOnWindowDeactivation).setCancelOnClickOutside(myCancelOnClickOutside).setCouldPin(myCouldPin).setOkHandler(myItemChosenRunnable);
        if (myCancelCallback != null) {
            builder.setCancelCallback(myCancelCallback);
        }
        Predicate<KeyEvent> keyEventHandler = myChooserComponent.getKeyEventHandler();
        if (keyEventHandler != null) {
            builder.setKeyEventHandler(keyEventHandler);
        }

        builder.setHeaderLeftActions(myHeaderLeftActions);
        builder.setHeaderRightActions(myHeaderRightActions);

        if (myMinSize != null) {
            builder.setMinSize(myMinSize);
        }

        myPopup = builder.createPopup();
        return myPopup;
    }

    private static void addEastComponentToContentPane(JPanel contentPane, JComponent component) {
        contentPane.add(component, BorderLayout.EAST);
    }

    private static void addSouthComponentToContentPane(JPanel contentPane, JComponent component) {
        contentPane.add(component, BorderLayout.SOUTH);
    }

    protected void addCenterComponentToContentPane(JPanel contentPane, JComponent component) {
        contentPane.add(component, BorderLayout.CENTER);
    }


    @Override
    public PopupChooserBuilder<T> setMinSize(final Dimension dimension) {
        myMinSize = dimension;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> registerKeyboardAction(KeyStroke keyStroke, ActionListener actionListener) {
        myKeyboardActions.add(Pair.create(actionListener, keyStroke));
        return this;
    }

    private void registerClosePopupKeyboardAction(final KeyStroke keyStroke, final boolean shouldPerformAction) {
        registerPopupKeyboardAction(keyStroke, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!shouldPerformAction && myChooserComponent.checkResetFilter()) {
                    return;
                }
                closePopup(null, shouldPerformAction);
            }
        });
    }

    private void registerPopupKeyboardAction(final KeyStroke keyStroke, AbstractAction action) {
        myChooserComponent.getComponent().registerKeyboardAction(action, keyStroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void closePopup(MouseEvent e, boolean isOk) {
        if (isOk) {
            myPopup.closeOk(e);
        }
        else {
            myPopup.cancel(e);
        }
    }

    @Override
    public PopupChooserBuilder<T> setAutoSelectIfEmpty(final boolean autoselect) {
        myAutoselect = autoselect;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setCancelKeyEnabled(final boolean enabled) {
        myCancelKeyEnabled = enabled;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> addListener(final JBPopupListener listener) {
        myListeners.add(listener);
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setMayBeParent(boolean mayBeParent) {
        myMayBeParent = mayBeParent;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setCloseOnEnter(boolean closeOnEnter) {
        myCloseOnEnter = closeOnEnter;
        return this;
    }

    @Nonnull
    public PopupChooserBuilder<T> setFocusOwners(@Nonnull Component[] focusOwners) {
        myFocusOwners = focusOwners;
        return this;
    }

    @Override
    @Nonnull
    public PopupChooserBuilder<T> setAdText(String ad) {
        setAdText(ad, SwingConstants.LEFT);
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setAdText(String ad, int alignment) {
        myAd = ad;
        myAdAlignment = alignment;
        return this;
    }

    @Override
    public PopupChooserBuilder<T> setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation) {
        myCancelOnWindowDeactivation = cancelOnWindowDeactivation;
        return this;
    }

    @Override
    public IPopupChooserBuilder<T> setSelectionMode(int selection) {
        myChooserComponent.setSelectionMode(selection);
        return this;
    }

    @Override
    public IPopupChooserBuilder<T> setSelectedValue(T preselection, boolean shouldScroll) {
        myChooserComponent.setSelectedValue(preselection, shouldScroll);
        return this;
    }

    @Override
    public IPopupChooserBuilder<T> setAccessibleName(String title) {
        AccessibleContextUtil.setName(myChooserComponent.getComponent(), title);
        return this;
    }

    @Override
    public IPopupChooserBuilder<T> setItemSelectedCallback(Consumer<? super T> c) {
        myChooserComponent.setItemSelectedCallback(c);
        return this;
    }

    private void addCancelCallback(Supplier<Boolean> cbb) {
        Supplier<Boolean> callback = myCancelCallback;
        myCancelCallback = () -> cbb.get() && (callback == null || callback.get());
    }

    @Override
    public IPopupChooserBuilder<T> withHintUpdateSupply() {
        HintUpdateSupply.installSimpleHintUpdateSupply(myChooserComponent.getComponent());
        addCancelCallback(() -> {
            HintUpdateSupply.hideHint(myChooserComponent.getComponent());
            return true;
        });
        return this;
    }

    @Override
    public IPopupChooserBuilder<T> setFont(Font f) {
        myChooserComponent.setFont(f);
        return this;
    }

    @Override
    public IPopupChooserBuilder<T> setVisibleRowCount(int visibleRowCount) {
        myVisibleRowCount = visibleRowCount;
        return this;
    }

    public int getVisibleRowCount() {
        return myVisibleRowCount;
    }

    @Override
    public ListComponentUpdater<T> getBackgroundUpdater() {
        return myChooserComponent.getBackgroundUpdater();
    }

    @Nonnull
    @Override
    public PopupChooserBuilder<T> setHeaderLeftActions(@Nonnull List<? extends AnAction> headerLeftActions) {
        if (myHeaderLeftActions.isEmpty()) {
            myHeaderLeftActions = new ArrayList<>();
        }
        myHeaderLeftActions.addAll(headerLeftActions);
        return this;
    }

    @Nonnull
    @Override
    public PopupChooserBuilder<T> setHeaderRightActions(@Nonnull List<? extends AnAction> headerRightActions) {
        if (myHeaderRightActions.isEmpty()) {
            myHeaderRightActions = new ArrayList<>();
        }
        myHeaderRightActions.addAll(headerRightActions);
        return this;
    }
}
