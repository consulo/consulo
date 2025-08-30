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
package consulo.versionControlSystem.log.impl.internal;

import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.ui.ModalityState;
import consulo.versionControlSystem.log.VcsLogRefresher;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogFilterer;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PostponableLogRefresher implements VcsLogRefresher {
  @Nonnull
  protected final VcsLogDataImpl myLogData;
  @Nonnull
  private final Set<VirtualFile> myRootsToRefresh = new HashSet<>();
  @Nonnull
  private final Set<VcsLogWindow> myLogWindows = new HashSet<>();

  public PostponableLogRefresher(@Nonnull VcsLogDataImpl logData) {
    myLogData = logData;
    myLogData.addDataPackChangeListener(dataPack -> {
      for (VcsLogWindow window : myLogWindows) {
        dataPackArrived(window.getFilterer(), window.isVisible());
      }
    });
  }

  @Nonnull
  public Disposable addLogWindow(@Nonnull VcsLogWindow window) {
    myLogWindows.add(window);
    filtererActivated(window.getFilterer(), true);
    return () -> myLogWindows.remove(window);
  }

  @Nonnull
  public Disposable addLogWindow(@Nonnull VcsLogFilterer filterer) {
    return addLogWindow(new VcsLogWindow(filterer));
  }

  public static boolean keepUpToDate() {
    return Registry.is("vcs.log.keep.up.to.date",  true) && !PowerSaveMode.isEnabled();
  }

  protected boolean canRefreshNow() {
    return keepUpToDate() || isLogVisible();
  }

  public boolean isLogVisible() {
    for (VcsLogWindow window : myLogWindows) {
      if (window.isVisible()) return true;
    }
    return false;
  }

  public void filtererActivated(@Nonnull VcsLogFilterer filterer, boolean firstTime) {
    if (!myRootsToRefresh.isEmpty()) {
      refreshPostponedRoots();
    }
    else {
      if (firstTime) {
        filterer.onRefresh();
      }
      filterer.setValid(true);
    }
  }

  private static void dataPackArrived(@Nonnull VcsLogFilterer filterer, boolean visible) {
    if (!visible) {
      filterer.setValid(false);
    }
    filterer.onRefresh();
  }

  @Override
  public void refresh(@Nonnull final VirtualFile root) {
    Application.get().invokeLater(
      () -> {
        if (canRefreshNow()) {
          myLogData.refresh(Collections.singleton(root));
        }
        else {
          myRootsToRefresh.add(root);
        }
      },
      ModalityState.any()
    );
  }

  protected void refreshPostponedRoots() {
    Set<VirtualFile> toRefresh = new HashSet<>(myRootsToRefresh);
    myRootsToRefresh.removeAll(toRefresh); // clear the set, but keep roots which could possibly arrive after collecting them in the var.
    myLogData.refresh(toRefresh);
  }

  @Nonnull
  public Set<VcsLogWindow> getLogWindows() {
    return myLogWindows;
  }

  public static class VcsLogWindow {
    @Nonnull
    private final VcsLogFilterer myFilterer;

    public VcsLogWindow(@Nonnull VcsLogFilterer filterer) {
      myFilterer = filterer;
    }

    @Nonnull
    public VcsLogFilterer getFilterer() {
      return myFilterer;
    }

    public boolean isVisible() {
      return true;
    }
  }
}
