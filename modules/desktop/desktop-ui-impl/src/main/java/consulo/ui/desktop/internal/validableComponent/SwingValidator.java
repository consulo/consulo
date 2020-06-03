/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal.validableComponent;

import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.reference.SoftReference;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.ui.ValidableComponent;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

/**
 * @author VISTALL
 * @since 2019-11-04
 */
public class SwingValidator<V> {
  private List<ValidableComponent.Validator<V>> myValidators = ContainerUtil.createLockFreeCopyOnWriteList();

  private WeakReference<JBPopup> myLastPopup;

  @Nonnull
  public Disposable addValidator(ValidableComponent.Validator<V> validator) {
    myValidators.add(validator);
    return () -> myValidators.remove(validator);
  }

  public boolean validateValue(Component awtComponent, V value, boolean silence) {
    for (ValidableComponent.Validator<V> validator : myValidators) {
      ValidableComponent.ValidationInfo validationInfo = validator.validateValue(value);
      if (validationInfo != null) {
        if(!silence) {
          doShowPopup((JComponent)awtComponent, validationInfo);
        }
        return false;
      }
      else {
        JBPopup popup = SoftReference.dereference(myLastPopup);
        if(popup != null) {
          popup.cancel();
          myLastPopup = null;
        }
      }
    }
    return true;
  }

  private void doShowPopup(JComponent awtComponent, ValidableComponent.ValidationInfo validationInfo) {
    Ref<Dimension> dimensionRef = Ref.create();
    ValidationInfo info = new ValidationInfo(validationInfo.getMessage());
    ComponentPopupBuilder popupBuilder = ComponentValidator.createPopupBuilder(info, tipComponent -> {
      dimensionRef.set(tipComponent.getPreferredSize());

    }).setCancelOnMouseOutCallback(e -> e.getID() == MouseEvent.MOUSE_PRESSED && !withinComponent(info, e));

    getFocusable(awtComponent).ifPresent(fc -> {
      if (fc.hasFocus()) {
        showPopup(awtComponent, popupBuilder, dimensionRef.get());
      }
    });
  }

  private void showPopup(JComponent awtComponent, ComponentPopupBuilder popupBuilder, Dimension popupSize) {
    Insets i = awtComponent.getInsets();
    Point point = new Point(JBUIScale.scale(40), i.top - JBUIScale.scale(6) - popupSize.height);
    RelativePoint popupLocation = new RelativePoint(awtComponent, point);

    JBPopup popup = popupBuilder.createPopup();
    popup.show(popupLocation);

    myLastPopup = new WeakReference<>(popup);
  }

  public static boolean withinComponent(@Nonnull ValidationInfo info, @Nonnull MouseEvent e) {
    if (info.component != null && info.component.isShowing()) {
      Rectangle screenBounds = new Rectangle(info.component.getLocationOnScreen(), info.component.getSize());
      return screenBounds.contains(e.getLocationOnScreen());
    }
    else {
      return false;
    }
  }

  private static Optional<Component> getFocusable(Component source) {
    return (source instanceof JComboBox && !((JComboBox)source).isEditable() || source instanceof JCheckBox || source instanceof JRadioButton)
           ? Optional.of(source)
           : UIUtil.uiTraverser(source).filter(c -> c instanceof JTextComponent && c.isFocusable()).toList().stream().findFirst();
  }
}
