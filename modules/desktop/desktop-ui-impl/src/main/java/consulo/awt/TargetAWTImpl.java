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
package consulo.awt;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.BitUtil;
import consulo.ui.TextAttribute;
import consulo.ui.shared.ColorValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-05-15
 */
public class TargetAWTImpl extends TargetAWT {
  public static SimpleTextAttributes from(@Nonnull TextAttribute textAttribute) {
    int mask = 0;

    mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_PLAIN, BitUtil.isSet(textAttribute.getStyle(), TextAttribute.STYLE_PLAIN));
    mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_BOLD, BitUtil.isSet(textAttribute.getStyle(), TextAttribute.STYLE_BOLD));
    mask = BitUtil.set(mask, SimpleTextAttributes.STYLE_ITALIC, BitUtil.isSet(textAttribute.getStyle(), TextAttribute.STYLE_ITALIC));

    ColorValue backgroundColor = textAttribute.getBackgroundColor();
    ColorValue foregroundColor = textAttribute.getForegroundColor();
    return new SimpleTextAttributes(mask, to(foregroundColor), to(backgroundColor));
  }
}
