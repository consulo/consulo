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
package consulo.desktop.awt.ui.keymap;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.platform.Platform;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.ModifiedInputDetails.Modifier;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.impl.internal.keymap.ModifierKeyDoubleClickHandlerBase;
import consulo.ui.ex.internal.KeyMapSetting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 2026-08-01
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class DesktopAWTModifierKeyDoubleClickHandler extends ModifierKeyDoubleClickHandlerBase {
    @Inject
    public DesktopAWTModifierKeyDoubleClickHandler(ActionManager actionManager, KeyMapSetting keyMapSetting) {
        super(actionManager, keyMapSetting);

        // one dispatcher for every registration - the state machines sort out which of them the key belongs to
        IdeEventQueueProxy.getInstance().addDispatcher(event -> event instanceof KeyEvent keyEvent && dispatch(keyEvent), null);
    }

    /**
     * Control is taken by the system on a mac, so the multi caret actions move to alt there.
     */
    @Override
    protected Modifier getMultiCaretActionModifier() {
        return Platform.current().os().isMac() ? Modifier.ALT : Modifier.CTRL;
    }

    private boolean dispatch(KeyEvent event) {
        int id = event.getID();
        if (id != KeyEvent.KEY_PRESSED && id != KeyEvent.KEY_RELEASED) {
            return false;
        }

        return processKey(
            KeyCode.of(event.getKeyCode()),
            DesktopAWTInputDetails.toModifiers(event),
            id == KeyEvent.KEY_PRESSED
        );
    }

    @Override
    protected DataContext createDataContext() {
        return DataManager.getInstance().getDataContext(IdeFocusManager.findInstance().getFocusOwner());
    }
}
