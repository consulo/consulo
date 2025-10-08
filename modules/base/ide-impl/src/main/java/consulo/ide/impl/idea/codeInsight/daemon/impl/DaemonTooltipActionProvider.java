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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.daemon.impl.tooltips.TooltipActionProvider;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.editor.impl.internal.hint.TooltipAction;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.intention.AbstractEmptyIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionActionWithTextCaching;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
@ExtensionImpl(id = "defaultProvider", order = "last")
public class DaemonTooltipActionProvider implements TooltipActionProvider {
  @Nullable
  @Override
  public TooltipAction getTooltipAction(@Nonnull HighlightInfo info, @Nonnull Editor editor, @Nonnull PsiFile psiFile) {
    IntentionAction intention = extractMostPriorityFixFromHighlightInfo((HighlightInfoImpl)info, editor, psiFile);

    if(intention == null) return null;

    return wrapIntentionToTooltipAction(intention, (HighlightInfoImpl)info);
  }

  private TooltipAction wrapIntentionToTooltipAction(IntentionAction intention, HighlightInfoImpl info) {
    Pair<IntentionActionDescriptor, RangeMarker> pair = ContainerUtil.find(info.quickFixActionMarkers, it -> it.getFirst().getAction() == intention);

    int offset = pair != null && pair.getSecond().isValid() ? pair.getSecond().getStartOffset() : info.getActualStartOffset();

     return new DaemonTooltipAction(intention.getText(), offset);
  }

  private static IntentionAction extractMostPriorityFixFromHighlightInfo(HighlightInfoImpl highlightInfo, Editor editor, PsiFile psiFile) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    List<IntentionActionDescriptor> fixes = new ArrayList<>();
    List<Pair<IntentionActionDescriptor, TextRange>> quickFixActionMarkers = highlightInfo.quickFixActionRanges;
    if (quickFixActionMarkers == null || quickFixActionMarkers.isEmpty()) return null;

    fixes.addAll(ContainerUtil.map(quickFixActionMarkers, (it) -> it.getFirst()));

    IntentionsInfo intentionsInfo = new IntentionsInfo();

    ShowIntentionsPass.fillIntentionsInfoForHighlightInfo(highlightInfo, intentionsInfo, fixes);

    intentionsInfo.filterActions(psiFile);

    return getFirstAvailableAction(psiFile, editor, intentionsInfo);
  }

  private static IntentionAction getFirstAvailableAction(PsiFile psiFile, Editor editor, IntentionsInfo intentionsInfo) {
    Project project = psiFile.getProject();

    //sort the actions
    CachedIntentions cachedIntentions = CachedIntentions.createAndUpdateActions(project, psiFile, editor, intentionsInfo);

    List<IntentionActionWithTextCaching> allActions = cachedIntentions.getAllActions();

    if (allActions.isEmpty()) return null;

    for (IntentionActionWithTextCaching it : allActions) {
      IntentionAction action = IntentionActionDelegate.unwrap(it.getAction());

      if (!(action instanceof AbstractEmptyIntentionAction) && action.isAvailable(project, editor, psiFile)) {
        LocalizeValue text = it.getText();
        //we cannot properly render html inside the fix button fixes with html text
        if (!XmlStringUtil.isWrappedInHtml(text.get())) {
          return action;
        }
      }
    }
    return null;
  }
}
