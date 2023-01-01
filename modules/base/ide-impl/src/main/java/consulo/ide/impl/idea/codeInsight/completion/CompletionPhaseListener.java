// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

/**
 * @author yole
 */
@TopicAPI(ComponentScope.APPLICATION)
public interface CompletionPhaseListener {
  void completionPhaseChanged(boolean isCompletionRunning);
}
