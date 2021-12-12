/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.wm.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.BalloonHandler;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.openapi.wm.ex.StatusBarEx;
import consulo.disposer.Disposable;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class DesktopSwtStatusBarImpl implements StatusBarEx {
  private HorizontalLayout myLayout;

  public DesktopSwtStatusBarImpl(Application application, Object o) {
    myLayout = HorizontalLayout.create();
  }

  @Override
  public void addWidget(@Nonnull StatusBarWidget widget) {

  }

  @Override
  public void addWidget(@Nonnull StatusBarWidget widget, @Nonnull String anchor) {

  }

  @Override
  public void addWidget(@Nonnull StatusBarWidget widget, @Nonnull Disposable parentDisposable) {

  }

  @Override
  public void addWidget(@Nonnull StatusBarWidget widget, @Nonnull String anchor, @Nonnull Disposable parentDisposable) {

  }

  @Override
  public void removeWidget(@Nonnull String id) {

  }

  @Override
  public void updateWidget(@Nonnull String id) {

  }

  @Nullable
  @Override
  public StatusBarWidget getWidget(String id) {
    return null;
  }

  @Override
  public void fireNotificationPopup(@Nonnull JComponent content, Color backgroundColor) {

  }

  @Override
  public StatusBar createChild() {
    return null;
  }

  @Override
  public StatusBar findChild(Component c) {
    return null;
  }

  @Override
  public IdeFrame getFrame() {
    return null;
  }

  @Override
  public void install(IdeFrame frame) {

  }

  @Nullable
  @Override
  public Project getProject() {
    return null;
  }

  @Override
  public void setInfo(@Nullable String s) {

  }

  @Override
  public void setInfo(@Nullable String s, @Nullable String requestor) {

  }

  @Override
  public String getInfo() {
    return null;
  }

  @Override
  public void dispose() {

  }

  @Override
  public void startRefreshIndication(String tooltipText) {

  }

  @Override
  public void stopRefreshIndication() {

  }

  @Override
  public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody) {
    return null;
  }

  @Override
  public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener) {
    return null;
  }

  @Override
  public void addProgress(@Nonnull ProgressIndicatorEx indicator, @Nonnull TaskInfo info) {

  }

  @Override
  public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
    return null;
  }

  @Override
  public void updateWidgets() {

  }

  @Override
  public boolean isProcessWindowOpen() {
    return false;
  }

  @Override
  public void setProcessWindowOpen(boolean open) {

  }

  @Override
  public Dimension getSize() {
    return null;
  }

  @Override
  public boolean isVisible() {
    return false;
  }

  @Nullable
  @Override
  public String getInfoRequestor() {
    return null;
  }

  @Nonnull
  @Override
  public consulo.ui.Component getUIComponent() {
    return myLayout;
  }
}
