// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.popup;

import consulo.application.util.function.Processor;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.event.JBPopupListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IPopupChooserBuilder<T> {

    IPopupChooserBuilder<T> setRenderer(ListCellRenderer renderer);

    @Nonnull
    IPopupChooserBuilder<T> setItemChosenCallback(@Nonnull Consumer<? super T> callback);

    @Nonnull
    IPopupChooserBuilder<T> setItemsChosenCallback(@Nonnull Consumer<? super Set<T>> callback);

    IPopupChooserBuilder<T> setCancelOnClickOutside(boolean cancelOnClickOutside);

    @Nonnull
    IPopupChooserBuilder<T> setTitle(@Nonnull @Nls(capitalization = Nls.Capitalization.Title) String title);

    @Nonnull
    IPopupChooserBuilder<T> setCouldPin(@Nullable Processor<? super JBPopup> callback);

    IPopupChooserBuilder<T> setRequestFocus(boolean requestFocus);

    IPopupChooserBuilder<T> setResizable(boolean forceResizable);

    IPopupChooserBuilder<T> setMovable(boolean forceMovable);

    IPopupChooserBuilder<T> setDimensionServiceKey(String key);

    IPopupChooserBuilder<T> setUseDimensionServiceForXYLocation(boolean use);

    IPopupChooserBuilder<T> setCancelCallback(Supplier<Boolean> callback);

    IPopupChooserBuilder<T> setAlpha(float alpha);

    IPopupChooserBuilder<T> setAutoselectOnMouseMove(boolean doAutoSelect);

    IPopupChooserBuilder<T> setNamerForFiltering(Function<? super T, String> namer);

    IPopupChooserBuilder<T> setAutoPackHeightOnFiltering(boolean autoPackHeightOnFiltering);

    IPopupChooserBuilder<T> setModalContext(boolean modalContext);

    @Nonnull
    JBPopup createPopup();

    IPopupChooserBuilder<T> setMinSize(Dimension dimension);

    IPopupChooserBuilder<T> registerKeyboardAction(KeyStroke keyStroke, ActionListener actionListener);

    IPopupChooserBuilder<T> setAutoSelectIfEmpty(boolean autoselect);

    IPopupChooserBuilder<T> setCancelKeyEnabled(boolean enabled);

    IPopupChooserBuilder<T> addListener(JBPopupListener listener);

    IPopupChooserBuilder<T> setMayBeParent(boolean mayBeParent);

    IPopupChooserBuilder<T> setCloseOnEnter(boolean closeOnEnter);

    @Nonnull
    IPopupChooserBuilder<T> setAdText(String ad);

    IPopupChooserBuilder<T> setAdText(String ad, int alignment);

    IPopupChooserBuilder<T> setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation);

    IPopupChooserBuilder<T> setSelectionMode(int selection);

    IPopupChooserBuilder<T> setSelectedValue(T preselection, boolean shouldScroll);

    IPopupChooserBuilder<T> setAccessibleName(String title);

    IPopupChooserBuilder<T> setItemSelectedCallback(Consumer<? super T> c);

    IPopupChooserBuilder<T> withHintUpdateSupply();

    IPopupChooserBuilder<T> setFont(Font f);

    IPopupChooserBuilder<T> setVisibleRowCount(int visibleRowCount);

    @Nonnull
    IPopupChooserBuilder<T> setHeaderLeftActions(@Nonnull List<? extends AnAction> actions);

    @Nonnull
    IPopupChooserBuilder<T> setHeaderRightActions(@Nonnull List<? extends AnAction> actions);

    ListComponentUpdater<T> getBackgroundUpdater();

    @Nonnull
    default IPopupChooserBuilder<T> setSouthComponent(@Nonnull JComponent cmp) {
        return this;
    }
}
