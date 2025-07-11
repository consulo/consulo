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
package consulo.execution.test.sm.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.test.sm.TestsLocationProviderUtil;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author VISTALL
 * @since 2022-07-21
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface SMTestHelper {
    @Nonnull
    List<TestsLocationProviderUtil.FileInfo> collectCandidates(Project project, String fileName, boolean includeNonProjectItems);
}
