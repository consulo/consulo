/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author: Eugene Zhuravlev
 * Date: Jan 22, 2003
 * Time: 2:25:31 PM
 */
package consulo.ide.impl.idea.compiler.progress;

import consulo.compiler.ExitStatus;
import consulo.application.Application;
import consulo.compiler.CompilerManager;
import consulo.compiler.CompilerMessage;
import consulo.compiler.CompilerMessageCategory;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.impl.internal.progress.AbstractProgressIndicatorExBase;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.AppIconScheme;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.navigation.Navigatable;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.awt.UIUtil;
import consulo.ide.impl.compiler.BuildViewServiceFactory;
import consulo.ide.impl.compiler.CompilerManagerImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CompilerTask extends Task.Backgroundable {
  private static final String APP_ICON_ID = "compiler";

  private final boolean myWaitForPreviousSession;
  private int myErrorCount = 0;
  private int myWarningCount = 0;

  private volatile ProgressIndicator myIndicator = new EmptyProgressIndicator();
  private Runnable myCompileWork;
  private final boolean myCompilationStartedAutomatically;
  private final UUID mySessionId;

  private final BuildViewService myBuildViewService;
  private long myEndCompilationStamp;
  private ExitStatus myExitStatus;

  public CompilerTask(@Nonnull Project project, String contentName, boolean waitForPreviousSession, boolean compilationStartedAutomatically) {
    super(project, contentName);
    myWaitForPreviousSession = waitForPreviousSession;
    myCompilationStartedAutomatically = compilationStartedAutomatically;
    mySessionId = UUID.randomUUID();
    myBuildViewService = Application.get().getInstance(BuildViewServiceFactory.class).createBuildViewService(project, mySessionId, contentName);
  }

  @Nonnull
  public ProgressIndicator getIndicator() {
    return myIndicator;
  }

  public void setEndCompilationStamp(ExitStatus exitStatus, long endCompilationStamp) {
    myExitStatus = exitStatus;
    myEndCompilationStamp = endCompilationStamp;
  }

  @Override
  @Nullable
  public NotificationInfo getNotificationInfo() {
    return new NotificationInfo(myErrorCount > 0 ? "Compiler (errors)" : "Compiler (success)", "Compilation Finished", myErrorCount + " Errors, " + myWarningCount + " Warnings", true);
  }

  @Override
  public void run(@Nonnull final ProgressIndicator indicator) {
    myIndicator = indicator;

    long startCompilationStamp = System.currentTimeMillis();


    indicator.setIndeterminate(false);

    myBuildViewService.onStart(mySessionId, startCompilationStamp, null, indicator);

    final Semaphore semaphore = ((CompilerManagerImpl)CompilerManager.getInstance((Project)myProject)).getCompilationSemaphore();
    boolean acquired = false;
    try {

      try {
        while (!acquired) {
          acquired = semaphore.tryAcquire(300, TimeUnit.MILLISECONDS);
          if (!acquired && !myWaitForPreviousSession) {
            return;
          }
          if (indicator.isCanceled()) {
            // give up obtaining the semaphore,
            // let compile work begin in order to stop gracefuly on cancel event
            break;
          }
        }
      }
      catch (InterruptedException ignored) {
      }

      if (!isHeadless()) {
        addIndicatorDelegate();
      }
      myCompileWork.run();
    }
    catch (ProcessCanceledException ignored) {
    }
    finally {
      try {
        indicator.stop();

        myBuildViewService.onEnd(mySessionId, myExitStatus, myEndCompilationStamp);
      }
      finally {
        if (acquired) {
          semaphore.release();
        }
      }
    }
  }

  private void addIndicatorDelegate() {
    ProgressIndicator indicator = myIndicator;
    if (!(indicator instanceof ProgressIndicatorEx)) return;
    ((ProgressIndicatorEx)indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {

      @Override
      public void cancel() {
        super.cancel();
        stopAppIconProgress();
      }

      @Override
      public void stop() {
        super.stop();
        stopAppIconProgress();
      }

      private void stopAppIconProgress() {
        UIUtil.invokeLaterIfNeeded(() -> {
          AppIcon appIcon = AppIcon.getInstance();
          if (appIcon.hideProgress((Project)myProject, APP_ICON_ID)) {
            if (myErrorCount > 0) {
              appIcon.setErrorBadge((Project)myProject, String.valueOf(myErrorCount));
              appIcon.requestAttention((Project)myProject, true);
            }
            else if (!myCompilationStartedAutomatically) {
              appIcon.setOkBadge((Project)myProject, true);
              appIcon.requestAttention((Project)myProject, false);
            }
          }
        });
      }

      @Override
      public void setFraction(final double fraction) {
        super.setFraction(fraction);
        UIUtil.invokeLaterIfNeeded(() -> AppIcon.getInstance().setProgress((Project)myProject, APP_ICON_ID, AppIconScheme.Progress.BUILD, fraction, true));
      }
    });
  }

  public void cancel() {
    if (!myIndicator.isCanceled()) {
      myIndicator.cancel();
    }
  }

  public void addMessage(final CompilerMessage message) {
    final CompilerMessageCategory messageCategory = message.getCategory();
    if (CompilerMessageCategory.WARNING.equals(messageCategory)) {
      myWarningCount += 1;
    }
    else if (CompilerMessageCategory.ERROR.equals(messageCategory)) {
      myErrorCount += 1;
      informWolf(message);
    }

    myBuildViewService.addMessage(mySessionId, message);
  }

  private void informWolf(final CompilerMessage message) {
    WolfTheProblemSolver wolf = WolfTheProblemSolver.getInstance((Project)myProject);
    VirtualFile file = getVirtualFile(message);
    wolf.queue(file);
  }

  public void start(Runnable compileWork) {
    myCompileWork = compileWork;
    queue();
  }

  private static VirtualFile getVirtualFile(final CompilerMessage message) {
    VirtualFile virtualFile = message.getVirtualFile();
    if (virtualFile == null) {
      Navigatable navigatable = message.getNavigatable();
      if (navigatable instanceof OpenFileDescriptorImpl) {
        virtualFile = ((OpenFileDescriptorImpl)navigatable).getFile();
      }
    }
    return virtualFile;
  }

  public static TextRange getTextRange(final CompilerMessage message) {
    Navigatable navigatable = message.getNavigatable();
    if (navigatable instanceof OpenFileDescriptorImpl) {
      int offset = ((OpenFileDescriptorImpl)navigatable).getOffset();
      return new TextRange(offset, offset);
    }
    return TextRange.EMPTY_RANGE;
  }
}