/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.search;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import consulo.lombok.annotations.ApplicationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredReadAction;

import java.util.List;

@ApplicationService
public abstract class PredefinedSearchScopeProvider {
  @RequiredReadAction
  public abstract List<SearchScope> getPredefinedScopes(@NotNull final Project project,
                                                        @Nullable final DataContext dataContext,
                                                        boolean suggestSearchInLibs,
                                                        boolean prevSearchFiles,
                                                        boolean currentSelection,
                                                        boolean usageView);
}
