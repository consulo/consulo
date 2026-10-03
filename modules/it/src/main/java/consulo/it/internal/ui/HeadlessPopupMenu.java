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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.PopupMenu;

/**
 * @author VISTALL
 */
public class HeadlessPopupMenu extends HeadlessMenu implements PopupMenu {
    private final Component myTarget;
    private boolean myShowing;
    private boolean myOpenOnClick;

    public HeadlessPopupMenu(Component target) {
        super(LocalizeValue.empty());
        myTarget = target;
    }

    public Component getTarget() {
        return myTarget;
    }

    public boolean isShowing() {
        return myShowing;
    }

    public boolean isOpenOnClick() {
        return myOpenOnClick;
    }

    @Override
    public void show(int relativeX, int relativeY) {
        myShowing = true;
    }

    @Override
    public void hide() {
        myShowing = false;
    }

    @Override
    public void setOpenOnClick(boolean openOnClick) {
        myOpenOnClick = openOnClick;
    }
}
