// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.xdebugger.impl.breakpoints;

import consulo.document.Document;
import jakarta.annotation.Nonnull;

public interface XBreakpointTypeWithDocumentDelegation {
  /*
   * return a custom document which should be used for breakpoint highlighting
   */
  @Nonnull
  Document getDocumentForHighlighting(@Nonnull Document document);
}
