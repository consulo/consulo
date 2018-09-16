/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * @author Alexander Koshevoy
 */
public interface PathMapper {

  boolean isEmpty();

  boolean canReplaceLocal(@Nonnull String localPath);

  @Nonnull
  String convertToLocal(@Nonnull String remotePath);

  boolean canReplaceRemote(@Nonnull String remotePath);

  @Nonnull
  String convertToRemote(@Nonnull String localPath);

  @Nonnull
  List<String> convertToRemote(@Nonnull Collection<String> paths);
}
