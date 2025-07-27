/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.internal;


import consulo.container.plugin.PluginId;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.Comparator;

public abstract class ActionManagerEx extends ActionManager {
    public static ActionManagerEx getInstanceEx() {
        return (ActionManagerEx) getInstance();
    }

    public abstract void fireBeforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event);

    public abstract void fireAfterActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event);

    public abstract void fireBeforeEditorTyping(char c, DataContext dataContext);

    /**
     * For logging purposes
     */
    public abstract String getLastPreformedActionId();

    public abstract String getPrevPreformedActionId();

    /**
     * Comparator compares action ids (String) on order of action registration.
     *
     * @return a negative integer if action that corresponds to the first id was registered earler than the action that corresponds
     * <p/>
     * to the second id; zero if both ids are equal; a positive number otherwise.
     */
    public abstract Comparator<String> getRegistrationOrderComparator();

    /**
     * Similar to {@link KeyStroke#getKeyStroke(String)} but allows keys in lower case.
     * <p/>
     * I.e. "control x" is accepted and interpreted as "control X".
     *
     * @return null if string cannot be parsed.
     */

    @Nullable
    public static KeyStroke getKeyStroke(String s) {
        KeyStroke result = null;
        try {
            result = KeyStroke.getKeyStroke(s);
        }
        catch (Exception ex) {
            //ok
        }

        if (result == null && s != null && s.length() >= 2 && s.charAt(s.length() - 2) == ' ') {
            try {
                String s1 = s.substring(0, s.length() - 1) + Character.toUpperCase(s.charAt(s.length() - 1));
                result = KeyStroke.getKeyStroke(s1);
            }
            catch (Exception ex) {
                // ok
            }
        }
        return result;
    }

    @Override
    public abstract String[] getPluginActions(PluginId pluginId);

    public abstract void queueActionPerformedEvent(AnAction action, DataContext context, AnActionEvent event);

    public abstract boolean isActionPopupStackEmpty();

    public abstract boolean isTransparentOnlyActionsUpdateNow();

    public abstract boolean performDumbAwareUpdate(@Nonnull AnAction action, @Nonnull AnActionEvent e, boolean beforeActionPerformed);

    public void fireBeforeActionPerformed(String actionId, InputEvent event) {
        AnAction action = getAction(actionId);
        if (action != null) {
            DataContext context = DataManager.getInstance().getDataContext();
            AnActionEvent e = new AnActionEvent(event, context, ActionPlaces.UNKNOWN, action.getTemplatePresentation(), this, 0);
            fireBeforeActionPerformed(action, context, e);
        }
    }

    @Nonnull
    public abstract ActionPopupMenu createActionPopupMenu(
        @Nonnull String place,
        @Nonnull ActionGroup group,
        @Nullable PresentationFactory presentationFactory
    );
}
