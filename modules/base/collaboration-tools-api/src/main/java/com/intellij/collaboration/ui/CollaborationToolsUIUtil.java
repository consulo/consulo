// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import com.intellij.collaboration.ui.codereview.comment.RoundedPanel;
import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.CodeReviewColorUtil;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import com.intellij.collaboration.ui.util.JComponentOverlay;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.event.DocumentAdapter;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.NameFilteringListModel;
import consulo.ui.ex.awt.speedSearch.SpeedSearch;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.update.Activatable;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CollaborationToolsUIUtil {
    static final Key<CoroutineScope> COMPONENT_SCOPE_KEY = Key.create("Collaboration.Component.Coroutine.Scope");

    private CollaborationToolsUIUtil() {
    }

    public static @Nonnull Icon getAnimatedLoadingIcon() {
        return AnimatedIcon.Default.INSTANCE;
    }

    /**
     * Connects {@code searchTextField} to a {@code list} to be used as a filter
     */
    public static <T> void attachSearch(
        @Nonnull JList<T> list,
        @Nonnull SearchTextField searchTextField,
        @Nonnull Function<T, String> searchBy
    ) {
        SpeedSearch speedSearch = new SpeedSearch(false);
        NameFilteringListModel<T> filteringListModel = new NameFilteringListModel<>(
            list.getModel(), searchBy::apply, speedSearch::shouldBeShowing, () -> {
            String filter = speedSearch.getFilter();
            return filter != null ? filter : "";
        }
        );
        list.setModel(filteringListModel);

        searchTextField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@Nonnull DocumentEvent e) {
                speedSearch.updatePattern(searchTextField.getText());
            }
        });

        speedSearch.addChangeListener(() -> {
            T prevSelection = list.getSelectedValue();
            filteringListModel.refilter();
            if (filteringListModel.getSize() > 0) {
                int fullMatchIndex = speedSearch.isHoldingFilter()
                    ? filteringListModel.getClosestMatchIndex()
                    : filteringListModel.getElementIndex(prevSelection);
                if (fullMatchIndex != -1) {
                    list.setSelectedIndex(fullMatchIndex);
                }
                if (filteringListModel.getSize() <= list.getSelectedIndex() ||
                    !filteringListModel.contains(list.getSelectedValue())) {
                    list.setSelectedIndex(0);
                }
            }
        });

        ScrollingUtil.installActions(list);
        ScrollingUtil.installActions(list, searchTextField.getTextEditor());
    }

    /**
     * Show an error on {@code component} if there's one in {@code errorValue}
     */
    public static void installValidator(@Nonnull JComponent component, @Nonnull SingleValueModel<@Nls String> errorValue) {
        UiNotifyConnector.installOn(component, new ValidatorActivatable(errorValue, component), false);
    }

    private static final class ValidatorActivatable implements Activatable {
        private final SingleValueModel<String> errorValue;
        private final JComponent component;
        private Disposable validatorDisposable;
        private ComponentValidator validator;

        ValidatorActivatable(@Nonnull SingleValueModel<String> errorValue, @Nonnull JComponent component) {
            this.errorValue = errorValue;
            this.component = component;
            errorValue.addListener(v -> {
                if (validator != null) {
                    validator.revalidate();
                }
            });
        }

        @Override
        public void showNotify() {
            validatorDisposable = Disposer.newDisposable("Component validator");
            validator = new ComponentValidator(validatorDisposable)
                .withValidator((Supplier<ValidationInfo>) () -> {
                    String value = errorValue.getValue();
                    return value != null ? new ValidationInfo(value, component) : null;
                })
                .installOn(component);
            validator.revalidate();
        }

        @Override
        public void hideNotify() {
            if (validatorDisposable != null) {
                Disposer.dispose(validatorDisposable);
                validatorDisposable = null;
            }
            validator = null;
        }
    }

    /**
     * Show progress label over {@code component}
     */
    public static @Nonnull JComponent wrapWithProgressOverlay(
        @Nonnull JComponent component,
        @Nonnull SingleValueModel<Boolean> inProgressValue
    ) {
        JLabel busyLabel = new JLabel(new AnimatedIcon.Default());
        inProgressValue.addAndInvokeListener(v -> {
            busyLabel.setVisible(v);
            component.setEnabled(!v);
        });
        return JComponentOverlay.createCentered(component, busyLabel);
    }

    /**
     * Wrap component with {@link SingleComponentCenteringLayout} to show component in a center
     */
    public static @Nonnull JComponent moveToCenter(@Nonnull JComponent component) {
        JPanel panel = new JPanel(new SingleComponentCenteringLayout());
        panel.setOpaque(false);
        panel.add(component);
        return panel;
    }

    /**
     * Adds actions to transfer focus by tab/shift-tab key for given {@code component}.
     */
    public static void registerFocusActions(@Nonnull JComponent component) {
        component.registerKeyboardAction(
            e -> component.transferFocus(),
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0),
            JComponent.WHEN_FOCUSED
        );
        component.registerKeyboardAction(
            e -> component.transferFocusBackward(),
            KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK),
            JComponent.WHEN_FOCUSED
        );
    }

    /**
     * Makes the button blue like a default button in dialogs
     */
    public static @Nonnull JButton defaultButton(@Nonnull JButton button) {
        setDefault(button, true);
        return button;
    }

    public static boolean isDefault(@Nonnull JButton button) {
        return ClientProperty.isTrue(button, DarculaButtonUI.DEFAULT_STYLE_KEY);
    }

    public static void setDefault(@Nonnull JButton button, boolean value) {
        if (value) {
            ClientProperty.put(button, DarculaButtonUI.DEFAULT_STYLE_KEY, true);
        }
        else {
            ClientProperty.remove(button, DarculaButtonUI.DEFAULT_STYLE_KEY);
        }
    }

    /**
     * Removes http(s) protocol and trailing slash from given {@code url}
     */
    public static @Nonnull String cleanupUrl(@Nonnull String url) {
        String result = url;
        if (result.startsWith("https://")) {
            result = result.substring("https://".length());
        }
        else if (result.startsWith("http://")) {
            result = result.substring("http://".length());
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Finds the proper focus target for {@code panel} and set focus to it
     */
    public static void focusPanel(@Nonnull JComponent panel) {
        Component toFocus = IdeFocusManager.findInstanceByComponent(panel).getFocusTargetFor(panel);
        if (toFocus != null) {
            toFocus.requestFocusInWindow();
        }
    }

    public static int getFocusBorderInset() {
        int bw = UIUtil.isUnderDefaultMacTheme() ? 3 : (int) DarculaUIUtil.BW.getUnscaled();
        int lw = UIUtil.isUnderDefaultMacTheme() ? 0 : (int) DarculaUIUtil.LW.getUnscaled();
        return bw + lw;
    }

    public static @Nonnull JComponent wrapWithLimitedSize(
        @Nonnull JComponent component,
        @Nullable Integer maxWidth,
        @Nullable Integer maxHeight
    ) {
        SizeRestrictedSingleComponentLayout layout = SizeRestrictedSingleComponentLayout.constant(maxWidth, maxHeight);
        JPanel panel = new JPanel(layout);
        panel.setName("Size limit wrapper");
        panel.setOpaque(false);
        panel.add(component);
        return panel;
    }

    public static @Nonnull JComponent wrapWithLimitedSize(
        @Nonnull JComponent component,
        @Nonnull DimensionRestrictions maxSize
    ) {
        SizeRestrictedSingleComponentLayout layout = new SizeRestrictedSingleComponentLayout();
        layout.setMaxSize(maxSize);
        JPanel panel = new JPanel(layout);
        panel.setName("Size limit wrapper");
        panel.setOpaque(false);
        panel.add(component);
        return panel;
    }

    public static @Nonnull JBColor getLabelBackground(@Nonnull String hexColor) {
        Color color = ColorUtil.fromHex(hexColor);
        return new JBColor(color, ColorUtil.darker(color, 3));
    }

    public static @Nonnull Color getLabelForeground(@Nonnull Color bg) {
        return ColorUtil.isDark(bg) ? JBColor.WHITE : JBColor.BLACK;
    }

    /**
     * Use method for different sizes depending on the type of UI (old/new).
     */
    public static int getSize(int oldUI, int newUI) {
        return ExperimentalUI.isNewUI() ? newUI : oldUI;
    }

    /**
     * Use method for different sizes depending on the type of UI (old/new).
     */
    public static @Nonnull Insets getInsets(@Nonnull Insets oldUI, @Nonnull Insets newUI) {
        return ExperimentalUI.isNewUI() ? newUI : oldUI;
    }

    /**
     * A text label with a rounded rectangle as a background.
     * To be used for various tags and badges.
     */
    public static @Nonnull JComponent createTagLabel(@Nls @Nonnull String text) {
        return createTagLabel(text, CodeReviewColorUtil.Review.stateForeground,
            CodeReviewColorUtil.Review.stateBackground, true
        );
    }

    public static @Nonnull JComponent createTagLabel(
        @Nls @Nonnull String text,
        @Nonnull Color textColor,
        @Nonnull Color backgroundColor,
        boolean compact
    ) {
        return createTagLabel(new SingleValueModel<>(text), textColor, backgroundColor, compact);
    }

    public static @Nonnull JComponent createTagLabel(
        @Nonnull SingleValueModel<@Nls String> model,
        @Nonnull Color textColor,
        @Nonnull Color backgroundColor,
        boolean compact
    ) {
        JLabel label = new JLabel(model.getValue());
        if (compact) {
            label.setFont(JBFont.small());
        }
        label.setForeground(textColor);
        label.setBorder(JBUI.Borders.empty(0, 4));
        model.addListener(v -> label.setText(v));

        BackgroundRoundedPanel panel = new BackgroundRoundedPanel(4, new SingleComponentCenteringLayout());
        panel.setFillBorder(false);
        panel.setBorder(JBUI.Borders.empty());
        panel.setBackground(backgroundColor);
        panel.add(label);
        return panel;
    }

    /**
     * Hides the component if none of the children are visible
     */
    public static void hideWhenNoVisibleChildren(@Nonnull JComponent component) {
        Component[] children = component.getComponents();
        component.setVisible(hasAnyVisible(children));
        for (Component child : children) {
            UIUtil.runWhenVisibilityChanged(child, () -> component.setVisible(hasAnyVisible(children)));
        }
    }

    private static boolean hasAnyVisible(Component @Nonnull [] components) {
        for (Component c : components) {
            if (c.isVisible()) {
                return true;
            }
        }
        return false;
    }

    @ApiStatus.Internal
    public static void validateAndApplyAction(@Nonnull DialogPanel panel, @Nonnull Runnable action) {
        panel.apply();
        var errors = panel.validateAll();
        if (errors.isEmpty()) {
            action.run();
            panel.reset();
        }
        else {
            JComponent componentWithError = errors.get(0).component;
            if (componentWithError != null) {
                focusPanel(componentWithError);
            }
        }
    }

    // -- Factory methods for panels (converted from top-level Kotlin functions) --

    public static @Nonnull JPanel verticalListPanel() {
        return verticalListPanel(0);
    }

    public static @Nonnull JPanel verticalListPanel(int gap) {
        JPanel panel = new OrientableScrollablePanel(SwingConstants.VERTICAL, ListLayout.vertical(gap));
        panel.setOpaque(false);
        return panel;
    }

    public static @Nonnull JPanel horizontalListPanel() {
        return horizontalListPanel(0);
    }

    public static @Nonnull JPanel horizontalListPanel(int gap) {
        JPanel panel = new OrientableScrollablePanel(SwingConstants.HORIZONTAL, ListLayout.horizontal(gap));
        panel.setOpaque(false);
        return panel;
    }

    public static @Nonnull JPanel scrollablePanel(int orientation) {
        return scrollablePanel(orientation, null);
    }

    public static @Nonnull JPanel scrollablePanel(int orientation, @Nullable LayoutManager layout) {
        return new OrientableScrollablePanel(orientation, layout);
    }

    public static @Nonnull JPanel clippingRoundedPanel() {
        return clippingRoundedPanel(8);
    }

    public static @Nonnull JPanel clippingRoundedPanel(int arcRadius) {
        return clippingRoundedPanel(arcRadius, null);
    }

    public static @Nonnull JPanel clippingRoundedPanel(int arcRadius, @Nullable LayoutManager layoutManager) {
        return new RoundedPanel(layoutManager, arcRadius);
    }

    public static @Nonnull JPanel clippingRoundedPanelWithBorder(
        int arcRadius,
        @Nonnull Color borderColor,
        @Nullable LayoutManager layoutManager
    ) {
        RoundedPanel panel = new RoundedPanel(layoutManager, arcRadius);
        panel.setBorder(new RoundedLineBorder(borderColor, (arcRadius + 1) * 2));
        return panel;
    }

    public static @Nonnull JPanel focusAwareClippingRoundedPanel(
        int arcRadius,
        @Nonnull Color borderColor,
        @Nullable LayoutManager layoutManager
    ) {
        RoundedPanel panel = new RoundedPanel(layoutManager, arcRadius);
        panel.setBorder(new FocusAwareRoundedLineBorder(borderColor, (arcRadius + 1) * 2));
        return panel;
    }

    public static @Nonnull JBColor jbColorFromHex(
        @NonNls @Nonnull String name,
        @NonNls @Nonnull String light,
        @NonNls @Nonnull String dark
    ) {
        return JBColor.namedColor(name, jbColorFromHex(light, dark));
    }

    public static @Nonnull JBColor jbColorFromHex(@NonNls @Nonnull String light, @NonNls @Nonnull String dark) {
        return new JBColor(ColorUtil.fromHex(light), ColorUtil.fromHex(dark));
    }

    /**
     * Selects first item in a combo box model
     */
    public static void selectFirst(@Nonnull ComboBoxModel<?> model) {
        if (model.getSize() == 0) {
            return;
        }
        Object first = model.getElementAt(0);
        model.setSelectedItem(first);
    }

    /**
     * Request focus on the component or a child determined by a focus policy
     */
    public static void requestFocusPreferred(@Nonnull JComponent component) {
        focusPanel(component);
    }

    static <E> int findIndex(@Nonnull ListModel<E> model, E item) {
        for (int i = 0; i < model.getSize(); i++) {
            if (Objects.equals(model.getElementAt(i), item)) {
                return i;
            }
        }
        return -1;
    }

    static <E> @Nonnull Iterable<E> items(@Nonnull ListModel<E> model) {
        return () -> new Iterator<>() {
            private int idx = -1;

            @Override
            public boolean hasNext() {
                return idx < model.getSize() - 1;
            }

            @Override
            public E next() {
                idx++;
                return model.getElementAt(idx);
            }
        };
    }
}
