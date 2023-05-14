/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.language.editor.hint.HintManager;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.hint.LineTooltipRenderer;
import consulo.language.editor.impl.internal.hint.TooltipGroup;
import consulo.codeEditor.Editor;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.language.editor.impl.internal.markup.TrafficTooltipRenderer;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.ui.ex.awt.HintHint;
import consulo.language.editor.impl.internal.hint.HintListener;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.component.util.ComparableObject;

import jakarta.annotation.Nonnull;
import java.awt.*;
import java.util.EventObject;

/**
 * User: cdr
 */
class TrafficTooltipRendererImpl extends ComparableObject.Impl implements TrafficTooltipRenderer {
  private TrafficProgressPanel myPanel;
  private final Runnable onHide;
  private TrafficLightRenderer myTrafficLightRenderer;

  TrafficTooltipRendererImpl(@Nonnull Runnable onHide, @Nonnull Editor editor) {
    super(editor);
    this.onHide = onHide;
  }

  @Override
  public void repaintTooltipWindow() {
    if (myPanel != null) {
      SeverityRegistrarImpl severityRegistrar = (SeverityRegistrarImpl)SeverityRegistrarImpl.getSeverityRegistrar(myTrafficLightRenderer.getProject());
      TrafficLightRenderer.DaemonCodeAnalyzerStatus status = myTrafficLightRenderer.getDaemonCodeAnalyzerStatus(severityRegistrar);
      myPanel.updatePanel(status, false);
    }
  }

  @Override
  public LightweightHint show(@Nonnull Editor editor, @Nonnull Point p, boolean alignToRight, @Nonnull TooltipGroup group, @Nonnull HintHint hintHint) {
    myTrafficLightRenderer = (TrafficLightRenderer)((EditorMarkupModel)editor.getMarkupModel()).getErrorStripeRenderer();
    myPanel = new TrafficProgressPanel(myTrafficLightRenderer, editor, hintHint);
    repaintTooltipWindow();
    LineTooltipRenderer.correctLocation(editor, myPanel, p, alignToRight, true, myPanel.getMinWidth());
    LightweightHint hint = new LightweightHint(myPanel);

    HintManagerImpl hintManager = (HintManagerImpl)HintManager.getInstance();
    hintManager.showEditorHint(hint, editor, p,
                               HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_OTHER_HINT |
                               HintManager.HIDE_BY_SCROLLING, 0, false, hintHint);
    hint.addHintListener(new HintListener() {
      @Override
      public void hintHidden(EventObject event) {
        if (myPanel == null) return; //double hide?
        myPanel = null;
        onHide.run();
      }
    });
    return hint;
  }
}
