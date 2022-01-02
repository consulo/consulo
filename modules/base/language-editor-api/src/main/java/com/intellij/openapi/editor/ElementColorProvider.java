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
package com.intellij.openapi.editor;

import com.intellij.psi.PsiElement;
import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
@Deprecated
@DeprecationInfo("Use consulo.language.editor.ElementColorProvider, will be removed after migration to java9 modules")
public interface ElementColorProvider extends consulo.language.editor.ElementColorProvider {
  @Nullable
  @RequiredReadAction
  ColorValue getColorFrom(@Nonnull PsiElement element);

  @RequiredWriteAction
  void setColorTo(@Nonnull PsiElement element, @Nonnull ColorValue color);
}
