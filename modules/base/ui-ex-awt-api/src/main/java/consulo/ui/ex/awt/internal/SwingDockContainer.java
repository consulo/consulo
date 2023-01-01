/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.ex.awt.internal;

import consulo.project.ui.wm.dock.DockContainer;
import consulo.ui.ex.awt.RelativeRectangle;

/**
 * @author VISTALL
 * @since 18-Apr-22
 */
public interface SwingDockContainer extends DockContainer {

  RelativeRectangle getAcceptArea();

  /**
   * This area is used when nothing was found with getAcceptArea
   */
  RelativeRectangle getAcceptAreaFallback();
}
