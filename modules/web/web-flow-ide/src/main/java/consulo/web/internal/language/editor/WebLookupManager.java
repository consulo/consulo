/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.language.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.language.editor.completion.lookup.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import kava.beans.PropertyChangeListener;

/**
 * @author VISTALL
 * @since 24/08/2023
 */
@Singleton
@ServiceImpl
public class WebLookupManager extends LookupManager {
  @Nullable
  @Override
  public LookupEx showLookup(@Nonnull Editor editor,
                             @Nonnull LookupElement[] items,
                             @Nonnull String prefix,
                             @Nonnull LookupArranger arranger) {
    return null;
  }

  @Override
  public void hideActiveLookup() {

  }

  @Nullable
  @Override
  public LookupEx getActiveLookup() {
    return null;
  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {

  }

  @Override
  public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable disposable) {

  }

  @Override
  public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {

  }

  @Nonnull
  @Override
  public Lookup createLookup(@Nonnull Editor editor,
                             @Nonnull LookupElement[] items,
                             @Nonnull String prefix,
                             @Nonnull LookupArranger arranger) {
    throw new UnsupportedOperationException();
  }
}
