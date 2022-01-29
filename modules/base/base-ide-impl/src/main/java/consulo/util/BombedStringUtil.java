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
package consulo.util;

import consulo.application.progress.ProcessCanceledException;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 28/12/2021
 */
public class BombedStringUtil {
  @Nonnull
  @Contract(pure = true)
  public static CharSequence newBombedCharSequence(@Nonnull CharSequence sequence, long delay) {
    final long myTime = System.currentTimeMillis() + delay;
    return new StringUtil.BombedCharSequence(sequence) {
      @Override
      protected void checkCanceled() {
        long l = System.currentTimeMillis();
        if (l >= myTime) {
          throw new ProcessCanceledException();
        }
      }
    };
  }
}
