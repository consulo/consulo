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
package com.intellij.ide;

import consulo.annotations.ReviewAfterMigrationToJRE;
import consulo.awt.hacking.GraphicsEnvironmentHacking;
import consulo.logging.Logger;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// manybe use Desktop class after migration to jdk 9
@ReviewAfterMigrationToJRE(9)
public class DisplayChangeDetector {
  private static final Logger LOG = Logger.getInstance(DisplayChangeDetector.class);
  private static final DisplayChangeDetector INSTANCE = new DisplayChangeDetector();

  public static DisplayChangeDetector getInstance() {
    return INSTANCE;
  }

  @SuppressWarnings("FieldCanBeLocal") // we need to keep a strong reference to this listener, as GraphicsEnvironment keeps only weak references to them
  private final Runnable myHandler;

  private final List<Listener> myListeners = new CopyOnWriteArrayList<Listener>();

  private DisplayChangeDetector() {
    myHandler = () -> {
      for (Listener listener : myListeners) {
        listener.displayChanged();
      }
    };

    try {
      GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

      env.getScreenDevices();    // init

      GraphicsEnvironmentHacking.addDisplayChangeListener(env, myHandler);
    }
    catch (HeadlessException ignored) {
    }
    catch (Throwable t) {
      LOG.error("Cannot setup display change listener", t);
    }
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(Listener listener) {
    myListeners.remove(listener);
  }

  public interface Listener {
    void displayChanged();
  }
}
