/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.language.codeStyle.arrangement;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.psi.codeStyle.arrangement.engine.ArrangementEngine;
import consulo.language.Language;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.arrangement.*;
import consulo.language.codeStyle.arrangement.match.ArrangementMatchRule;
import consulo.language.codeStyle.arrangement.match.ArrangementSectionRule;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsAware;
import consulo.language.psi.PsiElement;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author VISTALL
 * @since 2024-02-10
 */
@ServiceImpl
@Singleton
public class MemberOrderServiceImpl implements MemberOrderService {
  @RequiredReadAction
  @Override
  @Nullable
  public PsiElement getAnchor(@Nonnull PsiElement member, @Nonnull CommonCodeStyleSettings settings, @Nonnull PsiElement context) {
    Language language = context.getLanguage();
    Rearranger<?> rearranger = Rearranger.forLanguage(language);
    if (rearranger == null) {
      return null;
    }

    ArrangementSettings arrangementSettings = settings.getArrangementSettings();
    if (arrangementSettings == null && rearranger instanceof ArrangementStandardSettingsAware) {
      arrangementSettings = ((ArrangementStandardSettingsAware)rearranger).getDefaultSettings();
    }

    if (arrangementSettings == null) {
      return null;
    }

    Pair<? extends ArrangementEntry, ? extends List<? extends ArrangementEntry>> pair =
      rearranger.parseWithNew(context, null, Collections.singleton(context.getTextRange()), member, arrangementSettings);
    if (pair == null || pair.second.isEmpty()) {
      return null;
    }

    ArrangementEntry memberEntry = pair.first;
    List<? extends ArrangementEntry> entries = pair.second;
    ArrangementEntry parentEntry = entries.get(0);
    List<? extends ArrangementEntry> nonArranged = parentEntry.getChildren();
    List<ArrangementEntry> entriesWithNew = new ArrayList<ArrangementEntry>(nonArranged);
    entriesWithNew.add(memberEntry);
    //TODO: check insert new element
    final List<? extends ArrangementMatchRule> rulesByPriority = arrangementSettings.getRulesSortedByPriority();
    final List<ArrangementSectionRule> extendedSectionRules = ArrangementUtil.getExtendedSectionRules(arrangementSettings);
    List<ArrangementEntry> arranged = ArrangementEngine.arrange(entriesWithNew, extendedSectionRules, rulesByPriority, null);
    int i = arranged.indexOf(memberEntry);

    if (i <= 0) {
      return context;
    }

    ArrangementEntry anchorEntry = null;
    if (i >= arranged.size() - 1) {
      anchorEntry = nonArranged.get(nonArranged.size() - 1);
    }
    else {
      Set<ArrangementEntry> entriesBelow = new HashSet<ArrangementEntry>();
      entriesBelow.addAll(arranged.subList(i + 1, arranged.size()));
      for (ArrangementEntry entry : nonArranged) {
        if (entriesBelow.contains(entry)) {
          break;
        }
        anchorEntry = entry;
      }
    }

    if (anchorEntry == null) {
      return context;
    }

    int offset = anchorEntry.getEndOffset() - 1 - context.getTextRange().getStartOffset();
    PsiElement element = context.findElementAt(offset);
    for (PsiElement e = element; e != null && e.getTextRange().getStartOffset() >= anchorEntry.getStartOffset(); e = e.getParent()) {
      element = e;
    }
    return element;
  }
}
