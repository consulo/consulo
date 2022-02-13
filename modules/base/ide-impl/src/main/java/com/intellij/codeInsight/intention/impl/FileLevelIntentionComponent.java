// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.intention.impl;

import consulo.editor.markup.GutterMark;
import consulo.language.editor.rawHighlight.impl.HighlightInfoImpl;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.EmptyIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.application.AllIcons;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.editor.Editor;
import consulo.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import com.intellij.openapi.util.Pair;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import com.intellij.ui.ClickListener;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.LightColors;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.TargetAWT;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author max
 */
public class FileLevelIntentionComponent extends EditorNotificationPanel {
  private final Project myProject;

  public FileLevelIntentionComponent(final String description,
                                     @Nonnull HighlightSeverity severity,
                                     @Nullable GutterMark gutterMark,
                                     @Nullable final List<Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange>> intentions,
                                     @Nonnull final Project project,
                                     @Nonnull final PsiFile psiFile,
                                     @Nonnull final Editor editor, @Nullable String tooltip) {
    super(getColor(project, severity));
    myProject = project;

    final ShowIntentionsPass.IntentionsInfo info = new ShowIntentionsPass.IntentionsInfo();

    if (intentions != null) {
      for (Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange> intention : intentions) {
        final HighlightInfoImpl.IntentionActionDescriptor descriptor = intention.getFirst();
        info.intentionsToShow.add(descriptor);
        final IntentionAction action = descriptor.getAction();
        if (action instanceof EmptyIntentionAction) {
          continue;
        }
        final String text = action.getText();
        createActionLabel(text, () -> {
          PsiDocumentManager.getInstance(myProject).commitAllDocuments();
          ShowIntentionActionsHandler.chooseActionAndInvoke(psiFile, editor, action, text);
        });
      }
    }

    myLabel.setText(description);
    myLabel.setToolTipText(tooltip);
    if (gutterMark != null) {
      myLabel.setIcon(TargetAWT.to(gutterMark.getIcon()));
    }

    if (intentions != null && !intentions.isEmpty()) {
      myGearLabel.setIcon(TargetAWT.to(AllIcons.General.GearPlain));

      new ClickListener() {
        @Override
        public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
          CachedIntentions cachedIntentions = new CachedIntentions(project, psiFile, editor);
          IntentionListStep step = new IntentionListStep(null, editor, psiFile, project, cachedIntentions);
          HighlightInfoImpl.IntentionActionDescriptor descriptor = intentions.get(0).getFirst();
          IntentionActionWithTextCaching actionWithTextCaching = cachedIntentions.wrapAction(descriptor, psiFile, psiFile, editor);
          if (step.hasSubstep(actionWithTextCaching)) {
            step = step.getSubStep(actionWithTextCaching, null);
          }
          ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
          Dimension dimension = popup.getContent().getPreferredSize();
          Point at = new Point(-dimension.width + myGearLabel.getWidth(), FileLevelIntentionComponent.this.getHeight());
          popup.show(new RelativePoint(e.getComponent(), at));
          return true;
        }
      }.installOn(myGearLabel);
    }
  }

  @Nonnull
  private static Color getColor(@Nonnull Project project, @Nonnull HighlightSeverity severity) {
    if (SeverityRegistrar.getSeverityRegistrar(project).compare(severity, HighlightSeverity.ERROR) >= 0) {
      return LightColors.RED;
    }

    if (SeverityRegistrar.getSeverityRegistrar(project).compare(severity, HighlightSeverity.WARNING) >= 0) {
      return LightColors.YELLOW;
    }

    return LightColors.GREEN;
  }
}
