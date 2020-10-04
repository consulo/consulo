/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.wm.impl;

import consulo.disposer.Disposable;
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
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WebStatusBarImpl implements StatusBarEx {
  private WebIdeFrameImpl myFrame;

  public WebStatusBarImpl(WebIdeFrameImpl frame) {
    myFrame = frame;
  }

  @Override
  public void setInfo(@javax.annotation.Nullable String s) {

  }

  @Override
  public void setInfo(@Nullable String s, @javax.annotation.Nullable String requestor) {

  }

  @Override
  public String getInfo() {
    return null;
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

  @javax.annotation.Nullable
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
  public JComponent getComponent() {
    return null;
  }

  @Override
  public StatusBar findChild(Component c) {
    return null;
  }

  @Override
  public IdeFrame getFrame() {
    return myFrame;
  }

  @Override
  public void install(IdeFrame frame) {

  }

  @Nullable
  @Override
  public Project getProject() {
    return myFrame.getProject();
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
  public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody, @javax.annotation.Nullable Image icon, @Nullable HyperlinkListener listener) {
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
    return true;
  }

  @javax.annotation.Nullable
  @Override
  public String getInfoRequestor() {
    return null;
  }
}
