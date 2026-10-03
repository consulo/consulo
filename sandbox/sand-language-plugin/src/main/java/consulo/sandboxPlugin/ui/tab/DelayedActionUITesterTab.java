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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.DelayedAction;
import consulo.ui.Label;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.VerticalLayout;

import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "delayedAction", order = "after inputs")
public class DelayedActionUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("DelayedAction");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(LocalizeValue.localizeTODO("The indicator is drawn where the click happened, for two seconds")));
        layout.add(Button.create(LocalizeValue.localizeTODO("Start"), e -> {
            DelayedAction action = DelayedAction.start(e);
            UIAccess.current().getScheduler().schedule(action::stop, 2, TimeUnit.SECONDS);
        }));
        return layout;
    }
}
