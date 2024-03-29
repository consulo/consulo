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
package consulo.language.psi;

import consulo.annotation.DeprecationInfo;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a syntax error (for example, invalid token) in Java or custom language code.
 */
public interface PsiErrorElement extends PsiElement {
  /**
   * Returns the description of the error.
   *
   * @return the error description.
   */
  @Nullable
  @Deprecated
  @DeprecationInfo("Use #getErrorDescriptionValue()")
  default String getErrorDescription() {
    LocalizeValue errorDescriptionValue = getErrorDescriptionValue();
    return errorDescriptionValue == LocalizeValue.empty() ? null : errorDescriptionValue.toString();
  }

  /**
   * Returns the description of the error.
   *
   * @return the error description.
   */
  @Nonnull
  LocalizeValue getErrorDescriptionValue();
}