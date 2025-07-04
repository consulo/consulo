/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.find;

import consulo.application.dumb.DumbAware;
import consulo.find.FindSearchContext;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.find.SearchSession;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-12-04
 */
public class ContextFilterActionGroup extends DefaultActionGroup implements DumbAware {
    private final Image myPrimaryImage;
    
    public ContextFilterActionGroup() {
        super(FindLocalize.findPopupShowFilterPopup(), true);

        myPrimaryImage = PlatformIconGroup.generalFilter();

        KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
        if (keyboardShortcut != null) {
            setShortcutSet(new CustomShortcutSet(keyboardShortcut));
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        SearchSession search = e.getData(SearchSession.KEY);
        if (search != null && search.getFindModel().getSearchContext() != FindSearchContext.ANY) {
            e.getPresentation().setIcon(ImageEffects.layered(myPrimaryImage, PlatformIconGroup.greenbadge()));
        } else {
            e.getPresentation().setIcon(myPrimaryImage);
        }
    }

    @Override
    public boolean showBelowArrow() {
        return false;
    }
}
