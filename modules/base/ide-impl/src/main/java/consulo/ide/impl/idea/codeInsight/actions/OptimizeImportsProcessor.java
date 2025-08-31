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

package consulo.ide.impl.idea.codeInsight.actions;

import consulo.language.codeStyle.internal.CoreCodeStyleUtil;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.SmartList;
import consulo.util.lang.EmptyRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;

import static consulo.ide.impl.idea.codeInsight.actions.OptimizeImportsProcessor.NotificationInfo.NOTHING_CHANGED_NOTIFICATION;
import static consulo.ide.impl.idea.codeInsight.actions.OptimizeImportsProcessor.NotificationInfo.SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION;

public class OptimizeImportsProcessor extends AbstractLayoutCodeProcessor {
  public static final String COMMAND_NAME = CodeInsightBundle.message("process.optimize.imports");
  private final List<NotificationInfo> myOptimizerNotifications = new SmartList<>();

  public OptimizeImportsProcessor(@Nonnull Project project) {
    super(project, COMMAND_NAME, CodeInsightLocalize.progressTextOptimizingImports().get(), false);
  }

  public OptimizeImportsProcessor(@Nonnull Project project, Module module) {
    super(project, module, COMMAND_NAME, CodeInsightLocalize.progressTextOptimizingImports().get(), false);
  }

  public OptimizeImportsProcessor(@Nonnull Project project, PsiDirectory directory, boolean includeSubdirs) {
    super(project, directory, includeSubdirs, CodeInsightLocalize.progressTextOptimizingImports().get(), COMMAND_NAME, false);
  }

  public OptimizeImportsProcessor(@Nonnull Project project, PsiDirectory directory, boolean includeSubdirs, boolean processOnlyVcsChangedFiles) {
    super(project, directory, includeSubdirs,
      CodeInsightLocalize.progressTextOptimizingImports().get(), COMMAND_NAME, processOnlyVcsChangedFiles);
  }

  public OptimizeImportsProcessor(@Nonnull Project project, PsiFile file) {
    super(project, file, CodeInsightLocalize.progressTextOptimizingImports().get(), COMMAND_NAME, false);
  }

  public OptimizeImportsProcessor(@Nonnull Project project, PsiFile[] files, Runnable postRunnable) {
    this(project, files, COMMAND_NAME, postRunnable);
  }

  public OptimizeImportsProcessor(@Nonnull Project project, PsiFile[] files, String commandName, Runnable postRunnable) {
    super(project, files, CodeInsightLocalize.progressTextOptimizingImports().get(), commandName, postRunnable, false);
  }

  public OptimizeImportsProcessor(@Nonnull AbstractLayoutCodeProcessor processor) {
    super(processor, COMMAND_NAME, CodeInsightLocalize.progressTextOptimizingImports().get());
  }

  @Override
  @Nonnull
  protected FutureTask<Boolean> prepareTask(@Nonnull PsiFile file, boolean processChangedTextOnly) {
    if (DumbService.isDumb(file.getProject())) {
      return new FutureTask<>(EmptyRunnable.INSTANCE, true);
    }

    Set<ImportOptimizer> optimizers = ImportOptimizer.forFile(file);
    List<Runnable> runnables = new ArrayList<>();
    List<PsiFile> files = file.getViewProvider().getAllFiles();
    for (ImportOptimizer optimizer : optimizers) {
      for (PsiFile psiFile : files) {
        if (optimizer.supports(psiFile)) {
          runnables.add(optimizer.processFile(psiFile));
        }
      }
    }

    Runnable runnable = !runnables.isEmpty() ? () -> {
      CoreCodeStyleUtil.setSequentialProcessingAllowed(false);
      try {
        for (Runnable runnable1 : runnables) {
          runnable1.run();
          retrieveAndStoreNotificationInfo(runnable1);
        }
        putNotificationInfoIntoCollector();
      }
      finally {
        CoreCodeStyleUtil.setSequentialProcessingAllowed(true);
      }
    } : EmptyRunnable.getInstance();

    return new FutureTask<>(runnable, true);
  }

  private void retrieveAndStoreNotificationInfo(@Nonnull Runnable runnable) {
    if (runnable instanceof ImportOptimizer.CollectingInfoRunnable collectingInfoRunnable) {
      String optimizerMessage = collectingInfoRunnable.getUserNotificationInfo();
      myOptimizerNotifications.add(optimizerMessage != null ? new NotificationInfo(optimizerMessage) : NOTHING_CHANGED_NOTIFICATION);
    }
    else if (runnable == EmptyRunnable.getInstance()) {
      myOptimizerNotifications.add(NOTHING_CHANGED_NOTIFICATION);
    }
    else {
      myOptimizerNotifications.add(SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION);
    }
  }

  private void putNotificationInfoIntoCollector() {
    LayoutCodeInfoCollector collector = getInfoCollector();
    if (collector == null) {
      return;
    }

    boolean atLeastOneOptimizerChangedSomething = false;
    for (NotificationInfo info : myOptimizerNotifications) {
      atLeastOneOptimizerChangedSomething |= info.isSomethingChanged();
      if (info.getMessage() != null) {
        collector.setOptimizeImportsNotification(info.getMessage());
        return;
      }
    }

    collector.setOptimizeImportsNotification(atLeastOneOptimizerChangedSomething ? "imports optimized" : null);
  }

  static class NotificationInfo {
    public static final NotificationInfo NOTHING_CHANGED_NOTIFICATION = new NotificationInfo(false, null);
    public static final NotificationInfo SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION = new NotificationInfo(true, null);

    private final boolean mySomethingChanged;
    private final String myMessage;

    NotificationInfo(@Nonnull String message) {
      this(true, message);
    }

    public boolean isSomethingChanged() {
      return mySomethingChanged;
    }

    public String getMessage() {
      return myMessage;
    }

    private NotificationInfo(boolean isSomethingChanged, @Nullable String message) {
      mySomethingChanged = isSomethingChanged;
      myMessage = message;
    }
  }
}
