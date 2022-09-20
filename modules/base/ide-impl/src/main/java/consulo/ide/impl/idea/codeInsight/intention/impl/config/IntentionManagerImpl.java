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

package consulo.ide.impl.idea.codeInsight.intention.impl.config;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.codeInsight.daemon.impl.CleanupOnScopeIntention;
import consulo.ide.impl.idea.codeInsight.daemon.impl.EditCleanupProfileIntentionAction;
import consulo.language.editor.impl.internal.inspection.scheme.GlobalInspectionToolWrapper;
import consulo.language.editor.inspection.GlobalSimpleInspectionTool;
import consulo.ide.impl.idea.codeInspection.actions.CleanupAllIntention;
import consulo.ide.impl.idea.codeInspection.actions.CleanupInspectionIntention;
import consulo.ide.impl.idea.codeInspection.actions.RunInspectionIntention;
import consulo.ide.impl.idea.codeInspection.ex.*;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.language.editor.inspection.GlobalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.IntentionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.Lists;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dsl
 */
@Singleton
@ServiceImpl
public class IntentionManagerImpl extends IntentionManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(IntentionManagerImpl.class);

  private final List<IntentionAction> myActions = Lists.newLockFreeCopyOnWriteList();

  private final Provider<IntentionManagerSettings> mySettingsProvider;

  @Inject
  public IntentionManagerImpl(@Nonnull Application application, @Nonnull Provider<IntentionManagerSettings> settingsProvider) {
    mySettingsProvider = settingsProvider;

    addAction(new EditInspectionToolsSettingsInSuppressedPlaceIntention());

    application.getExtensionPoint(IntentionAction.class).forEachExtensionSafe(this::addAction);
  }

  @Nonnull
  public static String getDescriptionDirectoryName(final IntentionAction action) {
    return getDescriptionDirectoryName(action.getClass().getName());
  }

  private static String getDescriptionDirectoryName(final String fqn) {
    return fqn.substring(fqn.lastIndexOf('.') + 1).replaceAll("\\$", "");
  }

  @Override
  @Nonnull
  public List<IntentionAction> getStandardIntentionOptions(@Nonnull final HighlightDisplayKey displayKey, @Nonnull final PsiElement context) {
    List<IntentionAction> options = new ArrayList<>();
    options.add(new EditInspectionToolsSettingsAction(displayKey));
    options.add(new RunInspectionIntention(displayKey));
    options.add(new DisableInspectionToolAction(displayKey));
    return options;
  }

  @Nullable
  @Override
  public IntentionAction createFixAllIntention(InspectionToolWrapper toolWrapper, IntentionAction action) {
    if (toolWrapper instanceof LocalInspectionToolWrapper) {
      Class aClass = action.getClass();
      if (action instanceof QuickFixWrapper) {
        aClass = ((QuickFixWrapper)action).getFix().getClass();
      }
      return new CleanupInspectionIntention(toolWrapper, aClass, action.getText());
    }
    else if (toolWrapper instanceof GlobalInspectionToolWrapper) {
      GlobalInspectionTool wrappedTool = ((GlobalInspectionToolWrapper)toolWrapper).getTool();
      if (wrappedTool instanceof GlobalSimpleInspectionTool && (action instanceof LocalQuickFix || action instanceof QuickFixWrapper)) {
        Class aClass = action.getClass();
        if (action instanceof QuickFixWrapper) {
          aClass = ((QuickFixWrapper)action).getFix().getClass();
        }
        return new CleanupInspectionIntention(toolWrapper, aClass, action.getText());
      }
    }
    else {
      throw new AssertionError("unknown tool: " + toolWrapper);
    }
    return null;
  }

  @Override
  @Nonnull
  public LocalQuickFix convertToFix(@Nonnull final IntentionAction action) {
    if (action instanceof LocalQuickFix) {
      return (LocalQuickFix)action;
    }
    return new LocalQuickFix() {
      @Override
      @Nonnull
      public String getName() {
        return action.getText();
      }

      @Override
      @Nonnull
      public String getFamilyName() {
        return action.getFamilyName();
      }

      @Override
      public void applyFix(@Nonnull final Project project, @Nonnull final ProblemDescriptor descriptor) {
        final PsiFile psiFile = descriptor.getPsiElement().getContainingFile();
        try {
          action.invoke(project, new LazyEditor(psiFile), psiFile);
        }
        catch (IncorrectOperationException e) {
          LOG.error(e);
        }
      }
    };
  }

  @Override
  public void addAction(@Nonnull IntentionAction action) {
    myActions.add(action);
  }

  @Override
  @Nonnull
  public IntentionAction[] getIntentionActions() {
    return ArrayUtil.stripTrailingNulls(myActions.toArray(new IntentionAction[myActions.size()]));
  }

  @Nonnull
  @Override
  public IntentionAction[] getAvailableIntentionActions() {
    IntentionManagerSettings settings = getSettings();

    List<IntentionAction> list = new ArrayList<>(myActions.size());
    for (IntentionAction action : myActions) {
      if (settings.isEnabled(action)) {
        list.add(action);
      }
    }
    return list.toArray(new IntentionAction[list.size()]);
  }

  @Nonnull
  @Override
  public IntentionAction createCleanupAllIntention() {
    return CleanupAllIntention.INSTANCE;
  }

  @Nonnull
  @Override
  public List<IntentionAction> getCleanupIntentionOptions() {
    ArrayList<IntentionAction> options = new ArrayList<>();
    options.add(EditCleanupProfileIntentionAction.INSTANCE);
    options.add(CleanupOnScopeIntention.INSTANCE);
    return options;
  }

  @Override
  public void dispose() {

  }

  @Nonnull
  private IntentionManagerSettings getSettings() {
    return mySettingsProvider.get();
  }
}
