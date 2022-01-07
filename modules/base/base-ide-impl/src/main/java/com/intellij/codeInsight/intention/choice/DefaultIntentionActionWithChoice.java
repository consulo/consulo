/*
 * Copyright 2013-2021 consulo.io
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
package com.intellij.codeInsight.intention.choice;

import com.intellij.codeInsight.intention.IntentionActionWithChoice;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.QuickFix;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Default intention action with choice that uses [ChoiceTitleIntentionAction]
 * and [ChoiceVariantIntentionAction] as title and variant action respectively.
 * <p>
 * In most cases this class should be used to create new choice-based intention actions.
 */
public interface DefaultIntentionActionWithChoice extends IntentionActionWithChoice<ChoiceTitleIntentionAction, ChoiceVariantIntentionAction> {
  @Nonnull
  default List<LocalQuickFix> getAllAsFixes() {
    List<LocalQuickFix> result = new ArrayList<LocalQuickFix>();
    result.add(getTitle());
    result.addAll(getVariants());

    if (ContainerUtil.map(result, QuickFix::getFamilyName).size() != 1) {
      throw new IllegalArgumentException("All default intention actions with choice are expected to have same family");
    }

    return result;
  }
}
