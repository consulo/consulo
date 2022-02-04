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
package consulo.editor.impl;

import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.editor.ex.ScrollingModelEx;
import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Common part from desktop scrolling model
 */
public abstract class CodeEditorScrollingModelBase implements ScrollingModelEx {
  private static final Logger LOG = Logger.getInstance(CodeEditorScrollingModelBase.class);

  protected final CodeEditorBase myEditor;

  protected final List<VisibleAreaListener> myVisibleAreaListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public CodeEditorScrollingModelBase(CodeEditorBase editor) {
    myEditor = editor;
  }

  public void finishAnimation() {
  }

  @Override
  public void addVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
    myVisibleAreaListeners.add(listener);
  }

  @Override
  public void removeVisibleAreaListener(@Nonnull VisibleAreaListener listener) {
    boolean success = myVisibleAreaListeners.remove(listener);
    LOG.assertTrue(success);
  }

  public void dispose() {
  }
}
