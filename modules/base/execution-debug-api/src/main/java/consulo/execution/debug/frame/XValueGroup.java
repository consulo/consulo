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
package consulo.execution.debug.frame;

import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents a group of values in a debugger tree.
 */
public abstract class XValueGroup extends XValueContainer {
  private final String myName;

  protected XValueGroup(@Nonnull String name) {
    myName = name;
  }

  @Nonnull
  public String getName() {
    return myName;
  }

  @Nullable
  public Image getIcon() {
    return null;
  }

  /**
   * @return {@code true} to automatically expand the group node when it's added to a tree
   */
  public boolean isAutoExpand() {
    return false;
  }

  /**
   * @return separator between the group name and the {@link #getComment() comment} in the node text
   */
  @Nonnull
  public String getSeparator() {
    return " = ";
  }

  /**
   * @return optional comment shown after the group name
   */
  @Nullable
  public String getComment() {
    return null;
  }
}