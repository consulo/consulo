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

package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class AdditionalCompileScopeProvider {
  public static final ExtensionPointName<AdditionalCompileScopeProvider> EXTENSION_POINT_NAME =
          ExtensionPointName.create("com.intellij.compiler.additionalCompileScopeProvider");

  @javax.annotation.Nullable
  public abstract CompileScope getAdditionalScope(@Nonnull CompileScope baseScope,
                                                  @Nonnull Condition<com.intellij.openapi.compiler.Compiler> filter,
                                                  @Nonnull Project project);
}
