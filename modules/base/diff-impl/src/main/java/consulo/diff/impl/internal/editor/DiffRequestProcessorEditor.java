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
package consulo.diff.impl.internal.editor;

import consulo.diff.impl.internal.DiffRequestProcessor;
import consulo.diff.localize.DiffLocalize;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

// from kotlin
public class DiffRequestProcessorEditor extends FileEditorBase {
  private final DiffVirtualFile myFile;
  private final DiffRequestProcessor myProcessor;

  private boolean myDisposed;

  public DiffRequestProcessorEditor(DiffVirtualFile file, DiffRequestProcessor processor) {
    myFile = file;
    myProcessor = processor;
  }

  public DiffRequestProcessor getProcessor() {
    return myProcessor;
  }

  @Override
  public void dispose() {
    myDisposed = true;

    Disposer.dispose(myProcessor);

    super.dispose();
  }

  @Override
  public boolean isValid() {
    return !myDisposed && !myProcessor.isDisposed();
  }

  @Override
  @RequiredUIAccess
  public void selectNotify() {
    myProcessor.updateRequest();
  }

  @Override
  public Component getUIComponent() {
    return myProcessor.getComponent();
  }

  @Override
  public @Nullable Component getPreferredFocusedUIComponent() {
    return myProcessor.getPreferredFocusedComponent();
  }

  @Override
  public @Nullable DiffVirtualFile getFile() {
    return myFile;
  }

  @Override
  public String getName() {
    return DiffLocalize.diffFileEditorName().get();
  }
}
