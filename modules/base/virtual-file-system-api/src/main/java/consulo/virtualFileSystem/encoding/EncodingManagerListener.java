// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.encoding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;

import org.jspecify.annotations.Nullable;

// FIXME [VISTALL] this topic is app&project
@TopicAPI(ComponentScope.APPLICATION)
public interface EncodingManagerListener {
  /**
   * @param document     instanceof {@link consulo.document.Document}
   * @param propertyName
   * @param oldValue
   * @param newValue
   */
  void propertyChanged(@Nullable Object document, String propertyName, Object oldValue, Object newValue);
}
