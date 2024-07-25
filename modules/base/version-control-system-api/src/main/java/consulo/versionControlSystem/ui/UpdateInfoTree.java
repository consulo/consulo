/*
 * Copyright 2013-2024 consulo.io
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
package consulo.versionControlSystem.ui;

import consulo.disposer.Disposable;
import consulo.localHistory.Label;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 25-Jul-24
 */
public interface UpdateInfoTree extends Disposable {
  JComponent getComponent();

  void setBefore(Label before);

  void setAfter(Label after);
}
