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

package com.intellij.execution;

import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ExecutionTarget {
  @Nonnull
  public abstract String getId();

  @Nonnull
  public abstract String getDisplayName();

  @Nullable
  public abstract Image getIcon();

  public abstract boolean canRun(@Nonnull RunnerAndConfigurationSettings configuration);

  @Override
  public boolean equals(Object obj) {
    return obj == this || (getClass().isInstance(obj) && getId().equals(((ExecutionTarget)obj).getId()));
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public String toString() {
    return getId();
  }
}
