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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Alarm;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dsl
 */
@Singleton
public class IntentionManagerImpl extends IntentionManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(IntentionManagerImpl.class);

  private final List<IntentionAction> myActions = ContainerUtil.createLockFreeCopyOnWriteList();
  private final IntentionManagerSettings mySettings;

  private final Alarm myInitActionsAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

  @Inject
  public IntentionManagerImpl(IntentionManagerSettings intentionManagerSettings) {
    mySettings = intentionManagerSettings;

    addAction(new EditInspectionToolsSettingsInSuppressedPlaceIntention());

    for (IntentionActionBean bean : EP_INTENTION_ACTIONS.getExtensionList()) {
      registerIntentionFromBean(bean);
    }
  }

  private void registerIntentionFromBean(@Nonnull final IntentionActionBean extension) {
    final Runnable runnable = new Runnable() {
      @Override
      public void run() {
        final String descriptionDirectoryName = extension.getDescriptionDirectoryName();
        final String[] categories = extension.getCategories();
        final IntentionAction instance = createIntentionActionWrapper(extension, categories);
        if (categories == null) {
          addAction(instance);
        }
        else {
          if (descriptionDirectoryName != null) {
            addAction(instance);
            mySettings.registerIntentionMetaData(instance, categories, descriptionDirectoryName, extension.getMetadataClassLoader());
          }
          else {
            registerIntentionAndMetaData(instance, categories);
          }
        }
      }
    };
    //todo temporary hack, need smarter logic:
    // * on the first request, wait until all the initialization is finished
    // * ensure this request doesn't come on EDT
    // * while waiting, check for ProcessCanceledException
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      runnable.run();
    }
    else {
      myInitActionsAlarm.addRequest(runnable, 300);
    }
  }

  private static IntentionAction createIntentionActionWrapper(@Nonnull IntentionActionBean intentionActionBean, String[] categories) {
    return new IntentionActionWrapper(intentionActionBean, categories);
  }

  @Override
  public void registerIntentionAndMetaData(@Nonnull IntentionAction action, @Nonnull String... category) {
    registerIntentionAndMetaData(action, category, getDescriptionDirectoryName(action));
  }

  @Nonnull
  private static String getDescriptionDirectoryName(final IntentionAction action) {
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
  public void registerIntentionAndMetaData(@Nonnull IntentionAction action,
                                           @Nonnull String[] category,
                                           @Nonnull @NonNls String descriptionDirectoryName) {
    addAction(action);
    mySettings.registerIntentionMetaData(action, category, descriptionDirectoryName);
  }

  @Override
  public void registerIntentionAndMetaData(@Nonnull final IntentionAction action,
                                           @Nonnull final String[] category,
                                           @Nonnull final String description,
                                           @Nonnull final String exampleFileExtension,
                                           @Nonnull final String[] exampleTextBefore,
                                           @Nonnull final String[] exampleTextAfter) {
    addAction(action);

    IntentionActionMetaData metaData = new IntentionActionMetaData(action, category,
                                                                   new PlainTextDescriptor(description, "description.html"),
                                                                   mapToDescriptors(exampleTextBefore, "before." + exampleFileExtension),
                                                                   mapToDescriptors(exampleTextAfter, "after." + exampleFileExtension));
    mySettings.registerMetaData(metaData);
  }

  @Override
  public void unregisterIntention(@Nonnull IntentionAction intentionAction) {
    myActions.remove(intentionAction);
    mySettings.unregisterMetaData(intentionAction);
  }

  private static TextDescriptor[] mapToDescriptors(String[] texts, @NonNls String fileName) {
    TextDescriptor[] result = new TextDescriptor[texts.length];
    for (int i = 0; i < texts.length; i++) {
      result[i] = new PlainTextDescriptor(texts[i], fileName);
    }
    return result;
  }

  @Override
  @Nonnull
  public List<IntentionAction> getStandardIntentionOptions(@Nonnull final HighlightDisplayKey displayKey,
                                                           @Nonnull final PsiElement context) {
    List<IntentionAction> options = new ArrayList<IntentionAction>(9);
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
    List<IntentionAction> list = new ArrayList<IntentionAction>(myActions.size());
    for (IntentionAction action : myActions) {
      if (mySettings.isEnabled(action)) {
        list.add(action);
      }
    }
    return list.toArray(new IntentionAction[list.size()]);
  }

  public boolean hasActiveRequests() {
    return !myInitActionsAlarm.isEmpty();
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
}
