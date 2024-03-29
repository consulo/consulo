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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.impl.roots.ui.configuration.projectRoot.daemon.ApplicationStructureProblemsSettings;
import jakarta.inject.Singleton;

/**
 * @author nik
 */
@Singleton
@State(name = "ProjectStructureProblems", storages = {@Storage("structureProblems.xml")})
public class GlobalProjectStructureProblemsSettings extends ProjectStructureProblemsSettingsBase implements ApplicationStructureProblemsSettings {
}
