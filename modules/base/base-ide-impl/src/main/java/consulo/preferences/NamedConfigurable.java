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

package consulo.preferences;

import com.intellij.openapi.options.Configurable;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.WrappedLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2020-10-26
 */
public abstract class NamedConfigurable<T> implements Configurable, MasterDetailsConfigurable {
  private static class UIPanel {
    private final TextBox myNameField;
    private final DockLayout myNameLayout;
    private final DockLayout myWholePanel;
    private final WrappedLayout myOptionsPanel;
    private final WrappedLayout myTopRightPanel;

    @RequiredUIAccess
    private UIPanel(boolean isNameEditable, @Nonnull Consumer<String> nameListener) {
      myNameLayout = DockLayout.create();
      myNameLayout.setVisible(isNameEditable);

      myNameField = TextBox.create();

      myNameLayout.left(Label.create(CommonLocalize.nameLabelText()));
      myNameLayout.center(myNameField);

      myTopRightPanel = WrappedLayout.create();
      myNameLayout.right(myTopRightPanel);

      if (isNameEditable) {
        myNameField.addValueListener(it -> {
          String value = it.getValue();
          if (value == null) {
            // bug?
            return;
          }
          nameListener.accept(value);
        });
      }

      myWholePanel = DockLayout.create();
      myWholePanel.top(myNameLayout);

      myOptionsPanel = WrappedLayout.create();
      myWholePanel.center(myOptionsPanel);

      myNameLayout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 10);
      myNameLayout.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, null, 10);
      myNameLayout.addBorder(BorderPosition.TOP, BorderStyle.EMPTY, null, 10);
      myNameLayout.addBorder(BorderPosition.BOTTOM, BorderStyle.EMPTY, null, 6);
    }
  }

  private final boolean myNameEditable;
  @Nullable
  private final Runnable myUpdateTree;

  private boolean myNameVisible = true;

  private UIPanel myUIPanel;

  private Component myOptionsComponent;

  @RequiredUIAccess
  protected NamedConfigurable() {
    this(false, null);
  }

  @RequiredUIAccess
  protected NamedConfigurable(boolean isNameEditable, @Nullable final Runnable updateTree) {
    myNameEditable = isNameEditable;
    myUpdateTree = updateTree;
  }

  public boolean isNameEditable() {
    return myNameEditable;
  }

  @RequiredUIAccess
  public void setNameFieldShown(boolean shown) {
    myNameVisible = shown;
  }

  public abstract void setDisplayName(String name);

  @Override
  public abstract T getEditableObject();

  @Override
  public String getBannerSlogan() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public final Component createUIComponent(@Nonnull Disposable parentUIDisposable) {
    if (myUIPanel != null) {
      return myUIPanel.myWholePanel;
    }

    myUIPanel = new UIPanel(myNameEditable, newName -> {
      setDisplayName(newName);

      if (myUpdateTree != null) {
        myUpdateTree.run();
      }
    });

    myUIPanel.myNameField.setVisible(myNameVisible);

    if (myOptionsComponent == null) {
      myOptionsComponent = createOptionsPanel(parentUIDisposable);
      final Component component = createTopRightComponent(myUIPanel.myNameField, parentUIDisposable);
      if (component == null) {
        myUIPanel.myTopRightPanel.setVisible(false);
      }
      else {
        myUIPanel.myTopRightPanel.set(component);
      }
    }
    if (myOptionsComponent != null) {
      myUIPanel.myOptionsPanel.set(myOptionsComponent);
    }
    else {
      Logger.getInstance(getClass()).error("Options component is null for " + getClass());
    }
    updateName();
    return myUIPanel.myWholePanel;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myUIPanel = null;
    myOptionsComponent = null;
  }

  @Nullable
  @RequiredUIAccess
  protected Component createTopRightComponent(@Nonnull TextBox textBox, @Nonnull Disposable parentUIDisposable) {
    return null;
  }

  @Override
  @RequiredUIAccess
  public void updateName() {
    if (myUIPanel == null) {
      throw new IllegalArgumentException("not initialized");
    }

    myUIPanel.myNameField.setValue(getDisplayName());
  }

  @RequiredUIAccess
  @Nonnull
  public abstract Component createOptionsPanel(@Nonnull Disposable parentUIDisposable);
}
