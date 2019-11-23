/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.container.impl.classloader;

import consulo.container.plugin.PluginId;
import consulo.container.StartupError;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author peter
 */
public class PluginLoadStatistics {
  private static class InternalPluginClassCache extends PluginLoadStatistics {
    private final Object myLock = new Object();
    private final Map<PluginId, AtomicInteger> myClassCounts = new HashMap<PluginId, AtomicInteger>();

    private InternalPluginClassCache() {
    }

    @Override
    void addPluginClass(@Nonnull PluginId pluginId) {
      synchronized (myLock) {
        AtomicInteger value = myClassCounts.get(pluginId);
        if (value == null) {
          myClassCounts.put(pluginId, value = new AtomicInteger());
        }
        value.incrementAndGet();
      }
    }

    @Override
    public void dumpPluginClassStatistics(Consumer<String> logInfo) {
      List<PluginId> counters;
      synchronized (myLock) {
        //noinspection unchecked
        counters = new ArrayList(Arrays.asList(myClassCounts.keySet()));
      }

      Collections.sort(counters, new Comparator<PluginId>() {
        @Override
        public int compare(PluginId o1, PluginId o2) {
          return myClassCounts.get(o2).get() - myClassCounts.get(o1).get();
        }
      });
      for (PluginId id : counters) {
        logInfo.accept(id + " loaded " + myClassCounts.get(id) + " classes");
      }
    }
  }

  private static PluginLoadStatistics ourInstance;

  public static void initialize(boolean internal) {
    if (ourInstance != null) {
      throw new StartupError("duplicate initialize");
    }

    ourInstance = internal ? new InternalPluginClassCache() : new PluginLoadStatistics();
  }

  @Nonnull
  public static PluginLoadStatistics get() {
    if(ourInstance == null) {
      throw new StartupError("not initialized");
    }
    return ourInstance;
  }

  PluginLoadStatistics() {
  }

  void addPluginClass(@Nonnull PluginId pluginId) {
  }

  public void dumpPluginClassStatistics(Consumer<String> logInfo) {
  }
}
