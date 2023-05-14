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
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureHandler;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows a custom language plugin to control the operation of refactorings for
 * files in the language.
 *
 * @author ven
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RefactoringSupportProvider implements LanguageExtension {
  private static final ExtensionPointCacheKey<RefactoringSupportProvider, ByLanguageValue<RefactoringSupportProvider>> KEY =
          ExtensionPointCacheKey.create("RefactoringSupportProvider", LanguageOneToOne.build(new RefactoringSupportProvider() {
            @Nonnull
            @Override
            public Language getLanguage() {
              return Language.ANY;
            }
          }));

  @Nonnull
  public static RefactoringSupportProvider forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(RefactoringSupportProvider.class).getOrBuildCache(KEY).requiredGet(language);
  }

  /**
   * Allows several providers to be available for the same language
   *
   * @param context refactoring context
   * @return true if refactoring support is available in given context
   */
  public boolean isAvailable(@Nonnull PsiElement context) {
    return true;
  }

  /**
   * Checks if the Safe Delete refactoring can be applied to the specified element
   * in the language. The Safe Delete refactoring also requires the plugin to implement
   * Find Usages functionality.
   *
   * @param element the element for which Safe Delete was invoked
   * @return true if Safe Delete is available, false otherwise.
   */
  public boolean isSafeDeleteAvailable(PsiElement element) {
    return false;
  }

  /**
   * @return handler for introducing local variables in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getIntroduceVariableHandler() {
    return null;
  }

  /**
   * @return handler for extracting methods in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getExtractMethodHandler() {
    return null;
  }

  /**
   * @return handler for introducing constants in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getIntroduceConstantHandler() {
    return null;
  }

  /**
   * @return handler for introducing fields in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getIntroduceFieldHandler() {
    return null;
  }

  /**
   * @return handler for introducing parameters in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getIntroduceParameterHandler() {
    return null;
  }

  /**
   * @return handler for pulling up members in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getPullUpHandler() {
    return null;
  }

  /**
   * @return handler for pushing down members in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getPushDownHandler() {
    return null;
  }

  /**
   * @return handler for extracting members to an interface in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getExtractInterfaceHandler() {
    return null;
  }

  /**
   * @return handler for extracting members to some module in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getExtractModuleHandler() {
    return null;
  }

  /**
   * @return handler for extracting super class in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getExtractSuperClassHandler() {
    return null;
  }

  /**
   * @return handler for changing signature in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public ChangeSignatureHandler getChangeSignatureHandler() {
    return null;
  }

  public boolean isInplaceRenameAvailable(PsiElement element, PsiElement context) {
    return false;
  }

  public boolean isInplaceIntroduceAvailable(PsiElement element, PsiElement context) {
    return false;
  }

  /**
   * @return handler for extracting [delegate] class in this language
   * @see RefactoringActionHandler
   */
  @Nullable
  public RefactoringActionHandler getExtractClassHandler() {
    return null;
  }

  public boolean isMemberInplaceRenameAvailable(PsiElement element, PsiElement context) {
    return false;
  }
}
