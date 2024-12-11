/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.project.ui.util;

import consulo.application.Application;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.Balloon;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BooleanSupplier;

/**
 * @author yole
 */
public class AppUIUtil {
  private AppUIUtil() {
  }

  public static void invokeLaterIfProjectAlive(@Nonnull final Project project, @Nonnull final Runnable runnable) {
    final Application application = Application.get();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.invokeLater(runnable, () -> !project.isOpen() || project.isDisposed());
    }
  }

  public static void invokeOnEdt(@RequiredUIAccess Runnable runnable) {
    invokeOnEdt(runnable, null);
  }

  public static void invokeOnEdt(@RequiredUIAccess Runnable runnable, @Nullable BooleanSupplier condition) {
    final Application application = Application.get();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else if (condition == null) {
      application.invokeLater(runnable);
    }
    else {
      application.invokeLater(runnable, condition);
    }
  }

  public static void hideToolWindowBalloon(@Nonnull final String id, @Nonnull final Project project) {
    invokeLaterIfProjectAlive(project, () -> {
      Balloon balloon = ToolWindowManager.getInstance(project).getToolWindowBalloon(id);
      if (balloon != null) {
        balloon.hide();
      }
    });
  }
}
