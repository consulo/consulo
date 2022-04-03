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

package consulo.ui.ex.awt;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.awt.util.Alarm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("MethodMayBeStatic")
public abstract class AutoScrollFromSourceHandler implements Disposable {
  protected final Project myProject;
  protected final Alarm myAlarm;
  protected JComponent myComponent;

  public AutoScrollFromSourceHandler(@Nonnull Project project, @Nonnull JComponent view) {
    this(project, view, null);
  }

  public AutoScrollFromSourceHandler(@Nonnull Project project, @Nonnull JComponent view, @Nullable Disposable parentDisposable) {
    myProject = project;

    if (parentDisposable != null) {
      Disposer.register(parentDisposable, this);
    }
    myComponent = view;
    myAlarm = new Alarm(this);
  }

  protected abstract boolean isAutoScrollEnabled();

  protected abstract void setAutoScrollEnabled(boolean enabled);

  protected ModalityState getModalityState() {
    return Application.get().getCurrentModalityState();
  }

  protected long getAlarmDelay() {
    return 500;
  }

  public abstract void install();

  @Override
  public void dispose() {
    if (!myAlarm.isDisposed()) {
      myAlarm.cancelAllRequests();
    }
  }

  public ToggleAction createToggleAction() {
    return new AutoScrollFromSourceAction();
  }

  private class AutoScrollFromSourceAction extends ToggleAction implements DumbAware {
    public AutoScrollFromSourceAction() {
      super(UIBundle.message("autoscroll.from.source.action.name"), UIBundle.message("autoscroll.from.source.action.description"), AllIcons.General.AutoscrollFromSource);
    }

    @Override
    public boolean isSelected(final AnActionEvent event) {
      return isAutoScrollEnabled();
    }

    @Override
    public void setSelected(final AnActionEvent event, final boolean flag) {
      setAutoScrollEnabled(flag);
    }
  }
}

