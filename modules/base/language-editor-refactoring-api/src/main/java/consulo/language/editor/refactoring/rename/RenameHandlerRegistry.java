/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.inplace.MemberInplaceRenameHandler;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author dsl
 */
public class RenameHandlerRegistry {
    public static final Key<Boolean> SELECT_ALL = Key.create("rename.selectAll");
    private final Set<RenameHandler> myHandlers = new HashSet<>();
    private static final RenameHandlerRegistry INSTANCE = new RenameHandlerRegistry();
    private final PsiElementRenameHandler myDefaultElementRenameHandler;

    public static RenameHandlerRegistry getInstance() {
        return INSTANCE;
    }

    private RenameHandlerRegistry() {
        // should be checked last
        myDefaultElementRenameHandler = new PsiElementRenameHandler();
    }

    @RequiredReadAction
    public boolean hasAvailableHandler(DataContext dataContext) {
        for (RenameHandler renameHandler : RenameHandler.EP_NAME.getExtensionList()) {
            if (renameHandler.isAvailableOnDataContext(dataContext)) {
                return true;
            }
        }
        for (RenameHandler renameHandler : myHandlers) {
            if (renameHandler.isAvailableOnDataContext(dataContext)) {
                return true;
            }
        }
        return myDefaultElementRenameHandler.isAvailableOnDataContext(dataContext);
    }

    /**
     * The handlers which can rename what the context points at, keyed by their titles. A member in-place handler is left
     * out when something else can rename the element too.
     */
    @RequiredReadAction
    public Map<LocalizeValue, RenameHandler> getRenameHandlers(DataContext dataContext) {
        Map<LocalizeValue, RenameHandler> availableHandlers = new TreeMap<>();
        RenameHandler.EP_NAME.forEachExtensionSafe(renameHandler -> {
            if (renameHandler.isRenaming(dataContext)) {
                availableHandlers.put(getHandlerTitle(renameHandler), renameHandler);
            }
        });

        for (RenameHandler renameHandler : myHandlers) {
            if (renameHandler.isRenaming(dataContext)) {
                availableHandlers.put(getHandlerTitle(renameHandler), renameHandler);
            }
        }

        if (availableHandlers.size() > 1) {
            availableHandlers.values().removeIf(handler -> handler instanceof MemberInplaceRenameHandler);
        }

        if (availableHandlers.isEmpty() && myDefaultElementRenameHandler.isRenaming(dataContext)) {
            availableHandlers.put(getHandlerTitle(myDefaultElementRenameHandler), myDefaultElementRenameHandler);
        }
        return availableHandlers;
    }

    /**
     * The handler to rename with. When more than one can, the user picks one and the future completes once they did;
     * dismissing the choice cancels it.
     */
    @RequiredUIAccess
    public CompletableFuture<@Nullable RenameHandler> getRenameHandlerAsync(DataContext dataContext) {
        Map<LocalizeValue, RenameHandler> availableHandlers = getRenameHandlers(dataContext);
        if (availableHandlers.size() <= 1) {
            return CompletableFuture.completedFuture(availableHandlers.isEmpty() ? null : availableHandlers.values().iterator().next());
        }

        CompletableFuture<@Nullable RenameHandler> result = new CompletableFuture<>();
        List<LocalizeValue> titles = new ArrayList<>(availableHandlers.keySet());
        BaseListPopupStep<LocalizeValue> step = new BaseListPopupStep<>(RefactoringLocalize.whatWouldYouLikeToDo().get(), titles) {
            @Override
            public String getTextFor(LocalizeValue value) {
                return value.get();
            }

            @Override
            public PopupStep onChosen(LocalizeValue selectedValue, boolean finalChoice) {
                return doFinalStep(() -> result.complete(availableHandlers.get(selectedValue)));
            }

            @Override
            public void canceled() {
                result.cancel(false);
            }
        };

        JBPopupFactory.getInstance().createListPopup(step).showInBestPositionFor(dataContext);
        return result;
    }

    
    private static LocalizeValue getHandlerTitle(RenameHandler renameHandler) {
        return renameHandler.getActionTitleValue().capitalize();
    }

    /**
     * @see RenameHandler#EP_NAME
     * @deprecated
     */
    public void registerHandler(RenameHandler handler) {
        myHandlers.add(handler);
    }
}
