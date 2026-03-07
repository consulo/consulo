// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.application.localize.ApplicationLocalize;
import consulo.dataContext.DataContext;
import consulo.application.progress.ProgressIndicator;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionGroupExpander;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.ui.popup.NothingHereAction;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

class ActionStepBuilder {

    private final List<ActionPopupItem> myListModel;
    private final DataContext myDataContext;
    private final boolean myShowNumbers;
    private final boolean myUseAlphaAsNumbers;
    private final PresentationFactory myPresentationFactory;
    private final boolean myShowDisabled;
    private int myCurrentNumber;
    private final boolean myHonorActionMnemonics;
    private final String myActionPlace;
    @Nonnull
    private final ProgressIndicator myIndicator;

    private int myMaxIconWidth = -1;
    private int myMaxIconHeight = -1;

    ActionStepBuilder(@Nonnull DataContext dataContext,
                      boolean showNumbers,
                      boolean useAlphaAsNumbers,
                      boolean showDisabled,
                      boolean honorActionMnemonics,
                      @Nullable String actionPlace,
                      @Nonnull PresentationFactory presentationFactory,
                      @Nonnull ProgressIndicator indicator) {
        myUseAlphaAsNumbers = useAlphaAsNumbers;
        myPresentationFactory = presentationFactory;
        myListModel = new ArrayList<>();
        myDataContext = dataContext;
        myShowNumbers = showNumbers;
        myShowDisabled = showDisabled;
        myCurrentNumber = 0;
        myHonorActionMnemonics = honorActionMnemonics;
        myActionPlace = ObjectUtil.notNull(actionPlace, ActionPlaces.UNKNOWN);
        myIndicator = indicator;
    }

    @Nonnull
    public List<ActionPopupItem> getItems() {
        return myListModel;
    }

    @Nonnull
    public CompletableFuture<List<ActionPopupItem>> buildGroup(@Nonnull ActionGroup actionGroup) {
        return appendActionsFromGroup(actionGroup).thenApply(v -> {
            if (myListModel.isEmpty()) {
                myListModel.add(new ActionPopupItem(NothingHereAction.INSTANCE, ApplicationLocalize.nothingHere()));
            }
            return myListModel;
        });
    }

    private void calcMaxIconSize(List<? extends AnAction> actions) {
        if (myPresentationFactory instanceof MenuItemPresentationFactory factory && factory.shallHideIcons()) {
            return;
        }

        for (AnAction action : actions) {
            if (action == null || action instanceof AnSeparator) {
                continue;
            }

            Presentation presentation = myPresentationFactory.getPresentation(action);

            Image icon = presentation.getIcon();

            if (icon == null && action instanceof Toggleable) {
                icon = Image.empty(Image.DEFAULT_ICON_SIZE);
            }

            if (icon != null) {
                int width = icon.getWidth();
                int height = icon.getHeight();
                if (myMaxIconWidth < width) {
                    myMaxIconWidth = width;
                }
                if (myMaxIconHeight < height) {
                    myMaxIconHeight = height;
                }
            }
        }
    }

    @Nonnull
    private CompletableFuture<Void> appendActionsFromGroup(@Nonnull ActionGroup actionGroup) {
        Predicate<AnAction> filter = action -> {
            if (myShowDisabled) {
                return true;
            }

            if (action instanceof AnSeparator) {
                return true;
            }
            Presentation presentation = myPresentationFactory.getPresentation(action);
            return presentation.isEnabledAndVisible();
        };

        return ActionGroupExpander.expandActionGroup(actionGroup, myPresentationFactory, myDataContext, myActionPlace, filter, myIndicator)
            .thenAccept(actions -> {
                calcMaxIconSize(actions);

                for (AnAction action : actions) {
                    appendAction(action);
                }
            });
    }

    private void appendAction(AnAction action) {
        Character mnemonic = null;
        if (myShowNumbers) {
            if (myCurrentNumber < 9) {
                mnemonic = Character.forDigit(myCurrentNumber + 1, 10);
            }
            else if (myCurrentNumber == 9) {
                mnemonic = '0';
            }
            else if (myUseAlphaAsNumbers) {
                mnemonic = (char) ('A' + myCurrentNumber - 10);
            }
            myCurrentNumber++;
        }

        ActionPopupItem item = new ActionPopupItem(action, mnemonic, myShowNumbers, myHonorActionMnemonics, myMaxIconWidth, myMaxIconHeight);

        item.updateFromPresentation(myPresentationFactory, myActionPlace);

        myListModel.add(item);
    }
}
