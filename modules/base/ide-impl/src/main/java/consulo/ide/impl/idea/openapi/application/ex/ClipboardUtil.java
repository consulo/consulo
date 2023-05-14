/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.application.ex;

import consulo.application.Application;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.application.util.SystemInfo;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.datatransfer.DataFlavor;
import java.util.function.Supplier;

public class ClipboardUtil {
  private static final Logger LOG = Logger.getInstance(ClipboardUtil.class);

  public static <E> E handleClipboardSafely(@Nonnull Supplier<? extends E> supplier, @Nonnull Supplier<? extends E> onFail) {
    try {
      return supplier.get();
    }
    catch (IllegalStateException e) {
      if (SystemInfo.isWindows) {
        LOG.debug("Clipboard is busy");
      }
      else {
        LOG.warn(e);
      }
    }
    catch (NullPointerException e) {
      LOG.warn("Java bug #6322854", e);
    }
    catch (IllegalArgumentException e) {
      LOG.warn("Java bug #7173464", e);
    }
    return onFail.get();
  }

  @Nullable
  public static String getTextInClipboard() {
    if(!Application.get().isSwingApplication()) {
      return null; // TODO [VISTALL] no clipboard support
    }
    return CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
  }
}