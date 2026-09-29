/*
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
package consulo.ui.ex.impl.internal.popup.action;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.KeepPopupOnPerform;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 2026-09-29
 */
public final class PopupInlineActions {
    public record Button(@Nullable ActionPopupItem item, boolean alwaysVisible) {
        public boolean isMore() {
            return item == null;
        }
    }

    private final ListPopupStep<Object> myStep;
    private final @Nullable ActionPopupStep myActionStep;
    private final BooleanSupplier myShowSubmenuOnHover;

    @SuppressWarnings("unchecked")
    public PopupInlineActions(ListPopupStep<?> step, BooleanSupplier showSubmenuOnHover) {
        myStep = (ListPopupStep<Object>) step;
        myActionStep = step instanceof ActionPopupStep actionStep ? actionStep : null;
        myShowSubmenuOnHover = showSubmenuOnHover;
    }

    public List<Button> getButtons(@Nullable Object element) {
        List<ActionPopupItem> inlineItems = getInlineItems(element);
        boolean hasMoreButton = hasMoreButton(element);
        if (inlineItems.isEmpty() && !hasMoreButton) {
            return List.of();
        }

        List<Button> buttons = new ArrayList<>(inlineItems.size() + 1);
        boolean anyAlwaysVisible = false;
        for (ActionPopupItem item : inlineItems) {
            boolean alwaysVisible = Boolean.TRUE.equals(item.getClientProperty(ActionUtil.ALWAYS_VISIBLE_INLINE_ACTION));
            anyAlwaysVisible |= alwaysVisible;
            buttons.add(new Button(item, alwaysVisible));
        }

        if (hasMoreButton) {
            buttons.add(new Button(null, anyAlwaysVisible));
        }
        return buttons;
    }

    public int getButtonsCount(@Nullable Object element) {
        return getInlineItems(element).size() + (hasMoreButton(element) ? 1 : 0);
    }

    public boolean hasButtons(@Nullable Object element) {
        return getButtonsCount(element) > 0;
    }

    public boolean isMoreButton(@Nullable Object element, int index) {
        return hasMoreButton(element) && index == getButtonsCount(element) - 1;
    }

    public @Nullable ActionPopupItem getInlineItem(@Nullable Object element, int index) {
        List<ActionPopupItem> inlineItems = getInlineItems(element);
        return index >= 0 && index < inlineItems.size() ? inlineItems.get(index) : null;
    }

    public LocalizeValue getToolTip(@Nullable Object element, int index) {
        if (isMoreButton(element, index)) {
            return ActionLocalize.inlineActionsMoreActionsText();
        }

        ActionPopupItem item = getInlineItem(element, index);
        return item == null ? LocalizeValue.empty() : item.getText();
    }

    public KeepPopupOnPerform getKeepPopupOnPerform(@Nullable Object element, int index) {
        ActionPopupItem item = isMoreButton(element, index) ? null : getInlineItem(element, index);
        return item == null ? KeepPopupOnPerform.ALWAYS : item.getKeepPopupOnPerform();
    }

    public @Nullable Image getIcon(Object element, Button button, boolean selected) {
        ActionPopupItem item = button.item();
        if (item == null) {
            return myStep.isFinal(element) ? PlatformIconGroup.actionsMorevertical() : PlatformIconGroup.ideMenuarrow();
        }
        return item.getIcon(selected);
    }

    private List<ActionPopupItem> getInlineItems(@Nullable Object element) {
        if (myActionStep != null && element instanceof ActionPopupItem item) {
            return myActionStep.getInlineItems(item);
        }
        return List.of();
    }

    private boolean hasMoreButton(@Nullable Object element) {
        return element != null
            && myStep.hasSubstep(element)
            && !myShowSubmenuOnHover.getAsBoolean()
            && myStep.isFinal(element);
    }
}
