// Copyright 2000-2017 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package consulo.language.editor.parameterInfo;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ParameterInfoHandler<ParameterOwner, ParameterType> extends LanguageExtension {
  ExtensionPointCacheKey<ParameterInfoHandler, ByLanguageValue<List<ParameterInfoHandler>>> KEY =
          ExtensionPointCacheKey.create("ParameterInfoHandler", LanguageOneToMany.build(false));

  @Nonnull
  static List<ParameterInfoHandler> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(ParameterInfoHandler.class).getOrBuildCache(KEY).requiredGet(language);
  }

  boolean couldShowInLookup();

  @Nullable
  Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context);

  /**
   * <p>Find psiElement for parameter info should also set ItemsToShow in context and may set highlighted element</p>
   *
   * <p>Note: it is executed on non UI thread</p>
   */
  @Nullable
  ParameterOwner findElementForParameterInfo(@Nonnull CreateParameterInfoContext context);

  // Usually context.showHint
  void showParameterInfo(@Nonnull ParameterOwner element, @Nonnull CreateParameterInfoContext context);

  /**
   * <p>Hint has to be removed if method returns <code>null</code>.</p>
   *
   * <p>Note: it is executed on non-UI thread</p>
   */
  @Nullable
  ParameterOwner findElementForUpdatingParameterInfo(@Nonnull UpdateParameterInfoContext context);

  /**
   * This method performs some extra action (e.g. show hints) with a result of execution of
   * {@link #findElementForUpdatingParameterInfo(UpdateParameterInfoContext)} on UI thread.
   */
  default void processFoundElementForUpdatingParameterInfo(@Nullable ParameterOwner parameterOwner, @Nonnull UpdateParameterInfoContext context) {
  }

  /**
   * <p>Updates parameter info context due to change of caret position.</p>
   *
   * <p>It could update context and state of {@link UpdateParameterInfoContext#getObjectsToView()}</p>
   *
   * <p>Note: <code>context.getParameterOwner()</code> equals to <code>parameterOwner</code> or <code>null</code></p>
   *
   * <p>Note: it is executed on non UI thread.</p>
   */
  void updateParameterInfo(@Nonnull ParameterOwner parameterOwner, @Nonnull UpdateParameterInfoContext context);

  /**
   * <p>This method is executed on UI thread and supposed only to update UI representation using
   * {@link ParameterInfoUIContext#setUIComponentEnabled(boolean)} or {@link ParameterInfoUIContext#setupUIComponentPresentation(String, int, int, boolean, boolean, boolean, Color)}.</p>
   *
   * <p>Don't perform any heavy calculations like resolve here: move it to {@link #findElementForParameterInfo(CreateParameterInfoContext)} or
   * {@link #updateParameterInfo(Object, UpdateParameterInfoContext)}.</p>
   */
  void updateUI(ParameterType p, @Nonnull ParameterInfoUIContext context);

  default boolean supportsOverloadSwitching() {
    return false;
  }

  default void dispose(@Nonnull DeleteParameterInfoContext context) {
  }

  default boolean isWhitespaceSensitive() {
    return false;
  }

  default void syncUpdateOnCaretMove(@Nonnull UpdateParameterInfoContext context) {
  }

  /**
   * @deprecated not used
   */
  @Deprecated
  @Nullable
  default Object[] getParametersForDocumentation(ParameterType p, ParameterInfoContext context) {
    return null;
  }

  /**
   * @deprecated not used
   */
  @Deprecated
  @Nullable
  default String getParameterCloseChars() {
    return null;
  }

  /**
   * @deprecated not used
   */
  @Deprecated
  default boolean tracksParameterIndex() {
    return false;
  }
}
