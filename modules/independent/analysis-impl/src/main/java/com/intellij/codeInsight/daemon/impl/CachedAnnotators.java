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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageAnnotators;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

import java.util.Collection;
import java.util.List;

@Singleton
public class CachedAnnotators {
  private final ThreadLocalAnnotatorMap<String, Annotator> cachedAnnotators = new ThreadLocalAnnotatorMap<String, Annotator>() {
    @Nonnull
    @Override
    public Collection<Annotator> initialValue(@Nonnull String languageId) {
      Language language = Language.findLanguageByID(languageId);
      return language == null ? ContainerUtil.<Annotator>emptyList() : LanguageAnnotators.INSTANCE.allForLanguage(language);
    }
  };

  public CachedAnnotators(Project project) {
    ExtensionPointListener<Annotator> listener = new ExtensionPointListener<Annotator>() {
      @Override
      public void extensionAdded(@Nonnull Annotator extension, @javax.annotation.Nullable PluginDescriptor pluginDescriptor) {
        cachedAnnotators.clear();
      }

      @Override
      public void extensionRemoved(@Nonnull Annotator extension, @javax.annotation.Nullable PluginDescriptor pluginDescriptor) {
        cachedAnnotators.clear();
      }
    };
    LanguageAnnotators.INSTANCE.addListener(listener, project);
  }

  @Nonnull
  List<Annotator> get(@Nonnull String languageId) {
    return cachedAnnotators.get(languageId);
  }
}
