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
package consulo.util.jna;

import com.sun.jna.Native;
import org.slf4j.Logger;

public class JnaLoader {
  private static volatile boolean ourJnaLoaded = false;

  public static synchronized void load(Logger logger) {
    try {
      long t = System.currentTimeMillis();
      int ptrSize = Native.POINTER_SIZE;
      t = System.currentTimeMillis() - t;
      logger.info("JNA library (" + (ptrSize << 3) + "-bit) loaded in " + t + " ms");
      ourJnaLoaded = true;
    }
    catch (Throwable t) {
      String osName = System.getProperty("os.name");
      String osVersion = System.getProperty("os.version").toLowerCase();

      logger.error("Unable to load JNA library (OS: " + osName + " " + osVersion + ")", t);
    }
  }

  public static boolean isLoaded() {
    return ourJnaLoaded;
  }
}