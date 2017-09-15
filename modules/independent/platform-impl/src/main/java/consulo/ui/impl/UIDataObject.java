/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.impl;

import com.intellij.util.EventDispatcher;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.style.ColorKey;

import java.util.Collection;
import java.util.EventListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 14-Sep-17
 */
public class UIDataObject {
  public static class BorderInfo {
    public BorderPosition myBorderPosition;
    public BorderStyle myBorderStyle;
    public ColorKey myColorKey;
    public int myWidth;

    public BorderInfo(BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
      myBorderPosition = borderPosition;
      myBorderStyle = borderStyle;
      myColorKey = colorKey;
      myWidth = width;
    }
  }

  private Map<Class, EventDispatcher> myListeners = new ConcurrentHashMap<>();
  private Map<BorderPosition, BorderInfo> myBorders = new ConcurrentHashMap<>();

  @SuppressWarnings("unchecked")
  public <T extends EventListener> Runnable addListener(Class<T> clazz, T value) {
    EventDispatcher<T> eventDispatcher = myListeners.computeIfAbsent(clazz, aClass -> EventDispatcher.create(aClass));
    eventDispatcher.addListener(value);
    return () -> eventDispatcher.removeListener(value);
  }

  @SuppressWarnings("unchecked")
  public <T extends EventListener> T getDispatcher(Class<T> clazz) {
    return (T)myListeners.computeIfAbsent(clazz, aClass -> EventDispatcher.create(aClass)).getMulticaster();
  }

  public void addBorder(BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    BorderInfo borderInfo = new BorderInfo(borderPosition, borderStyle, colorKey, width);
    myBorders.put(borderPosition, borderInfo);
  }

  public void removeBorder(BorderPosition borderPosition) {
    myBorders.remove(borderPosition);
  }

  public Collection<BorderInfo> getBorders() {
    return myBorders.values();
  }
}
