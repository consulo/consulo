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
package com.intellij.openapi.wm.ex;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.BalloonHandler;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.StatusBar;
import consulo.disposer.Disposable;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;

/**
 * @author spleaner
 */
public interface StatusBarEx extends StatusBar, Disposable {
  Key<String> HOVERED_WIDGET_ID = Key.create("HOVERED_WIDGET_ID");

  void startRefreshIndication(String tooltipText);

  void stopRefreshIndication();

  BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody);

  BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener);

  void addProgress(@Nonnull ProgressIndicatorEx indicator, @Nonnull TaskInfo info);

  List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses();

  void updateWidgets();

  boolean isProcessWindowOpen();

  void setProcessWindowOpen(boolean open);

  Dimension getSize();

  boolean isVisible();

  @Nullable
  String getInfoRequestor();
}
