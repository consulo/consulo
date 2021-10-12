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
package consulo.ui;

import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 12/10/2021
 */
public interface AdvancedLabel extends Component {
  @Nonnull
  static AdvancedLabel create() {
    return UIInternal.get()._Components_advancedLabel();
  }

  /**
   * Refresh presentation of label, fully reset state of label
   * @return this
   */
  @Nonnull
  AdvancedLabel updatePresentation(@Nonnull Consumer<TextItemPresentation> consumer);
}
