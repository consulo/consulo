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
package consulo.externalService.impl.internal.startup;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeStepProvider;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@ExtensionImpl(id = "keymap", order = "after theme")
public class KeymapStartupCustomizeStepProvider implements StartupCustomizeStepProvider {
    @Override
    public void collectSteps(StartupCustomizeContext context, Consumer<StartupCustomizeStep> consumer) {
        consumer.accept(new KeymapStartupCustomizeStep());
    }
}
