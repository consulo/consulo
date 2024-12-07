// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.ui.components.panels.VerticalBox;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class TrafficProgressPanel extends JPanel {
  static final String MAX_TEXT = "100%";
  private static final String MIN_TEXT = "0%";

  private final JLabel statistics = new JLabel();
  private final Map<JProgressBar, JLabel> myProgressToText = new HashMap<>();

  private final JLabel statusLabel = new JLabel();
  private final JLabel statusExtraLineLabel = new JLabel();
  @Nonnull
  private final TrafficLightRenderer myTrafficLightRenderer;

  private final JPanel myPassStatuses = new JPanel();
  private final JPanel myEmptyPassStatuses = new NonOpaquePanel();
  private final Wrapper myPassStatusesContainer = new Wrapper();

  @Nonnull
  private final HintHint myHintHint;

  TrafficProgressPanel(@Nonnull TrafficLightRenderer trafficLightRenderer, @Nonnull Editor editor, @Nonnull HintHint hintHint) {
    myHintHint = hintHint;
    myTrafficLightRenderer = trafficLightRenderer;

    setLayout(new BorderLayout());

    VerticalBox center = new VerticalBox();

    add(center, BorderLayout.NORTH);
    center.add(statusLabel);
    center.add(statusExtraLineLabel);
    center.add(new Separator());
    center.add(Box.createVerticalStrut(6));

    TrafficLightRenderer.DaemonCodeAnalyzerStatus fakeStatusLargeEnough = new TrafficLightRenderer.DaemonCodeAnalyzerStatus();
    fakeStatusLargeEnough.errorCount = new int[]{1, 1, 1, 1};
    Project project = trafficLightRenderer.getProject();
    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    fakeStatusLargeEnough.passes = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      fakeStatusLargeEnough.passes.add(
        new ProgressableTextEditorHighlightingPass(
          project,
          editor.getDocument(),
          DaemonLocalize.passWolf().get(),
          psiFile,
          editor,
          TextRange.EMPTY_RANGE,
          false,
          HighlightInfoProcessor.getEmpty()
        ) {
          @Override
          protected void collectInformationWithProgress(@Nonnull ProgressIndicator progress) {
          }

          @Override
          protected void applyInformationWithProgress() {
          }
        }
      );
    }
    center.add(myPassStatusesContainer);

    add(statistics, BorderLayout.SOUTH);
    updatePanel(fakeStatusLargeEnough, true);

    hintHint.initStyle(this, true);
    statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
  }

  int getMinWidth() {
    return Math.max(Math.max(Math.max(getLabelMinWidth(statistics), getLabelMinWidth(statusExtraLineLabel)), getLabelMinWidth(statusLabel)), getLabelMinWidth(new JLabel(CodeInsightBundle.message("label.slow.inspections.progress.report.long.line"))));
  }

  private int getLabelMinWidth(@Nonnull JLabel label) {
    String text = label.getText();
    Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();

    if (icon == null && StringUtil.isEmpty(text)) {
      return 0;
    }

    Rectangle paintIconR = new Rectangle();
    Rectangle paintTextR = new Rectangle();
    Rectangle paintViewR = new Rectangle(10000, 10000);

    SwingUtilities.layoutCompoundLabel(label, getFontMetrics(getFont()), text, icon, label.getVerticalAlignment(), label.getHorizontalAlignment(), label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
                                       paintViewR, paintIconR, paintTextR, label.getIconTextGap());

    return paintTextR.width;
  }

  private class Separator extends NonOpaquePanel {
    @Override
    protected void paintComponent(@Nonnull Graphics g) {
      Insets insets = getInsets();
      if (insets == null) {
        insets = JBUI.emptyInsets();
      }
      g.setColor(myHintHint.getTextForeground());
      g.drawLine(insets.left, insets.top, getWidth() - insets.left - insets.right, insets.top);
    }

    @Nonnull
    @Override
    public Dimension getPreferredSize() {
      return new Dimension(1, 1);
    }

    @Nonnull
    @Override
    public Dimension getMinimumSize() {
      return new Dimension(1, 1);
    }
  }

  void updatePanel(@Nonnull TrafficLightRenderer.DaemonCodeAnalyzerStatus status, boolean isFake) {
    try {
      boolean needRebuild = myTrafficLightRenderer.updatePanel(status);
      statusLabel.setText(myTrafficLightRenderer.statusLabel);
      if (myTrafficLightRenderer.statusExtraLine == null) {
        statusExtraLineLabel.setVisible(false);
      }
      else {
        statusExtraLineLabel.setText(myTrafficLightRenderer.statusExtraLine);
        statusExtraLineLabel.setVisible(true);
      }
      myPassStatuses.setVisible(myTrafficLightRenderer.passStatusesVisible);
      statistics.setText(myTrafficLightRenderer.statistics);
      resetProgressBars(myTrafficLightRenderer.progressBarsEnabled, myTrafficLightRenderer.progressBarsCompleted);

      if (needRebuild) {
        // passes set has changed
        rebuildPassesProgress(status);
      }

      for (ProgressableTextEditorHighlightingPass pass : status.passes) {
        double progress = pass.getProgress();
        Pair<JProgressBar, JLabel> pair = myTrafficLightRenderer.passes.get(pass);
        JProgressBar progressBar = pair.first;
        int percent = (int)Math.round(progress * TrafficLightRenderer.MAX);
        if (percent == 100 && !pass.isFinished()) {
          percent = 99;
        }
        progressBar.setValue(percent);
        JLabel percentage = pair.second;
        percentage.setText(percent + "%");
      }
    }
    finally {
      if (isFake) {
        myEmptyPassStatuses.setPreferredSize(myPassStatuses.getPreferredSize());
        myPassStatusesContainer.setContent(myEmptyPassStatuses);
      }
      else {
        myPassStatusesContainer.setContent(myPassStatuses);
      }
    }
  }

  private void resetProgressBars(final boolean enabled, @Nullable final Boolean completed) {
    for (JProgressBar progress : UIUtil.uiTraverser(myPassStatuses).traverse().filter(JProgressBar.class)) {
      progress.setEnabled(enabled);
      if (completed != null) {
        if (completed) {
          progress.setValue(TrafficLightRenderer.MAX);
          myProgressToText.get(progress).setText(MAX_TEXT);
        }
        else {
          progress.setValue(0);
          myProgressToText.get(progress).setText(MIN_TEXT);
        }
      }
    }
  }

  private void rebuildPassesProgress(@Nonnull TrafficLightRenderer.DaemonCodeAnalyzerStatus status) {
    myPassStatuses.removeAll();
    myPassStatuses.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    for (ProgressableTextEditorHighlightingPass pass : status.passes) {
      JLabel label = new JLabel(pass.getPresentableName() + ": ");
      label.setHorizontalTextPosition(SwingConstants.RIGHT);

      Pair<JProgressBar, JLabel> pair = myTrafficLightRenderer.passes.get(pass);
      JProgressBar progressBar = pair.getFirst();
      UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, progressBar);
      JLabel percLabel = pair.getSecond();
      myProgressToText.put(progressBar, percLabel);
      c.gridx = 0;
      myPassStatuses.add(label, c);
      c.gridx = 1;
      myPassStatuses.add(progressBar, c);
      c.gridx = 2;
      c.weightx = 1;
      myPassStatuses.add(percLabel, c);

      c.gridy++;
    }

    myHintHint.initStyle(myPassStatuses, true);
  }
}
