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
package consulo.endpoint.impl.internal.client.generator;

import consulo.endpoint.client.generator.AvailableClientSettings;
import consulo.endpoint.client.generator.ClientGeneratorSetting;

public final class CurlClientSettings implements AvailableClientSettings {
    private final ClientGeneratorSetting mySetting = new ClientGeneratorSetting();

    @Override
    public boolean getBoilerplateAvailable() {
        return true;
    }

    @Override
    public ClientGeneratorSetting getActualClientSettings() {
        return mySetting;
    }
}
