/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff;

import consulo.diff.DiffRequestPanel;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.NoDiffRequest;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class DiffRequestPanelImpl implements DiffRequestPanel {
  @Nonnull
  private final JPanel myPanel;
  @Nonnull
  private final MyDiffRequestProcessor myProcessor;

  public DiffRequestPanelImpl(@Nullable Project project, @Nullable Window window) {
    myProcessor = new MyDiffRequestProcessor(project, window);
    myProcessor.putContextUserData(DiffUserDataKeys.DO_NOT_CHANGE_WINDOW_TITLE, true);

    myPanel = new JPanel(new BorderLayout()) {
      @Override
      public void addNotify() {
        super.addNotify();
        myProcessor.updateRequest();
      }
    };
    myPanel.add(myProcessor.getComponent());
  }

  @Override
  public void setRequest(@Nullable DiffRequest request) {
    setRequest(request, null);
  }

  @Override
  public void setRequest(@Nullable DiffRequest request, @Nullable Object identity) {
    myProcessor.setRequest(request, identity);
  }

  @RequiredUIAccess
  @Override
  public <T> void putContextHints(@Nonnull Key<T> key, @Nullable T value) {
    myProcessor.putContextUserData(key, value);
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

  @Override
  public void dispose() {
    Disposer.dispose(myProcessor);
  }

  private static class MyDiffRequestProcessor extends DiffRequestProcessor {
    @Nullable
    private final Window myWindow;

    @Nonnull
    private DiffRequest myRequest = NoDiffRequest.INSTANCE;
    @Nullable
    private Object myRequestIdentity = null;

    public MyDiffRequestProcessor(@Nullable Project project, @Nullable Window window) {
      super(project);
      myWindow = window;
    }

    public synchronized void setRequest(@Nullable DiffRequest request, @Nullable Object identity) {
      if (myRequestIdentity != null && identity != null && myRequestIdentity.equals(identity)) return;

      myRequest = request != null ? request : NoDiffRequest.INSTANCE;
      myRequestIdentity = identity;

      UIUtil.invokeLaterIfNeeded(() -> updateRequest());
    }

    @Override
    @RequiredUIAccess
    public synchronized void updateRequest(boolean force, @Nullable DiffUserDataKeysEx.ScrollToPolicy scrollToChangePolicy) {
      applyRequest(myRequest, force, scrollToChangePolicy);
    }

    @Override
    protected void setWindowTitle(@Nonnull String title) {
      if (myWindow == null) return;
      if (myWindow instanceof JDialog) ((JDialog)myWindow).setTitle(title);
      if (myWindow instanceof JFrame) ((JFrame)myWindow).setTitle(title);
    }
  }
}
