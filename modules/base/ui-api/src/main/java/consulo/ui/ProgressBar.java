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
package consulo.ui;

import consulo.ui.internal.UIInternal;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public interface ProgressBar extends Component {
  static ProgressBar create() {
    return UIInternal.get()._Components_progressBar();
  }

  void setIndeterminate(boolean value);

  boolean isIndeterminate();

  void setMinimum(int value);

  void setMaximum(int value);

  void setValue(int value);
}
