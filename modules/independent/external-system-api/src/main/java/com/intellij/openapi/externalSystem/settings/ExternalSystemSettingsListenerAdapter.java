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
package com.intellij.openapi.externalSystem.settings;

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 6/13/13 7:37 PM
 */
public abstract class ExternalSystemSettingsListenerAdapter<S extends ExternalProjectSettings> implements ExternalSystemSettingsListener<S> {

  @Override
  public void onProjectRenamed(@Nonnull String oldName, @Nonnull String newName) {
  }

  @Override
  public void onProjectsLinked(@Nonnull Collection<S> settings) {
  }

  @Override
  public void onProjectsUnlinked(@Nonnull Set<String> linkedProjectPaths) {
  }

  @Override
  public void onUseAutoImportChange(boolean currentValue, @Nonnull String linkedProjectPath) {
  }

  @Override
  public void onBulkChangeStart() {
  }

  @Override
  public void onBulkChangeEnd() {
  }
}
