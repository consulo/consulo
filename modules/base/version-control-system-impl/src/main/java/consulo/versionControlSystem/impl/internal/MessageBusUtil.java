/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.ProcessCanceledException;
import consulo.project.Project;

import java.util.function.Consumer;

/**
 * @author Irina.Chernushina
 * @since 2012-11-08
 */
@Deprecated
public class MessageBusUtil {
  public static <T> void runOnSyncPublisher(final Project project, final Class<T> topic, final Consumer<T> listener) {
    final Application application = ApplicationManager.getApplication();
    final Runnable runnable = createPublisherRunnable(project, topic, listener);
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.runReadAction(runnable);
    }
  }

  private static <T> Runnable createPublisherRunnable(final Project project, final Class<T> topic, final Consumer<T> listener) {
    return new Runnable() {
      @Override
      public void run() {
        if (project.isDisposed()) throw new ProcessCanceledException();
        listener.accept(project.getMessageBus().syncPublisher(topic));
      }
    };
  }

  public static <T> void invokeLaterIfNeededOnSyncPublisher(final Project project, final Class<T> topic, final Consumer<T> listener) {
    final Application application = ApplicationManager.getApplication();
    final Runnable runnable = createPublisherRunnable(project, topic, listener);
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.invokeLater(runnable);
    }
  }
}
