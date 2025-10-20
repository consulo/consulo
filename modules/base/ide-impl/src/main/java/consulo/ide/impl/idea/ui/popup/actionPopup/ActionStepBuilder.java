// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup.actionPopup;

import consulo.application.localize.ApplicationLocalize;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionGroupExpander;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.ui.popup.NothingHereAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    private int myMaxIconWidth = -1;
    private int myMaxIconHeight = -1;

    ActionStepBuilder(@Nonnull DataContext dataContext,
                      boolean showNumbers,
                      boolean useAlphaAsNumbers,
                      boolean showDisabled,
                      boolean honorActionMnemonics,
                      @Nullable String actionPlace,
                      @Nonnull PresentationFactory presentationFactory) {
        myUseAlphaAsNumbers = useAlphaAsNumbers;
        myPresentationFactory = presentationFactory;
        myListModel = new ArrayList<>();
        myDataContext = dataContext;
        myShowNumbers = showNumbers;
        myShowDisabled = showDisabled;
        myCurrentNumber = 0;
        myHonorActionMnemonics = honorActionMnemonics;
        myActionPlace = ObjectUtil.notNull(actionPlace, ActionPlaces.UNKNOWN);
    }

    @Nonnull
    public List<ActionPopupItem> getItems() {
        return myListModel;
    }

    public void buildGroup(@Nonnull ActionGroup actionGroup) {
        appendActionsFromGroup(actionGroup);

        if (myListModel.isEmpty()) {
            myListModel.add(new ActionPopupItem(NothingHereAction.INSTANCE, ApplicationLocalize.nothingHere()));
        }
    }

    private void calcMaxIconSize(List<AnAction> actions) {
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

    @RequiredUIAccess
    private void appendActionsFromGroup(@Nonnull ActionGroup actionGroup) {
        List<AnAction> actions =
            ActionGroupExpander.expandActionGroup(actionGroup, myPresentationFactory, myDataContext, myActionPlace, action -> {
                if (myShowDisabled) {
                    return true;
                }

                if (action instanceof AnSeparator) {
                    return true;
                }
                Presentation presentation = myPresentationFactory.getPresentation(action);
                return presentation.isEnabledAndVisible();
            });

        calcMaxIconSize(actions);

        for (AnAction action : actions) {
            appendAction(action);
        }
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
