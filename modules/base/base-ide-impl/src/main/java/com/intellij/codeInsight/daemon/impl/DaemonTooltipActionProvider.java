/*
 * Copyright 2013-2019 consulo.io
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

import com.intellij.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import com.intellij.codeInsight.intention.AbstractEmptyIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.codeInsight.intention.impl.CachedIntentions;
import com.intellij.codeInsight.intention.impl.IntentionActionWithTextCaching;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.ex.TooltipAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.util.XmlStringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
public class DaemonTooltipActionProvider implements TooltipActionProvider {
  @Nullable
  @Override
  public TooltipAction getTooltipAction(@Nonnull HighlightInfo info, @Nonnull Editor editor, @Nonnull PsiFile psiFile) {
    IntentionAction intention = extractMostPriorityFixFromHighlightInfo(info, editor, psiFile);

    if(intention == null) return null;

    return wrapIntentionToTooltipAction(intention, info);
  }

  private TooltipAction wrapIntentionToTooltipAction(IntentionAction intention, HighlightInfo info) {
    Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker> pair = ContainerUtil.find(info.quickFixActionMarkers, it -> it.getFirst().getAction() == intention);

    int offset = pair != null && pair.getSecond().isValid() ? pair.getSecond().getStartOffset() : info.getActualStartOffset();

     return new DaemonTooltipAction(intention.getText(), offset);
  }

  private static IntentionAction extractMostPriorityFixFromHighlightInfo(HighlightInfo highlightInfo, Editor editor, PsiFile psiFile) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    List<HighlightInfo.IntentionActionDescriptor> fixes = new ArrayList<>();
    List<Pair<HighlightInfo.IntentionActionDescriptor, TextRange>> quickFixActionMarkers = highlightInfo.quickFixActionRanges;
    if (quickFixActionMarkers == null || quickFixActionMarkers.isEmpty()) return null;

    fixes.addAll(ContainerUtil.map(quickFixActionMarkers, (it) -> it.getFirst()));

    ShowIntentionsPass.IntentionsInfo intentionsInfo = new ShowIntentionsPass.IntentionsInfo();

    ShowIntentionsPass.fillIntentionsInfoForHighlightInfo(highlightInfo, intentionsInfo, fixes);

    intentionsInfo.filterActions(psiFile);

    return getFirstAvailableAction(psiFile, editor, intentionsInfo);
  }

  private static IntentionAction getFirstAvailableAction(PsiFile psiFile, Editor editor, ShowIntentionsPass.IntentionsInfo intentionsInfo) {
    Project project = psiFile.getProject();

    //sort the actions
    CachedIntentions cachedIntentions = CachedIntentions.createAndUpdateActions(project, psiFile, editor, intentionsInfo);

    List<IntentionActionWithTextCaching> allActions = cachedIntentions.getAllActions();

    if (allActions.isEmpty()) return null;

    for (IntentionActionWithTextCaching it : allActions) {
      IntentionAction action = IntentionActionDelegate.unwrap(it.getAction());

      if (!(action instanceof AbstractEmptyIntentionAction) && action.isAvailable(project, editor, psiFile)) {
        String text = it.getText();
        //we cannot properly render html inside the fix button fixes with html text
        if (!XmlStringUtil.isWrappedInHtml(text)) {
          return action;
        }
      }
    }
    return null;
  }
}
