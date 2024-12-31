/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.action.toolbar;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.dataContext.DataManager;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
@Singleton
@ServiceImpl
public class DesktopAWTActionToolbarFactory implements ActionToolbarFactory {
    private final Application myApplication;
    private final ActionManager myActionManager;
    private final KeymapManager myKeymapManager;
    private final DataManager myDataManager;

    @Inject
    public DesktopAWTActionToolbarFactory(Application application,
                                          ActionManager actionManager,
                                          KeymapManager keymapManager,
                                          DataManager dataManager) {
        myApplication = application;
        myActionManager = actionManager;
        myKeymapManager = keymapManager;
        myDataManager = dataManager;
    }

    @Nonnull
    @Override
    public ActionToolbar createActionToolbar(String place, ActionGroup group, @Nonnull ActionToolbar.Style style) {
        if (style == ActionToolbar.Style.INPLACE) {
            return new SimpleActionToolbarImpl(place, group, style);
        }

        if (style == ActionToolbar.Style.BUTTON)  {
            return new ActionButtonToolbarImpl(place, group, myApplication, myKeymapManager, myActionManager, myDataManager);
        }

        return new AdvancedActionToolbarImpl(place, group, style);
    }
}
