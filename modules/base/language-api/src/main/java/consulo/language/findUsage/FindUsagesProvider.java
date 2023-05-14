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
package consulo.language.findUsage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.cacheBuilder.SimpleWordsScanner;
import consulo.language.cacheBuilder.WordsScanner;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines the support for the "Find Usages" feature in a custom language.
 *
 * @author max
 * @see #forLanguage(Language)
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FindUsagesProvider extends LanguageExtension {
  ExtensionPointCacheKey<FindUsagesProvider, ByLanguageValue<FindUsagesProvider>> KEY =
          ExtensionPointCacheKey.create("FindUsagesProvider", LanguageOneToOne.build(new EmptyFindUsagesProvider()));

  @Nonnull
  static FindUsagesProvider forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(FindUsagesProvider.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Gets the word scanner for building a word index for the specified language.
   * Note that the implementation MUST be thread-safe, otherwise you should return a new instance of your scanner
   * (that can be recommended as a best practice).
   *
   * @return the word scanner implementation, or null if {@link SimpleWordsScanner} is OK.
   */
  @Nullable
  default WordsScanner getWordsScanner() {
    return new SimpleWordsScanner();
  }

  /**
   * Checks if it makes sense to search for usages of the specified element.
   *
   * @param psiElement the element for which usages are searched.
   * @return true if the search is allowed, false otherwise.
   * @see FindManager#canFindUsages(PsiElement)
   */
  boolean canFindUsagesFor(@Nonnull PsiElement psiElement);

  /**
   * Returns the ID of the help topic which is shown when the specified element is selected
   * in the "Find Usages" dialog.
   *
   * @param psiElement the element for which the help topic is requested.
   * @return the help topic ID, or null if no help is available.
   */
  @Nullable
  default String getHelpId(@Nonnull PsiElement psiElement) {
    return "reference.dialogs.findUsages.other";
  }

  /**
   * Returns the user-visible type of the specified element, shown in the "Find Usages"
   * dialog (for example, "class" or "variable"). The type name should not be upper-cased.
   *
   * @param element the element for which the type is requested.
   * @return the type of the element.
   */
  @Nonnull
  String getType(@Nonnull PsiElement element);

  /**
   * Returns an expanded user-visible name of the specified element, shown in the "Find Usages"
   * dialog. For classes, this can return a fully qualified name of the class; for methods -
   * a signature of the method with parameters.
   *
   * @param element the element for which the name is requested.
   * @return the user-visible name.
   */
  @Nonnull
  String getDescriptiveName(@Nonnull PsiElement element);

  /**
   * Returns the text representing the specified PSI element in the Find Usages tree.
   *
   * @param element     the element for which the node text is requested.
   * @param useFullName if true, the returned text should use fully qualified names
   * @return the text representing the element.
   */
  @Nonnull
  String getNodeText(@Nonnull PsiElement element, boolean useFullName);
}
