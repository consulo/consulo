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
package consulo.web;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.idea.Main;
import com.intellij.openapi.application.PathManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ThreeState;
import org.mustbe.consulo.application.ApplicationProperties;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLDecoder;

/**
 * @author VISTALL
 * @since 15-May-16
 */
public class AppInit {
  public static ThreeState inited = ThreeState.NO;

  public static boolean init() {
    switch (inited) {
      case YES:
        return true;
      case NO:
        inited = ThreeState.UNSURE;
        new Thread() {
          @Override
          public void run() {
            initApplication();
          }
        }.start();
        return false;
      default:
        return false;
    }
  }

  private static void initApplication()  {
    try {
     // Logger.setFactory(OurWebLoggerFactory.class);

      File consuloWebJar = new File(URLDecoder.decode(AppInit.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF8"));
      File libFile = consuloWebJar.getParentFile();

      System.setProperty(PathManager.PROPERTY_CONFIG_PATH, libFile.getParent() + "/sandbox/config");
      System.setProperty(PathManager.PROPERTY_SYSTEM_PATH, libFile.getParent() + "/sandbox/system");
      System.setProperty(PathManager.PROPERTY_PLUGINS_PATH, new File(libFile, "plugins").getPath());
      System.setProperty(ApplicationProperties.CONSULO_AS_WEB_APP, Boolean.TRUE.toString());
      System.setProperty("java.awt.headless", Boolean.TRUE.toString());

      Main.setFlags(ArrayUtil.EMPTY_STRING_ARRAY);

      Class<?> klass = Class.forName(PluginManager.class.getName());
      Method startMethod = klass.getDeclaredMethod("start", String.class, String.class, String[].class);
      startMethod.setAccessible(true);
      startMethod.invoke(null, Main.class.getName() + "Impl", "start", ArrayUtil.EMPTY_STRING_ARRAY);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
