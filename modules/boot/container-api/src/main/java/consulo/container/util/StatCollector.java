/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.util;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-07-29
 */
public class StatCollector {
  public static final String APP_INITIALIZE = "app.initialize";

  private final Map<String, Long> myTimes = new ConcurrentHashMap<String, Long>();

  @Nonnull
  public Runnable mark(final String id) {
    final long start = System.currentTimeMillis();

    return new Runnable() {
      @Override
      public void run() {
        long l = System.currentTimeMillis() - start;

        myTimes.put(id, l);
      }
    };
  }

  public void markWith(final String id, Runnable task) {
    Runnable mark = mark(id);

    task.run();

    mark.run();
  }

  public void dump(String title, Consumer<String> logInfo) {
    logInfo.accept(title + ":");
    for (Map.Entry<String, Long> entry : data()) {
      logInfo.accept(" - " + entry.getKey() + " - " + entry.getValue() + " ms");
    }
  }

  @Nonnull
  public Set<Map.Entry<String, Long>> data() {
    return myTimes.entrySet();
  }
}
