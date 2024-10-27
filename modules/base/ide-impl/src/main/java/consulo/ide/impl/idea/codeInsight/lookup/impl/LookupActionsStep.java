/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.lookup.impl;

import consulo.ide.impl.idea.ui.popup.ClosableByLeftArrow;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementAction;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author peter
 */
public class LookupActionsStep extends BaseListPopupStep<LookupElementAction> implements ClosableByLeftArrow {
    private final LookupEx myLookup;
    private final LookupElement myLookupElement;
    private final Image myEmptyIcon;

    public LookupActionsStep(Collection<LookupElementAction> actions, LookupEx lookup, LookupElement lookupElement) {
        super(null, new ArrayList<>(actions));
        myLookup = lookup;
        myLookupElement = lookupElement;

        int w = 0, h = 0;
        for (LookupElementAction action : actions) {
            final Image icon = action.getIcon();
            if (icon != null) {
                w = Math.max(w, icon.getWidth());
                h = Math.max(h, icon.getHeight());
            }
        }
        myEmptyIcon = Image.empty(w, h);
    }

    @Override
    @RequiredUIAccess
    public PopupStep onChosen(LookupElementAction selectedValue, boolean finalChoice) {
        final LookupElementAction.Result result = selectedValue.performLookupAction();
        if (result == LookupElementAction.Result.HIDE_LOOKUP) {
            myLookup.hideLookup(true);
        }
        else if (result == LookupElementAction.Result.REFRESH_ITEM) {
            myLookup.updateLookupWidth();
            myLookup.requestResize();
            myLookup.refreshUi(false, true);
        }
        else if (result instanceof LookupElementAction.Result.ChooseItem chooseItem) {
            myLookup.setCurrentItem(chooseItem.item);
            CommandProcessor.getInstance().newCommand()
                .project(myLookup.getProject())
                .run(() -> myLookup.finishLookup(Lookup.AUTO_INSERT_SELECT_CHAR));
        }
        return FINAL_CHOICE;
    }

    @Override
    public Image getIconFor(LookupElementAction aValue) {
        return LookupIconUtil.augmentIcon(myLookup.getEditor(), aValue.getIcon(), myEmptyIcon);
    }

    @Nonnull
    @Override
    public String getTextFor(LookupElementAction value) {
        return value.getText();
    }
}