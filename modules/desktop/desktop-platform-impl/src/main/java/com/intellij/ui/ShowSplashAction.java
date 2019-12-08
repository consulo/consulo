/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.start.splash.DesktopSplash;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

import java.awt.event.*;
import java.util.concurrent.Future;

/**
 * @author Konstantin Bulenkov
 */
public class ShowSplashAction extends AnAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final DesktopSplash splash = new DesktopSplash(true);

    Future<?> task = ApplicationManager.getApplication().executeOnPooledThread((Runnable)() -> {
      for (int i = 0; i <= 100; i++) {
        final float progress = i / 100f;
        UIUtil.invokeLaterIfNeeded(() -> splash.showProgress("", progress));

        try {
          Thread.sleep(10L);
        }
        catch (InterruptedException e1) {
          break;
        }
      }
    });

    final SplashListener listener = new SplashListener(splash, task);
    splash.addFocusListener(listener);
    splash.addKeyListener(listener);
    splash.addMouseListener(listener);

    splash.show();
  }

  private static class SplashListener implements KeyListener, MouseListener, FocusListener {
    private final DesktopSplash mySplash;
    private Future<?> myTask;

    private SplashListener(DesktopSplash splash, Future<?> task) {
      mySplash = splash;
      myTask = task;
    }

    private void close() {
      if (mySplash.isVisible()) {
        mySplash.stopAnimation();
        myTask.cancel(false);
        mySplash.setVisible(false);
      }
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
      close();
    }

    @Override
    public void keyTyped(KeyEvent e) {
      close();
    }

    @Override
    public void keyPressed(KeyEvent e) {
      close();
    }

    @Override
    public void keyReleased(KeyEvent e) {
      close();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      close();
    }

    @Override
    public void mousePressed(MouseEvent e) {
      close();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      close();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
  }
}
