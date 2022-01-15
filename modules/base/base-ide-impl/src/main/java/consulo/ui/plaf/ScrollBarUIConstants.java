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
package consulo.ui.plaf;

import com.intellij.util.ui.RegionPainter;
import consulo.util.dataholder.Key;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 28-Jun-17
 */
public interface ScrollBarUIConstants {
  /**
   * This key defines a region painter, which is used by the custom ScrollBarUI
   * to draw additional paintings (i.e. error stripes) on the scrollbar's track.
   *
   * @see com.intellij.util.ui.UIUtil#putClientProperty
   */
  Key<RegionPainter<Object>> TRACK = Key.create("JB_SCROLL_BAR_TRACK");

  /**
   * This key define a factory for custom increase button, which is used by the custom ScrollBarUI
   */
  Key<Supplier<? extends JButton>> INCREASE_BUTTON_FACTORY = Key.create("SCROLL_INCREASE_BUTTON_FACTORY");
}
