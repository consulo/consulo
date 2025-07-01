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
package consulo.project.content;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionList;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import java.util.Set;

/**
 * @author anna
 * @since 2007-12-27
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface WatchedRootsProvider {
  @Nonnull
  @RequiredReadAction
  Set<String> getRootsToWatch();
}