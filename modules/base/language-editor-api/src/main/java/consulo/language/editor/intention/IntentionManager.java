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
package consulo.language.editor.intention;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.psi.PsiElement;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Manager for intentions. All intentions must be registered here.
 *
 * @see IntentionAction
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class IntentionManager  {
  /**
   * Key to be used within {@link UserDataHolder} in order to check presence of explicit indication on if intentions sub-menu
   * should be shown.
   */
  public static final Key<Boolean> SHOW_INTENTION_OPTIONS_KEY = Key.create("SHOW_INTENTION_OPTIONS_KEY");

  @Nonnull
  public static IntentionManager getInstance() {
    return Application.get().getInstance(IntentionManager.class);
  }

  /**
   * Registers an intention action.
   *
   * @param action the intention action to register.
   */
  @Deprecated(forRemoval = true)
  @DeprecationInfo("Use @ExtensionImpl for it")
  public abstract void addAction(@Nonnull IntentionAction action);

  /**
   * Returns all registered intention actions.
   *
   * @return array of registered actions.
   */
  @Nonnull
  public abstract IntentionAction[] getIntentionActions();

  /**
   * Returns all registered intention actions which are available now
   * (not disabled via Settings|Intentions or Alt-Enter|Disable intention quick fix)
   *
   * @return array of actions.
   */
  @Nonnull
  public abstract IntentionAction[] getAvailableIntentionActions();

  /**
   * @return actions used as additional options for the given problem.
   * E.g. actions for suppress the problem via comment, javadoc or annotation,
   * and edit corresponding inspection settings.
   */
  @Nonnull
  public abstract List<IntentionAction> getStandardIntentionOptions(@Nonnull HighlightDisplayKey displayKey, @Nonnull PsiElement context);

  /**
   * @return "Fix all '' inspections problems for a file" intention if toolWrapper is local inspection or simple global one
   */
  @Nullable
  public abstract IntentionAction createFixAllIntention(InspectionToolWrapper toolWrapper, IntentionAction action);

  /**
   * Wraps given action in a LocalQuickFix object.
   * @param action action to convert.
   * @return quick fix instance.
   */
  @Nonnull
  public abstract LocalQuickFix convertToFix(@Nonnull IntentionAction action);

  /**
   * @return intention to start code cleanup on file
   */
  @Nonnull
  public abstract IntentionAction createCleanupAllIntention();

  /**
   * @return options for cleanup intention {@link #createCleanupAllIntention()}
   * e.g. edit enabled cleanup inspections or starting cleanup on predefined scope
   */
  @Nonnull
  public abstract List<IntentionAction> getCleanupIntentionOptions();
}
