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
package consulo.ide.impl.idea.ui.popup;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-12-05
 */
public class ClosePopupAction extends DumbAwareAction {
    @Nonnull
    private final AbstractPopup myPopup;

    public ClosePopupAction(@Nonnull LocalizeValue text, @Nonnull Image icon, @Nonnull AbstractPopup popup) {
        super(text, LocalizeValue.of(), icon);
        myPopup = popup;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        myPopup.cancel(e.getInputEvent());
    }
}
