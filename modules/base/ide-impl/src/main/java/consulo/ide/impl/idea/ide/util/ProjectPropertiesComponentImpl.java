/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.util;

import consulo.annotation.component.ServiceImpl;
import consulo.component.impl.BasePropertiesComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.ProjectPropertiesComponent;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
@State(name = "PropertiesComponent", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
public class ProjectPropertiesComponentImpl extends BasePropertiesComponent implements ProjectPropertiesComponent, PropertiesComponent {
}
