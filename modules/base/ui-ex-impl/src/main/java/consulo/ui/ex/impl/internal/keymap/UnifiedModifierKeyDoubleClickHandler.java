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
package consulo.ui.ex.impl.internal.keymap;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.internal.KeyMapSetting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Shared by the frontends that have no event queue of their own - they feed
 * {@link #processKey(consulo.ui.event.details.KeyCode, java.util.Set, boolean)} from wherever they read keys
 * and install the data context along with it.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedModifierKeyDoubleClickHandler extends ModifierKeyDoubleClickHandlerBase {
    @Inject
    public UnifiedModifierKeyDoubleClickHandler(ActionManager actionManager, KeyMapSetting keyMapSetting) {
        super(actionManager, keyMapSetting);
    }
}
