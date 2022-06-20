/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.reporting;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.ApplicationInfo;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ApplicationManager;
import consulo.application.util.FreezeLogger;
import consulo.application.util.concurrent.ThreadDumper;
import consulo.application.util.registry.Registry;
import consulo.component.ComponentManager;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

@Singleton
@ServiceImpl
public class FreezeLoggerImpl extends FreezeLogger {

  private static final Logger LOG = Logger.getInstance(FreezeLoggerImpl.class);
  private static final Alarm ALARM = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, ApplicationManager.getApplication());
  private static final int MAX_ALLOWED_TIME = 500;

  @Override
  public void runUnderPerformanceMonitor(@Nullable ComponentManager project, @Nonnull Runnable action) {
    if (!shouldReport() || isUnderDebug() || ApplicationManager.getApplication().isUnitTestMode()) {
      action.run();
      return;
    }

    final IdeaModalityState initial = IdeaModalityState.current();
    ALARM.cancelAllRequests();
    ALARM.addRequest(() -> dumpThreads(project, initial), MAX_ALLOWED_TIME);

    try {
      action.run();
    }
    finally {
      ALARM.cancelAllRequests();
    }
  }

  private static boolean shouldReport() {
    return Registry.is("typing.freeze.report.dumps");
  }

  private static void dumpThreads(@Nullable ComponentManager project, @Nonnull IdeaModalityState initialState) {
    final ThreadInfo[] infos = ThreadDumper.getThreadInfos();
    final String edtTrace = ThreadDumper.dumpEdtStackTrace(infos);
    if (edtTrace.contains("java.lang.ClassLoader.loadClass")) {
      return;
    }

    final boolean isInDumbMode = project != null && !project.isDisposed() && DumbService.isDumb((Project)project);

    ApplicationManager.getApplication().invokeLater(() -> {
      if (!initialState.equals(IdeaModalityState.current())) return;
      sendDumpsInBackground(infos, isInDumbMode);
    }, IdeaModalityState.any());
  }

  private static void sendDumpsInBackground(ThreadInfo[] infos, boolean isInDumbMode) {
    //FIXME [VISTALL] we need this?
    /*ApplicationManager.getApplication().executeOnPooledThread(() -> {
      ThreadDumpInfo info = new ThreadDumpInfo(infos, isInDumbMode);
      String report = ReporterKt.createReportLine("typing-freeze-dumps", info);
      if (!StatsSender.INSTANCE.send(report, true)) {
        LOG.debug("Error while reporting thread dump");
      }
    }); */
  }

  private static boolean isUnderDebug() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp");
  }

}

class ThreadDumpInfo {
  public final ThreadInfo[] threadInfos;
  public final String version;
  public final String product;
  public final String buildNumber;
  public final boolean isInDumbMode;

  public ThreadDumpInfo(ThreadInfo[] threadInfos, boolean isInDumbMode) {
    this.threadInfos = threadInfos;
    this.product = ApplicationInfo.getInstance().getVersionName();
    this.version = ApplicationInfo.getInstance().getFullVersion();
    this.buildNumber = ApplicationInfo.getInstance().getBuild().toString();
    this.isInDumbMode = isInDumbMode;
  }
}