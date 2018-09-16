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

package com.intellij.lang.parameterInfo;

import com.intellij.codeInsight.lookup.LookupElement;
import javax.annotation.Nonnull;

public interface ParameterInfoHandler <ParameterOwner, ParameterType> {
  boolean couldShowInLookup();
  @javax.annotation.Nullable
  Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context);
  @javax.annotation.Nullable
  Object[] getParametersForDocumentation(ParameterType p, ParameterInfoContext context);

  // Find element for parameter info should also set ItemsToShow in context and may set highlighted element
  @javax.annotation.Nullable
  ParameterOwner findElementForParameterInfo(final CreateParameterInfoContext context);
  // Usually context.showHint
  void showParameterInfo(@Nonnull final ParameterOwner element, final CreateParameterInfoContext context);

  // Null returns leads to removing hint
  @javax.annotation.Nullable
  ParameterOwner findElementForUpdatingParameterInfo(final UpdateParameterInfoContext context);
  void updateParameterInfo(@Nonnull final ParameterOwner o, final UpdateParameterInfoContext context);

  // Can be null if parameter info does not track parameter index
  @javax.annotation.Nullable
  String getParameterCloseChars();
  boolean tracksParameterIndex();

  // context.setEnabled / context.setupUIComponentPresentation
  void updateUI(ParameterType p, ParameterInfoUIContext context);
}
