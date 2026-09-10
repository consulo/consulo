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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationPropertiesComponent;
import consulo.component.impl.internal.BasePropertiesComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;

/**
 * Headless {@code ApplicationPropertiesComponent}: the production impl lives in {@code ide-impl}; this
 * mirrors it on top of the shared {@link BasePropertiesComponent} store. The index gist manager reads its
 * reindex counter from it when the first scan runs.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
@State(name = "PropertiesComponent", storages = @Storage("options"))
public class HeadlessApplicationPropertiesComponent extends BasePropertiesComponent implements ApplicationPropertiesComponent {
}
