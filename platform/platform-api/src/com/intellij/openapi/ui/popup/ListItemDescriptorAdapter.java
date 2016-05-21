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
package com.intellij.openapi.ui.popup;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class ListItemDescriptorAdapter implements ListItemDescriptor {
  @Nullable
  @Override
  public String getCaptionAboveOf(Object value) {
    return null;
  }

  @Nullable
  @Override
  public String getTooltipFor(Object value) {
    return null;
  }

  @Override
  public Icon getIconFor(Object value) {
    return null;
  }

  @Override
  public boolean hasSeparatorAboveOf(Object value) {
    return false;
  }
}