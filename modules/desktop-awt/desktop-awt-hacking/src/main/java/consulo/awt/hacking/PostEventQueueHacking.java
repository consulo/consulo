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
package consulo.awt.hacking;

import sun.awt.AppContext;

import java.awt.*;
import java.lang.reflect.Constructor;

/**
 * @author VISTALL
 * @since 2019-11-18
 */
public class PostEventQueueHacking {
  public static void replacePostEventQueue(EventQueue eventQueue) {
    try {
      Class<?> aClass = Class.forName("sun.awt.PostEventQueue");
      Constructor<?> constructor = aClass.getDeclaredConstructor(EventQueue.class);
      constructor.setAccessible(true);
      Object postEventQueue = constructor.newInstance(eventQueue);

      AppContext.getAppContext().put("PostEventQueue", postEventQueue);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
