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
package com.intellij.openapi.vcs.changes.actions.diff;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.UserDataHolder;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.util.ThreeState;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ChangeDiffRequestProvider {
  ExtensionPointName<ChangeDiffRequestProvider> EP_NAME =
          ExtensionPointName.create("com.intellij.openapi.vcs.changes.actions.diff.ChangeDiffRequestProvider");

  @Nonnull
  ThreeState isEquals(@Nonnull Change change1, @Nonnull Change change2);

  boolean canCreate(@Nullable Project project, @Nonnull Change change);

  @Nonnull
  DiffRequest process(@Nonnull ChangeDiffRequestProducer presentable,
                      @Nonnull UserDataHolder context,
                      @Nonnull ProgressIndicator indicator) throws DiffRequestProducerException, ProcessCanceledException;
}
