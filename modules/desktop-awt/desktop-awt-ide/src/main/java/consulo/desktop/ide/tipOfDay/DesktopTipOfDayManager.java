/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.ide.tipOfDay;

import com.intellij.ide.util.TipDialog;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import consulo.disposer.Disposer;
import consulo.ide.tipOfDay.TipOfDayManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2020-06-23
 */
@Singleton
public class DesktopTipOfDayManager implements TipOfDayManager {
  private AtomicBoolean myAlreadyShow = new AtomicBoolean();

  @Override
  public void scheduleShow(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    if (myAlreadyShow.compareAndSet(false, true)) {
      Future<?> future = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
        if (project.isDisposed()) {
          // revert state if project canceled
          myAlreadyShow.compareAndSet(true, false);
          return;
        }

        uiAccess.give(this::showAsync);
      }, 5, TimeUnit.SECONDS);

      Disposer.register(project, () -> {
        if (!future.isDone()) {
          // task not done - it's mean, it was canceled before - reset state
          myAlreadyShow.compareAndSet(true, false);
          future.cancel(false);
        }
      });
    }
  }

  @Override
  @RequiredUIAccess
  public void showAsync() {
    new TipDialog().showAsync();
  }
}
