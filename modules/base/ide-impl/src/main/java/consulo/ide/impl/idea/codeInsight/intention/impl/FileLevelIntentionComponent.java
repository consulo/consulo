// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.GutterMark;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.intention.EmptyIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * @author max
 */
public class FileLevelIntentionComponent extends EditorNotificationPanel {
    private final Project myProject;

    @RequiredUIAccess
    public FileLevelIntentionComponent(final String description,
                                       @Nonnull HighlightSeverity severity,
                                       @Nullable GutterMark gutterMark,
                                       @Nullable final List<Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange>> intentions,
                                       @Nonnull final Project project,
                                       @Nonnull final PsiFile psiFile,
                                       @Nonnull final Editor editor,
                                       @Nullable String tooltip) {
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
            myGearButton.setVisible(true);
            myGearButton.setIcon(PlatformIconGroup.generalGearplain());
            myGearButton.addClickListener(event -> {
                CachedIntentions cachedIntentions = new CachedIntentions(project, psiFile, editor);
                IntentionListStep step = new IntentionListStep(null, editor, psiFile, project, cachedIntentions);
                HighlightInfoImpl.IntentionActionDescriptor descriptor = intentions.get(0).getFirst();
                IntentionActionWithTextCaching actionWithTextCaching = cachedIntentions.wrapAction(descriptor, psiFile, psiFile, editor);
                if (step.hasSubstep(actionWithTextCaching)) {
                    step = step.getSubStep(actionWithTextCaching, null);
                }

                ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
                
                popup.showBy(event.getComponent(), Objects.requireNonNull(event.getInputDetails()));
            });
        }
    }

    @Nonnull
    private static Color getColor(@Nonnull Project project, @Nonnull HighlightSeverity severity) {
        if (SeverityRegistrarImpl.getSeverityRegistrar(project).compare(severity, HighlightSeverity.ERROR) >= 0) {
            return LightColors.RED;
        }

        if (SeverityRegistrarImpl.getSeverityRegistrar(project).compare(severity, HighlightSeverity.WARNING) >= 0) {
            return LightColors.YELLOW;
        }

        return LightColors.GREEN;
    }
}
