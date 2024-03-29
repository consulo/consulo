/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.find.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import jakarta.inject.Inject;

@State(name = "FindRecents", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/find.recents.xml", roamingType = RoamingType.DISABLED)})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
final class FindRecents extends FindInProjectSettingsBase {
  public static FindRecents getInstance() {
    return ServiceManager.getService(FindRecents.class);
  }

  @Inject
  FindRecents() {
  }
}
