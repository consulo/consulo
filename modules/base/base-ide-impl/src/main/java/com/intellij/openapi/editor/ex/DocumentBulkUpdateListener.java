/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.openapi.editor.ex;

import consulo.document.Document;
import consulo.document.event.DocumentListener;
import consulo.component.messagebus.Topic;
import javax.annotation.Nonnull;

/**
 * @deprecated Use {@link DocumentListener} instead
 */
@Deprecated
public interface DocumentBulkUpdateListener {
  Topic<DocumentBulkUpdateListener> TOPIC = Topic.create("Bulk document change notification like reformat, etc.", DocumentBulkUpdateListener.class);

  void updateStarted(@Nonnull Document doc);

  void updateFinished(@Nonnull Document doc);

  /**
   * @deprecated Use {@link DocumentListener} instead
   */
  @Deprecated
  abstract class Adapter implements DocumentBulkUpdateListener {
    @Override
    public void updateFinished(@Nonnull final Document doc) {
    }

    @Override
    public void updateStarted(@Nonnull final Document doc) {
    }
  }
}