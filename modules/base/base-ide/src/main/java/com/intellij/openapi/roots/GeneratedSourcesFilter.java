/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.roots;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotation.access.RequiredReadAction;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class GeneratedSourcesFilter {
  public static final ExtensionPointName<GeneratedSourcesFilter> EP_NAME = ExtensionPointName.create("com.intellij.generatedSourcesFilter");

  @RequiredReadAction
  public abstract boolean isGeneratedSource(@Nonnull VirtualFile file, @Nonnull Project project);

  public static boolean isGeneratedSourceByAnyFilter(@Nonnull VirtualFile file, @Nonnull Project project) {
    return ReadAction.compute(() -> !project.isDisposed() && file.isValid() && EP_NAME.getExtensionList().stream().anyMatch(filter -> filter.isGeneratedSource(file, project)));
  }

  @RequiredReadAction
  @Deprecated
  public static boolean isGenerated(@Nonnull Project project, @Nonnull VirtualFile file) {
    return isGeneratedSourceByAnyFilter(file, project);
  }
}
