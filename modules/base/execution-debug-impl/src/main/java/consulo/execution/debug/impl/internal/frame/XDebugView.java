/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.frame;

import consulo.execution.internal.layout.ViewContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.dataContext.DataContext;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.awt.util.SingleAlarm;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.EventObject;

/**
 * @author nik
 */
public abstract class XDebugView implements Disposable {
  public enum SessionEvent {PAUSED, BEFORE_RESUME, RESUMED, STOPPED, FRAME_CHANGED, SETTINGS_CHANGED}

  private final SingleAlarm myClearAlarm;
  private static final int VIEW_CLEAR_DELAY = 100; //ms

  public XDebugView() {
    myClearAlarm = new SingleAlarm(() -> clear(), VIEW_CLEAR_DELAY, this);
  }

  protected final void requestClear() {
    myClearAlarm.cancelAndRequest();
  }

  protected final void cancelClear() {
    myClearAlarm.cancel();
  }

  protected abstract void clear();

  public abstract void processSessionEvent(SessionEvent event, XDebugSession session);

  protected static @Nullable XDebugSession getSession(EventObject e) {
    Component component = e.getSource() instanceof Component ? (Component)e.getSource() : null;
    return component == null ? null : getSession(component);
  }

  public static @Nullable XDebugSession getSession(Component component) {
    return getData(XDebugSession.DATA_KEY, component);
  }

  protected @Nullable VirtualFile getCurrentFile(Component component) {
    XDebugSession session = getSession(component);
    if (session != null) {
      XSourcePosition position = session.getCurrentPosition();
      if (position != null) {
        return position.getFile();
      }
    }
    return null;
  }

  public static @Nullable <T> T getData(Key<T> key, Component component) {
    DataContext dataContext = DataManager.getInstance().getDataContext(component);
    ViewContext viewContext = dataContext.getData(ViewContext.CONTEXT_KEY);
    ContentManager contentManager = viewContext == null ? null : viewContext.getContentManager();
    if (contentManager != null) {
      T data = DataManager.getInstance().getDataContext(contentManager.getComponent()).getData(key);
      if (data != null) {
        return data;
      }
    }
    return dataContext.getData(key);
  }
}
