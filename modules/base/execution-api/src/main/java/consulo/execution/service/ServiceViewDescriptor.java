// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.service;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataProvider;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigable;
import consulo.navigation.Navigatable;
import consulo.ui.ex.action.ActionGroup;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;

public interface ServiceViewDescriptor {
    Key<Boolean> ACTION_HOLDER_KEY = Key.create("ServiceViewActionHolderContentComponent");

    ItemPresentation getPresentation();

    default @Nullable String getId() {
        return getPresentation().getPresentableText();
    }

    default @Nullable JComponent getContentComponent() {
        return null;
    }

    default ItemPresentation getContentPresentation() {
        return getPresentation();
    }

    default ItemPresentation getCustomPresentation(ServiceViewOptions options, ServiceViewItemState state) {
        return getPresentation();
    }

    default @Nullable ActionGroup getToolbarActions() {
        return null;
    }

    default @Nullable ActionGroup getPopupActions() {
        return getToolbarActions();
    }

    default @Nullable DataProvider getDataProvider() {
        return null;
    }

    default void onNodeSelected(List<Object> selectedServices) {
    }

    default void onNodeUnselected() {
    }

    @RequiredReadAction
    default boolean handleDoubleClick(MouseEvent event) {
        Navigable navigable = getNavigable();
        if (navigable != null && navigable.canNavigateToSource()) {
            navigable.navigate(true);
            return true;
        }
        return false;
    }

    default @Nullable Object getPresentationTag(Object fragment) {
        return null;
    }

    default @Nullable Navigable getNavigable() {
        return null;
    }

    @Deprecated
    @DeprecationInfo("Use #getNavigable(), typo-corrected name")
    @SuppressWarnings({"SpellCheckingInspection", "deprecation"})
    default @Nullable Navigatable getNavigatable() {
        return (Navigatable) getNavigable();
    }

    default @Nullable Runnable getRemover() {
        return null;
    }

    default boolean isVisible() {
        return true;
    }
}
