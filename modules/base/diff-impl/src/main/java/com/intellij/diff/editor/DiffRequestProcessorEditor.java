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
package com.intellij.diff.editor;

import com.intellij.diff.impl.DiffRequestProcessor;
import com.intellij.diff.util.FileEditorBase;
import com.intellij.openapi.diff.DiffBundle;
import consulo.disposer.Disposer;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

// from kotlin
public class DiffRequestProcessorEditor extends FileEditorBase {
  private static final Logger LOG = Logger.getInstance(DiffRequestProcessorEditor.class);

  private class MyPanel extends JPanel {
    MyPanel(Component component) {
      super(new BorderLayout());

      add(component, BorderLayout.CENTER);

      addContainerListener(new ContainerAdapter() {
        @Override
        public void componentRemoved(ContainerEvent e) {
          if (myDisposed) {
            return;
          }

          LOG.error("DiffRequestProcessor cannot be shown twice, see com.intellij.ide.actions.SplitAction.FORBID_TAB_SPLIT, file: " + getFile());
        }
      });
    }
  }

  private final DiffVirtualFile myFile;
  private final DiffRequestProcessor myProcessor;

  private boolean myDisposed;

  private MyPanel myPanel;

  public DiffRequestProcessorEditor(DiffVirtualFile file, DiffRequestProcessor processor) {
    myFile = file;
    myProcessor = processor;

    myPanel = new MyPanel(processor.getComponent());
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
  public void selectNotify() {
    myProcessor.updateRequest();
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myProcessor.getPreferredFocusedComponent();
  }

  @Nullable
  @Override
  public DiffVirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  @Override
  public String getName() {
    return DiffBundle.message("diff.file.editor.name");
  }
}
