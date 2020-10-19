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

package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.CleanupOnScopeIntention;
import com.intellij.codeInsight.daemon.impl.EditCleanupProfileIntentionAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionBean;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInspection.GlobalInspectionTool;
import com.intellij.codeInspection.GlobalSimpleInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.actions.CleanupAllIntention;
import com.intellij.codeInspection.actions.CleanupInspectionIntention;
import com.intellij.codeInspection.actions.RunInspectionIntention;
import com.intellij.codeInspection.ex.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dsl
 */
@Singleton
public class IntentionManagerImpl extends IntentionManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(IntentionManagerImpl.class);

  private final List<IntentionAction> myActions = ContainerUtil.createLockFreeCopyOnWriteList();

  private final Provider<IntentionManagerSettings> mySettingsProvider;

  private ThreadLocal<Boolean> myInsideEpInitialization = ThreadLocal.withInitial(() -> Boolean.FALSE);

  @Inject
  public IntentionManagerImpl(@Nonnull Provider<IntentionManagerSettings> settingsProvider) {
    mySettingsProvider = settingsProvider;

    addAction(new EditInspectionToolsSettingsInSuppressedPlaceIntention());

    try {
      myInsideEpInitialization.set(Boolean.TRUE);

      for (IntentionActionBean bean : EP_INTENTION_ACTIONS.getExtensionList()) {
        addAction(new IntentionActionWrapper(bean));
      }
    }
    finally {
      myInsideEpInitialization.set(Boolean.FALSE);
    }
  }

  @Override
  public void registerIntentionAndMetaData(@Nonnull IntentionAction action, @Nonnull String... category) {
    registerIntentionAndMetaData(action, category, getDescriptionDirectoryName(action));
  }

  @Nonnull
  public static String getDescriptionDirectoryName(final IntentionAction action) {
    if (action instanceof IntentionActionWrapper) {
      final IntentionActionWrapper wrapper = (IntentionActionWrapper)action;
      return getDescriptionDirectoryName(wrapper.getImplementationClassName());
    }
    else {
      return getDescriptionDirectoryName(action.getClass().getName());
    }
  }

  private static String getDescriptionDirectoryName(final String fqn) {
    return fqn.substring(fqn.lastIndexOf('.') + 1).replaceAll("\\$", "");
  }

  @Override
  public void registerIntentionAndMetaData(@Nonnull IntentionAction action, @Nonnull String[] category, @Nonnull String descriptionDirectoryName) {
    addAction(action);

    getSettings().registerIntentionMetaData(action, category, descriptionDirectoryName);
  }

  @Override
  public void registerIntentionAndMetaData(@Nonnull final IntentionAction action,
                                           @Nonnull final String[] category,
                                           @Nonnull final String description,
                                           @Nonnull final String exampleFileExtension,
                                           @Nonnull final String[] exampleTextBefore,
                                           @Nonnull final String[] exampleTextAfter) {
    addAction(action);

    IntentionActionMetaData metaData =
            new IntentionActionMetaData(action, category, new PlainTextDescriptor(description, "description.html"), mapToDescriptors(exampleTextBefore, "before." + exampleFileExtension),
                                        mapToDescriptors(exampleTextAfter, "after." + exampleFileExtension));
    getSettings().registerMetaData(metaData);
  }

  @Override
  public void unregisterIntention(@Nonnull IntentionAction intentionAction) {
    IntentionManagerSettings settings = getSettings();

    myActions.remove(intentionAction);
    settings.unregisterMetaData(intentionAction);
  }

  private static TextDescriptor[] mapToDescriptors(String[] texts, String fileName) {
    TextDescriptor[] result = new TextDescriptor[texts.length];
    for (int i = 0; i < texts.length; i++) {
      result[i] = new PlainTextDescriptor(texts[i], fileName);
    }
    return result;
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
    if(myInsideEpInitialization.get()) {
      throw new IllegalArgumentException("Can't call settings here");
    }
    return mySettingsProvider.get();
  }
}
