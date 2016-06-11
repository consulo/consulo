/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.servlet.ui;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.intellij.util.Consumer;
import consulo.web.gwtUI.shared.UIServerEvent;
import consulo.web.gwtUI.shared.UIServerEventType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIAccessHelper {
  public static final UIAccessHelper ourInstance = new UIAccessHelper();

  private Executor myExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  private static ThreadLocal<UISessionManager.UIContext> ourLocal = new ThreadLocal<UISessionManager.UIContext>();

  public void run(final UISessionManager.UIContext context, final Runnable runnable) {
    if(!context.getSession().isOpen()) {
      return;
    }
    myExecutor.execute(new Runnable() {
      @Override
      public void run() {
        ourLocal.set(context);

        runnable.run();

        ourLocal.set(null);
      }
    });
  }

  public boolean isUIThread() {
    return ourLocal.get() != null;
  }

  public static void atUI(@NotNull UIServerEventType type, @NotNull Consumer<UIServerEvent> consumer) {
    final UISessionManager.UIContext context = ourLocal.get();
    if (context == null) {
      throw new IllegalArgumentException("Call must be wrapped inside UI thread");
    }

    final AutoBean<UIServerEvent> bean = UISessionManager.ourEventFactory.serverEvent();

    final UIServerEvent event = bean.as();
    event.setType(type);
    event.setSessionId(context.getId());

    consumer.consume(event);

    context.send(bean);
  }
}
