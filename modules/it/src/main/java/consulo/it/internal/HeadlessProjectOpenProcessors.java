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

import consulo.annotation.component.ServiceImpl;
import consulo.project.internal.ProjectOpenProcessor;
import consulo.project.internal.ProjectOpenProcessors;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Headless {@link ProjectOpenProcessors}: no processors, so {@code findProcessor} returns null and
 * the open flow falls back to opening the directory directly as a project.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl
public class HeadlessProjectOpenProcessors implements ProjectOpenProcessors {
    @Override
    public List<ProjectOpenProcessor> getProcessors() {
        return List.of();
    }
}
