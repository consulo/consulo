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
package consulo.project.ui.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposable;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @author spleaner
 */
public interface StatusBarEx extends StatusBar, Disposable {
  Key<String> HOVERED_WIDGET_ID = Key.create("HOVERED_WIDGET_ID");

  void startRefreshIndication(String tooltipText);

  void stopRefreshIndication();

  void addProgress(@Nonnull ProgressIndicator indicator, @Nonnull TaskInfo info);

  List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses();

  void updateWidgets();

  boolean isProcessWindowOpen();

  void setProcessWindowOpen(boolean open);

  Dimension getSize();

  boolean isVisible();

  @Nullable
  String getInfoRequestor();

  void addWidget(@Nonnull StatusBarWidget widget, @Nonnull List<String> order, @Nonnull Disposable parentDisposable);

  void removeWidget(@Nonnull String id);
}
