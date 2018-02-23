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
package com.intellij.pom.tree.events;

import com.intellij.lang.ASTNode;
import javax.annotation.Nonnull;

/**
 * @author ik
 */
public interface TreeChange {
  void addChange(ASTNode child, @Nonnull ChangeInfo changeInfo);

  @Nonnull
  ASTNode[] getAffectedChildren();

  ChangeInfo getChangeByChild(ASTNode child);

  int getChildOffsetInNewTree(@Nonnull ASTNode child);

  void composite(@Nonnull TreeChange treeChange);

  boolean isEmpty();

  void removeChange(ASTNode beforeEqualDepth);

  void add(@Nonnull TreeChange value);

  int getOldLength();
}
