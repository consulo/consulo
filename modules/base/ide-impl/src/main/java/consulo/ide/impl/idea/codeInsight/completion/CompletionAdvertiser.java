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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.util.ProcessingContext;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.action.ActionManager;
import consulo.externalService.statistic.FeatureUsageTracker;
import jakarta.annotation.Nonnull;

/**
 * Controls the text to display at the bottom of lookup list
 *
 * @author peter
 */
public abstract class CompletionAdvertiser {

  @jakarta.annotation.Nullable
  public abstract String advertise(@Nonnull CompletionParameters parameters, ProcessingContext context);

  @jakarta.annotation.Nullable
  public abstract String handleEmptyLookup(@Nonnull CompletionParameters parameters, ProcessingContext context);

  protected static String getShortcut(String id) {
    return KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(id));
  }

  protected static boolean shouldShowFeature(CompletionParameters parameters, String id) {
    return FeatureUsageTracker.getInstance().isToBeAdvertisedInLookup(id, parameters.getPosition().getProject());
  }

}