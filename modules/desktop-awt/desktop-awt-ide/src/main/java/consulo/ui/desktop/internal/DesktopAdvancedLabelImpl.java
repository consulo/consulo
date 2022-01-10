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
package consulo.ui.desktop.internal;

import com.intellij.ui.SimpleColoredComponent;
import consulo.ui.AdvancedLabel;
import consulo.ui.TextItemPresentation;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 12/10/2021
 */
public class DesktopAdvancedLabelImpl extends SwingComponentDelegate<DesktopAdvancedLabelImpl.MySimpleColoredComponent> implements AdvancedLabel {
  public static class MySimpleColoredComponent extends SimpleColoredComponent {

  }

  public DesktopAdvancedLabelImpl() {
    initialize(new MySimpleColoredComponent());
  }

  @Nonnull
  @Override
  public AdvancedLabel updatePresentation(@Nonnull Consumer<TextItemPresentation> consumer) {
    toAWTComponent().clear();

    consumer.accept(new DesktopTextItemPresentationImpl(toAWTComponent()));
    return this;
  }
}
