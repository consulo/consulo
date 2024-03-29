// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.intention.impl;

import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.intention.AbstractEmptyIntentionAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionActionDelegate;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.component.util.Iconable;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.editor.util.PsiUtilBase;
import consulo.logging.Logger;
import consulo.ui.ex.popup.ListSeparator;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.SpeedSearchFilter;
import consulo.ui.image.Image;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author cdr
 */
public class IntentionListStep implements ListPopupStep<IntentionActionWithTextCaching>, SpeedSearchFilter<IntentionActionWithTextCaching> {
  private static final Logger LOG = Logger.getInstance(IntentionListStep.class);

  private final CachedIntentions myCachedIntentions;
  @Nullable
  private final IntentionHintComponent myIntentionHintComponent;

  private Runnable myFinalRunnable;
  private final Project myProject;
  private final PsiFile myFile;
  @Nullable
  private final Editor myEditor;

  public IntentionListStep(@Nullable IntentionHintComponent intentionHintComponent, @Nullable Editor editor, @Nonnull PsiFile file, @Nonnull Project project, CachedIntentions intentions) {
    myIntentionHintComponent = intentionHintComponent;
    myProject = project;
    myFile = file;
    myEditor = editor;
    myCachedIntentions = intentions;
  }

  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public boolean isSelectable(final IntentionActionWithTextCaching action) {
    return true;
  }

  @Override
  public PopupStep onChosen(IntentionActionWithTextCaching action, final boolean finalChoice) {
    IntentionAction a = IntentionActionDelegate.unwrap(action.getAction());

    if (finalChoice && !(a instanceof AbstractEmptyIntentionAction)) {
      applyAction(action);
      return FINAL_CHOICE;
    }

    if (hasSubstep(action)) {
      return getSubStep(action, action.getToolName());
    }

    return FINAL_CHOICE;
  }

  @Override
  public Runnable getFinalRunnable() {
    return myFinalRunnable;
  }

  private void applyAction(@Nonnull IntentionActionWithTextCaching cachedAction) {
    myFinalRunnable = () -> {
      HintManager.getInstance().hideAllHints();
      if (myProject.isDisposed()) return;
      if (myEditor != null && (myEditor.isDisposed() || (!myEditor.getComponent().isShowing() && !ApplicationManager.getApplication().isUnitTestMode()))) return;

      if (DumbService.isDumb(myProject) && !DumbService.isDumbAware(cachedAction)) {
        DumbService.getInstance(myProject).showDumbModeNotification(cachedAction.getText() + " is not available during indexing");
        return;
      }

      PsiDocumentManager.getInstance(myProject).commitAllDocuments();

      PsiFile file = myEditor != null ? PsiUtilBase.getPsiFileInEditor(myEditor, myProject) : myFile;
      if (file == null) {
        return;
      }

      ShowIntentionActionsHandler.chooseActionAndInvoke(file, myEditor, cachedAction.getAction(), cachedAction.getText(), myProject);
    };
  }


  @Nonnull
  IntentionListStep getSubStep(@Nonnull IntentionActionWithTextCaching action, final String title) {
    ShowIntentionsPass.IntentionsInfo intentions = new ShowIntentionsPass.IntentionsInfo();
    for (final IntentionAction optionIntention : action.getOptionIntentions()) {
      intentions.intentionsToShow.add(new HighlightInfoImpl.IntentionActionDescriptor(optionIntention, getIcon(optionIntention)));
    }
    for (final IntentionAction optionFix : action.getOptionErrorFixes()) {
      intentions.errorFixesToShow.add(new HighlightInfoImpl.IntentionActionDescriptor(optionFix, getIcon(optionFix)));
    }
    for (final IntentionAction optionFix : action.getOptionInspectionFixes()) {
      intentions.inspectionFixesToShow.add(new HighlightInfoImpl.IntentionActionDescriptor(optionFix, getIcon(optionFix)));
    }

    return new IntentionListStep(myIntentionHintComponent, myEditor, myFile, myProject, CachedIntentions.create(myProject, myFile, myEditor, intentions)) {
      @Override
      public String getTitle() {
        return title;
      }
    };
  }

  private static Image getIcon(IntentionAction optionIntention) {
    return optionIntention instanceof Iconable ? ((Iconable)optionIntention).getIcon(0) : null;
  }

  @TestOnly
  public Map<IntentionAction, List<IntentionAction>> getActionsWithSubActions() {
    Map<IntentionAction, List<IntentionAction>> result = new LinkedHashMap<>();

    for (IntentionActionWithTextCaching cached : getValues()) {
      IntentionAction action = cached.getAction();
      if (ShowIntentionActionsHandler.chooseFileForAction(myFile, myEditor, action) == null) continue;

      List<IntentionActionWithTextCaching> subActions = getSubStep(cached, cached.getToolName()).getValues();
      List<IntentionAction> options =
              subActions.stream().map(IntentionActionWithTextCaching::getAction).filter(option -> ShowIntentionActionsHandler.chooseFileForAction(myFile, myEditor, option) != null).collect(Collectors.toList());
      result.put(action, options);
    }
    return result;
  }

  @Override
  public boolean hasSubstep(final IntentionActionWithTextCaching action) {
    return action.getOptionIntentions().size() + action.getOptionErrorFixes().size() > 0;
  }

  @Override
  @Nonnull
  public List<IntentionActionWithTextCaching> getValues() {
    return myCachedIntentions.getAllActions();
  }

  @Override
  @Nonnull
  public String getTextFor(final IntentionActionWithTextCaching action) {
    final String text = action.getText();
    if (LOG.isDebugEnabled() && text.startsWith("<html>")) {
      LOG.info("IntentionAction.getText() returned HTML: action=" + action.getAction().getClass() + " text=" + text);
    }
    return text;
  }

  @Override
  public Image getIconFor(final IntentionActionWithTextCaching value) {
    return myCachedIntentions.getIcon(value);
  }

  @Override
  public void canceled() {
    if (myIntentionHintComponent != null) {
      myIntentionHintComponent.canceled(this);
    }
  }

  @Override
  public int getDefaultOptionIndex() {
    return 0;
  }

  @Override
  public ListSeparator getSeparatorAbove(final IntentionActionWithTextCaching value) {
    List<IntentionActionWithTextCaching> values = getValues();
    int index = values.indexOf(value);
    if (index <= 0) return null;
    IntentionActionWithTextCaching prev = values.get(index - 1);

    if (myCachedIntentions.getGroup(value) != myCachedIntentions.getGroup(prev)) {
      return new ListSeparator();
    }
    return null;
  }

  @Override
  public boolean isMnemonicsNavigationEnabled() {
    return false;
  }

  @Override
  public MnemonicNavigationFilter<IntentionActionWithTextCaching> getMnemonicNavigationFilter() {
    return null;
  }

  @Override
  public boolean isSpeedSearchEnabled() {
    return true;
  }

  @Override
  public boolean isAutoSelectionEnabled() {
    return false;
  }

  @Override
  public SpeedSearchFilter<IntentionActionWithTextCaching> getSpeedSearchFilter() {
    return this;
  }

  //speed search filter
  @Override
  public String getIndexedString(final IntentionActionWithTextCaching value) {
    return getTextFor(value);
  }
}
