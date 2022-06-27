// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.document.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// FIXME [VISTALL] this topic is app&project
@TopicAPI(ComponentScope.APPLICATION)
public interface EncodingManagerListener {
  void propertyChanged(@Nullable Document document, @Nonnull String propertyName, final Object oldValue, final Object newValue);
}
