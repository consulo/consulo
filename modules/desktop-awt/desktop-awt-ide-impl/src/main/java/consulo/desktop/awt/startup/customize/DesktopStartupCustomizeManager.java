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
package consulo.desktop.awt.startup.customize;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.ide.impl.startup.customize.StartupCustomizeManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2021-09-01
 */
@Singleton
@ServiceImpl
public class DesktopStartupCustomizeManager implements StartupCustomizeManager {
    private final Application myApplication;

    @Inject
    public DesktopStartupCustomizeManager(Application application) {
        myApplication = application;
    }

    @RequiredUIAccess
    @Override
    public void showAsync(boolean firstShow) {
        FirstStartCustomizeUtil.showDialog(myApplication);
    }
}
