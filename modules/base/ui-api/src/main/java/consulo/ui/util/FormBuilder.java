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
package consulo.ui.util;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.StaticPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 06-Nov-17
 */
public class FormBuilder {
  @Nonnull
  public static FormBuilder create() {
    return new FormBuilder();
  }

  private int myLineCount;
  private TableLayout myLayout = TableLayout.create(StaticPosition.TOP);
  private List<Component> myBottomComponents = new ArrayList<>();

  private FormBuilder() {
  }

  @Nonnull
  @RequiredUIAccess
  @Deprecated
  public FormBuilder addLabeled(@Nonnull final String labelText, @Nonnull Component component) {
    if (!myBottomComponents.isEmpty()) {
      throw new IllegalArgumentException("Can't add labeled, after adding bottom components");
    }

    String newLabelText = labelText;
    if (!StringUtil.endsWithChar(newLabelText, ':')) {
      newLabelText += ": ";
    }

    myLayout.add(Label.create(newLabelText), TableLayout.cell(myLineCount, 0));
    myLayout.add(component, TableLayout.cell(myLineCount, 1).fill());

    myLineCount++;
    return this;
  }

  @Nonnull
  @RequiredUIAccess
  public FormBuilder addLabeled(@Nonnull final LocalizeValue labelText, @Nonnull Component component) {
    if(!myBottomComponents.isEmpty()) {
      throw new IllegalArgumentException("Can't add labeled, after adding bottom components");
    }

    Label label = Label.create(labelText);
    label.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 5);

    myLayout.add(label, TableLayout.cell(myLineCount, 0));
    myLayout.add(component, TableLayout.cell(myLineCount, 1).fill());

    myLineCount++;
    return this;
  }

  @Nonnull
  @RequiredUIAccess
  public FormBuilder addLabeled(@Nonnull final Component label, @Nonnull Component component) {
    if (!myBottomComponents.isEmpty()) {
      throw new IllegalArgumentException("Can't add labeled, after adding bottom components");
    }

    label.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, 5);
    
    myLayout.add(label, TableLayout.cell(myLineCount, 0));
    myLayout.add(component, TableLayout.cell(myLineCount, 1).fill());

    myLineCount++;
    return this;
  }

  public void addBottom(@Nonnull Component component) {
    myBottomComponents.add(component);
  }

  @Nonnull
  @RequiredUIAccess
  public Component build() {
    if(myBottomComponents.isEmpty()) {
      return myLayout;
    }

    VerticalLayout composite = VerticalLayout.create();
    composite.add(myLayout);
    for (Component bottomComponent : myBottomComponents) {
      composite.add(bottomComponent);
    }
    return composite;
  }
}
