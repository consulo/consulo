/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.ui.impl;

import consulo.ui.ex.awt.DialogWrapper;
import consulo.application.ui.wm.IdeFocusManager;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("ConstantConditions")
public class HeadlessDialog implements AbstractDialog {
  @Nonnull
  private final DialogWrapper myWrapper;

  public HeadlessDialog(@Nonnull DialogWrapper wrapper) {
    myWrapper = wrapper;
  }

  @Override
  public void setUndecorated(boolean undecorated) {
  }

  @Override
  public void addMouseListener(MouseListener listener) {
  }

  @Override
  public void addMouseMotionListener(MouseMotionListener listener) {
  }

  @Override
  public void addKeyListener(KeyListener listener) {
  }

  @Override
  public void setModal(boolean b) {
  }

  @Override
  public void toFront() {
  }

  @Override
  public void setContentPane(Container content) {
  }

  @Override
  public void centerInParent() {
  }

  @Override
  public void toBack() {
  }

  @Override
  public JRootPane getRootPane() {
    return null;
  }

  @Override
  public void remove(Component root) {
  }

  @Override
  public Container getContentPane() {
    return null;
  }

  @Override
  public void validate() {
  }

  @Override
  public void repaint() {
  }

  @Override
  public Window getOwner() {
    return null;
  }

  @Override
  public JDialog getWindow() {
    return null;
  }

  @Override
  public Dimension getSize() {
    return null;
  }

  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public void pack() {
  }

  @Override
  public Dimension getPreferredSize() {
    return null;
  }

  @Override
  public boolean isVisible() {
    return false;
  }

  @Override
  public boolean isShowing() {
    return false;
  }

  @Override
  public void setSize(int width, int height) {
  }

  @Override
  public void setTitle(String title) {
  }

  @Override
  public boolean isResizable() {
    return false;
  }

  @Override
  public void setResizable(boolean resizable) {
  }

  @Nonnull
  @Override
  public Point getLocation() {
    return new Point(0,0);
  }

  @Override
  public void setLocation(@Nonnull Point p) {
  }

  @Override
  public void setLocation(int x, int y) {
  }

  @Override
  public boolean isModal() {
    return false;
  }

  @Override
  public void setModalityType(Dialog.ModalityType modalityType) {
  }

  @Override
  public Dialog.ModalityType getModalityType() {
    return null;
  }

  @Override
  public void show() {
    myWrapper.close(DialogWrapper.OK_EXIT_CODE);
  }

  @Override
  public IdeFocusManager getFocusManager() {
    return null;
  }

  @Override
  public void dispose() {
  }
}
