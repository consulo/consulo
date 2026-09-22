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
package consulo.ui.ex.action;

import consulo.dataContext.DataContext;
import consulo.ui.Component;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import org.jspecify.annotations.Nullable;

import consulo.util.dataholder.Key;

import java.util.function.Predicate;

/**
 * An action shown as a control which opens a popup of its own rather than being performed by a press. What draws it
 * belongs to the toolbar it is placed in.
 *
 * @author VISTALL
 * @since 2026-09-22
 */
public abstract class ComboBoxAction extends AnAction implements AnActionWithSyncUpdate {
    /**
     * The control standing for this action, put there by whoever drew it so the action can reach it again.
     */
    public static final Key<Component> COMPONENT_KEY = Key.create("comboBoxActionComponent");

    /**
     * A control drawn for this action answers a press by running what is put here instead of dropping the popup,
     * and gives up its arrow while it does.
     */
    public static final String LIKE_BUTTON = "comboBoxActionLikeButton";

    private LocalizeValue myPopupTitle = LocalizeValue.empty();

    protected ComboBoxAction() {
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext context = e.getDataContext();

        createPopup(context, null).showInBestPositionFor(context);
    }

    @Override
    public void update(AnActionEvent e) {
    }

    @RequiredUIAccess
    public JBPopup createPopup(DataContext context, @Nullable Runnable onDispose) {
        return JBPopupFactory.getInstance().createActionGroupPopup(
            myPopupTitle.isEmpty() ? null : myPopupTitle.get(),
            createPopupActionGroup(context),
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            shouldShowDisabledActions(),
            onDispose,
            getMaxRows(),
            getPreselectCondition(),
            getPopupActionPlace()
        );
    }

    protected abstract ActionGroup createPopupActionGroup(DataContext context);

    public void setPopupTitle(LocalizeValue popupTitle) {
        myPopupTitle = popupTitle;
    }

    public LocalizeValue getPopupTitle() {
        return myPopupTitle;
    }

    public boolean shouldShowDisabledActions() {
        return false;
    }

    public int getMaxRows() {
        return 30;
    }

    public String getPopupActionPlace() {
        return ActionPlaces.UNKNOWN;
    }

    public @Nullable Predicate<AnAction> getPreselectCondition() {
        return null;
    }
}
