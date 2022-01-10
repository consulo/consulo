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

/*
 * @author max
 */
package com.intellij.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import com.intellij.util.messages.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.ui.UIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class DesktopIconDeferrerImpl extends IconDeferrer implements Disposable {
  private static final ThreadLocal<Boolean> ourEvaluationIsInProgress = ThreadLocal.withInitial(() -> Boolean.FALSE);

  private final Object LOCK = new Object();

  private final Map<Object, Image> myIconsCache = new LinkedHashMap<Object, Image>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<Object, Image> eldest) {
      return size() > 1000;
    }
  };

  private long myLastClearTimestamp;

  @Inject
  public DesktopIconDeferrerImpl(Application application) {
    final MessageBusConnection connection = application.getMessageBus().connect();
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        clear();
      }
    });
    connection.subscribe(PsiModificationTracker.TOPIC, this::clear);
    connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        clear();
      }
    });
    LowMemoryWatcher.register(this::clear, this);
  }

  protected final void clear() {
    synchronized (LOCK) {
      myIconsCache.clear();
      myLastClearTimestamp++;
    }
  }

  @Override
  public void dispose() {
    clear();
  }

  @Override
  public <T> Image defer(final Image base, final T param, @Nonnull final Function<T, Image> evaluator) {
    return deferImpl(base, param, evaluator, false);
  }

  @Override
  public <T> Image deferAutoUpdatable(Image base, T param, @Nonnull Function<T, Image> evaluator) {
    return deferImpl(base, param, evaluator, true);
  }

  private <T> Image deferImpl(Image base, T param, @Nonnull Function<T, Image> evaluator, final boolean autoUpdatable) {
    if (ourEvaluationIsInProgress.get()) {
      return evaluator.fun(param);
    }

    synchronized (LOCK) {
      Image result = myIconsCache.get(param);
      if (result == null) {
        final long started = myLastClearTimestamp;
        result = new DesktopDeferredIconImpl<>(base, param, evaluator, (DesktopDeferredIconImpl<T> source, T key, Image r) -> {
          synchronized (LOCK) {
            // check if our results is not outdated yet
            if (started == myLastClearTimestamp) {
              myIconsCache.put(key, autoUpdatable ? source : r);
            }
          }
        }, autoUpdatable);
        myIconsCache.put(param, result);
      }

      return result;
    }
  }

  protected void cacheIcon(Object key, Image value) {
    synchronized (LOCK) {
      myIconsCache.put(key, value);
    }
  }

  static void evaluateDeferred(@Nonnull Runnable runnable) {
    try {
      ourEvaluationIsInProgress.set(Boolean.TRUE);
      runnable.run();
    }
    finally {
      ourEvaluationIsInProgress.set(Boolean.FALSE);
    }
  }

  @Override
  public boolean equalIcons(Image icon1, Image icon2) {
    return DesktopDeferredIconImpl.equalIcons(icon1, icon2);
  }
}
