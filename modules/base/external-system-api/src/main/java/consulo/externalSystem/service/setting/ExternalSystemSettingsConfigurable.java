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
package consulo.externalSystem.service.setting;

import consulo.configurable.UnnamedConfigurable;
import consulo.disposer.Disposable;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;

/**
 * Base for a configurable which edits a single external system settings object.
 * <p/>
 * The settings object is the model of the configurable: it is given at construction, the user interface is built from it in
 * {@link #createUIComponent(Disposable)} and written back to it in {@link #apply()}. There is no separate step which pushes the
 * settings into the user interface, so the component is usable as soon as it is created.
 * <p/>
 * {@link AbstractExternalSystemSettings#setLinkedProjectsSettings} reports changes by comparing the stored settings object with the
 * given one. A caller which relies on those notifications must therefore hand a detached copy to this constructor and publish a copy
 * of it after {@link #apply()} - passing the stored object itself leaves both sides of that comparison on the same instance and no
 * listener is notified.
 *
 * @author VISTALL
 */
public abstract class ExternalSystemSettingsConfigurable<S> implements UnnamedConfigurable {
    private final S mySettings;

    protected ExternalSystemSettingsConfigurable(S settings) {
        mySettings = settings;
    }

    protected S getSettings() {
        return mySettings;
    }
}
