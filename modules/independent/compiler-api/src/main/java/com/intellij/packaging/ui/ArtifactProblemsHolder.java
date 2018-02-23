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
package com.intellij.packaging.ui;

import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public interface ArtifactProblemsHolder {
  @Nonnull
  PackagingElementResolvingContext getContext();

  void registerError(@Nonnull String message, @Nonnull String problemTypeId);

  void registerError(@Nonnull String message, @Nonnull String problemTypeId, @javax.annotation.Nullable List<PackagingElement<?>> pathToPlace,
                     @Nonnull ArtifactProblemQuickFix... quickFixes);

  void registerWarning(@Nonnull String message, @Nonnull String problemTypeId, @javax.annotation.Nullable List<PackagingElement<?>> pathToPlace,
                       @Nonnull ArtifactProblemQuickFix... quickFixes);
}
