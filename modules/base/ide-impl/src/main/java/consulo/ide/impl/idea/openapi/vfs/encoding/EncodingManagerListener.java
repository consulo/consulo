// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vfs.encoding;

import consulo.document.Document;
import consulo.component.messagebus.TopicImpl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EncodingManagerListener {
  TopicImpl<EncodingManagerListener> ENCODING_MANAGER_CHANGES = new TopicImpl<>("encoding manager changes", EncodingManagerListener.class);

  void propertyChanged(@Nullable Document document, @Nonnull String propertyName, final Object oldValue, final Object newValue);
}
