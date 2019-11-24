/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.desktop.internal.layout;

import com.intellij.ui.IdeBorderFactory;
import consulo.ui.Component;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * @author VISTALL
 * @since 15-Jun-16
 */
public class DesktopLabeledLayoutImpl extends DesktopLayoutBase implements LabeledLayout {
  public DesktopLabeledLayoutImpl(String text) {
    super(new BorderLayout());
    myComponent.setBorder(IdeBorderFactory.createTitledBorder(text));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public LabeledLayout set(@Nonnull Component component) {
    add(component, BorderLayout.CENTER);
    return this;
  }
}
