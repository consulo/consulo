/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.compiler.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.compiler.Compiler;
import consulo.compiler.scope.CompileScope;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author nik
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class AdditionalCompileScopeProvider {
  public static final ExtensionPointName<AdditionalCompileScopeProvider> EXTENSION_POINT_NAME = ExtensionPointName.create(AdditionalCompileScopeProvider.class);

  @Nullable
  public abstract CompileScope getAdditionalScope(@Nonnull CompileScope baseScope, @Nonnull Predicate<Compiler> filter, @Nonnull Project project);
}
