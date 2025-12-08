/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.application.Application;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 27/06/2023
 */
public class ActionGroupExpander {

    /**
     * @return actions from the given and nested non-popup groups that are visible after updating
     */
    @RequiredUIAccess
    public static List<AnAction> expandActionGroup(@Nonnull ActionGroup group,
                                                   PresentationFactory presentationFactory,
                                                   @Nonnull DataContext context,
                                                   @Nonnull String place,
                                                   @Nonnull Predicate<AnAction> filter) {
        List<AnAction> actions = new ActionUpdater(ActionManager.getInstance(),
            presentationFactory,
            context,
            place,
            false,
            false,
            UIAccess.current()
        ).expandActionGroup(group, group instanceof CompactActionGroup);

        actions = new ArrayList<>(actions);
        actions.removeIf(filter.negate());

        while (!actions.isEmpty() && actions.getFirst() instanceof AnSeparator) {
            actions.removeFirst();
        }

        while (!actions.isEmpty() && actions.getLast() instanceof AnSeparator) {
            actions.removeLast();
        }

        // remove separators if
        // item
        // separator="" <-- remove this separator
        // separator="some"
        // item
        ListIterator<AnAction> listed = actions.listIterator();
        while (listed.hasNext()) {
            AnAction action = listed.next();

            if (action instanceof AnSeparator separator
                && separator.getTextValue().isAbsent()
                && listed.nextIndex() < actions.size()
                && actions.get(listed.nextIndex()) instanceof AnSeparator nextSeparator
                && nextSeparator.getTextValue().isPresent()) {
                listed.remove();
            }
        }
        
        return actions;
    }

    @Nonnull
    public static CompletableFuture<List<? extends AnAction>> expandActionGroupAsync(@Nonnull ActionGroup group,
                                                                                     PresentationFactory presentationFactory,
                                                                                     @Nonnull DataContext context,
                                                                                     String place) {
        if (!(context instanceof AsyncDataContext)) {
            context = DataManager.getInstance().createAsyncDataContext(context);
        }
        return new ActionUpdater(ActionManager.getInstance(), presentationFactory, context, place, false, false, Application.get().getLastUIAccess())
            .expandActionGroupAsync(group, group instanceof CompactActionGroup);
    }
}
