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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementers of the interface encapsulate optimize imports process for the language.
 *
 * @author max
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ImportOptimizer extends LanguageExtension {
  ExtensionPointCacheKey<ImportOptimizer, ByLanguageValue<List<ImportOptimizer>>> KEY = ExtensionPointCacheKey.create("ImportOptimizer", LanguageOneToMany.build(false));

  @Nonnull
  public static List<ImportOptimizer> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(ImportOptimizer.class).getOrBuildCache(KEY).requiredGet(language);
  }

  @Nonnull
  @RequiredReadAction
  public static Set<ImportOptimizer> forFile(PsiFile file) {
    Set<ImportOptimizer> optimizers = new HashSet<>();
    for (PsiFile psiFile : file.getViewProvider().getAllFiles()) {
      List<ImportOptimizer> langOptimizers = ImportOptimizer.forLanguage(psiFile.getLanguage());
      for (ImportOptimizer optimizer : langOptimizers) {
        if (optimizer != null && optimizer.supports(psiFile)) {
          optimizers.add(optimizer);
          break;
        }
      }
    }
    return optimizers;
  }

  /**
   * Call to this method is made before the <code>processFile()</code> call to ensure implementation can process the file given
   *
   * @param file file to check
   * @return <code>true</code> if implementation can handle the file
   */
  boolean supports(PsiFile file);

  /**
   * Implementers of the method are expected to perform all necessary calculations synchronously and return a <code>Runnable</code>,
   * which performs modifications based on preprocessing results.
   * processFile() is guaranteed to run with {@link Application#runReadAction(Runnable)} privileges and
   * the Runnable returned is guaranteed to run with {@link Application#runWriteAction(Runnable)} privileges.
   * <p>
   * One can theoretically delay all the calculation until Runnable is called but this code will be executed in Swing thread thus
   * lengthy calculations may block user interface for some significant time.
   *
   * @param file to optimize an imports in. It's guaranteed to have a language this <code>ImportOptimizer</code> have been
   *             issued from.
   * @return a <code>java.lang.Runnable</code> object, which being called will replace original file imports with optimized version.
   */
  @Nonnull
  Runnable processFile(PsiFile file);

  /**
   * @return action text, it will be displayed for user, default 'Optimize Imports...'
   */
  @Nonnull
  default String getActionName() {
    return CodeInsightBundle.message("not.action.OptimizeImports.text");
  }

  /**
   * @return action description, it will be displayed for user, default 'Remove unused imports and reorder/reorganize imports.'
   */
  @Nonnull
  default String getActionDescription() {
    return CodeInsightBundle.message("not.action.OptimizeImports.description");
  }

  /**
   * In order to customize notification popup after reformat code action just return it from {@link #processFile} with proper information,
   * by default "imports optimized" is shown.
   */
  interface CollectingInfoRunnable extends Runnable {
    @Nullable
    String getUserNotificationInfo();
  }
}
