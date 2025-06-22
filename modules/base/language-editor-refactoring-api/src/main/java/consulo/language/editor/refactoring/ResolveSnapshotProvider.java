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

package consulo.language.editor.refactoring;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Maxim.Mossienko
 * @since 2009-07-29
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ResolveSnapshotProvider implements LanguageExtension {
  private static final ExtensionPointCacheKey<ResolveSnapshotProvider, ByLanguageValue<ResolveSnapshotProvider>> KEY =
          ExtensionPointCacheKey.create("ResolveSnapshotProvider", LanguageOneToOne.build());

  @Nullable
  public static ResolveSnapshotProvider forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(ResolveSnapshotProvider.class).getOrBuildCache(KEY).get(language);
  }

  public abstract ResolveSnapshot createSnapshot(PsiElement scope);

  public static abstract class ResolveSnapshot {
    public abstract void apply(String name);
  }
}
