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
package consulo.awt.hacking;

import consulo.awt.hacking.util.FieldAccessor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * @author VISTALL
 * @since 2020-10-19
 */
public class BasicComboBoxUIHacking {
  private static FieldAccessor<BasicComboBoxUI, ComboPopup> popupField = new FieldAccessor<>(BasicComboBoxUI.class, "popup");
  private static FieldAccessor<BasicComboBoxUI, MouseListener> popupMouseListener = new FieldAccessor<>(BasicComboBoxUI.class, "popupMouseListener");
  private static FieldAccessor<BasicComboBoxUI, MouseMotionListener> popupMouseMotionListener = new FieldAccessor<>(BasicComboBoxUI.class, "popupMouseMotionListener");
  private static FieldAccessor<BasicComboBoxUI, KeyListener> popupKeyListener = new FieldAccessor<>(BasicComboBoxUI.class, "popupKeyListener");
  private static FieldAccessor<BasicComboBoxUI, JButton> arrowButton = new FieldAccessor<>(BasicComboBoxUI.class, "arrowButton");

  public static ComboPopup getPopup(BasicComboBoxUI ui) {
    return popupField.isAvailable() ? popupField.get(ui) : null;
  }

  public static void setPopup(BasicComboBoxUI ui, ComboPopup popup) {
    if (popupField.isAvailable()) {
      popupField.set(ui, popup);
    }
  }

  public static MouseMotionListener getMouseMotionListener(BasicComboBoxUI ui) {
    return popupMouseMotionListener.isAvailable() ? popupMouseMotionListener.get(ui) : null;
  }

  public static void setMouseMotionListener(BasicComboBoxUI ui, MouseMotionListener listener) {
    if(popupMouseMotionListener.isAvailable()) {
      popupMouseMotionListener.set(ui, listener);
    }
  }

  public static KeyListener getKeyListener(BasicComboBoxUI ui) {
    return popupKeyListener.isAvailable() ? popupKeyListener.get(ui) : null;
  }

  public static void setKeyListener(BasicComboBoxUI ui, KeyListener keyListener) {
    if (popupKeyListener.isAvailable()) {
      popupKeyListener.set(ui, keyListener);
    }
  }

  public static JButton getArrowButton(BasicComboBoxUI ui) {
    return arrowButton.isAvailable() ? arrowButton.get(ui) : null;
  }

  public static MouseListener getMouseListener(BasicComboBoxUI ui) {
    return popupMouseListener.isAvailable() ? popupMouseListener.get(ui) : null;
  }

  public static void setMouseListener(BasicComboBoxUI ui, MouseListener listener) {
    if (popupMouseListener.isAvailable()) {
      popupMouseListener.set(ui, listener);
    }
  }
}
