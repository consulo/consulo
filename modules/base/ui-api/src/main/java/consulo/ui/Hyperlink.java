/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public interface Hyperlink extends Component {
  static Hyperlink create(LocalizeValue text) {
    return UIInternal.get()._Components_hyperlink(text);
  }
  static Hyperlink create(LocalizeValue text, @RequiredUIAccess ComponentEventListener<Component, HyperlinkEvent> listener) {
    Hyperlink hyperlink = UIInternal.get()._Components_hyperlink(text);
    hyperlink.addHyperlinkListener(listener);
    return hyperlink;
  }
  LocalizeValue getText();

  @RequiredUIAccess
  void setText(LocalizeValue text);

  void setIcon(@Nullable Image icon);

  @Nullable
  Image getIcon();
  default Disposable addHyperlinkListener(ComponentEventListener<Component, HyperlinkEvent> hyperlinkListener) {
    return addListener(HyperlinkEvent.class, hyperlinkListener);
  }
}
