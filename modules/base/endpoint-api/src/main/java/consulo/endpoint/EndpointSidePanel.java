// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;

import java.util.List;

public interface EndpointSidePanel {
    LocalizeValue getTitle();

    @RequiredUIAccess
    Component getComponent();

    /**
     * Determines whether the side panel is available.
     *
     * @return true if the side panel should be available; false otherwise.
     * If false, the panel may be disabled
     * or remain accessible if it has been previously selected by the user to avoid unnecessary flickering.
     */
    Coroutine<?, Boolean> isAvailable(List<? extends EndpointListItem> selectedItems);

    /**
     * Updates the component for the new list of selected endpoints items.
     * <p>
     * The update will be canceled if the selected endpoints (@param selectedItems) change or if there are psi changes.
     * The update will not be called if the tab has already been updated.
     * The update may also be called when {@code EndpointsView.forgetData} is invoked.
     */
    Coroutine<?, ?> update(List<? extends EndpointListItem> selectedItems);

    /**
     * Method called when the side panel is shown but not {@link #update}.
     * For example, this can be invoked if the panel was already updated before.
     */
    @RequiredUIAccess
    default void selected(List<? extends EndpointListItem> selectedItems) {
    }
}
