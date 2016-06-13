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
package consulo.web.servlet;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.BitUtil;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-May-16
 */
@Deprecated
public class IconServlet extends HttpServlet {
  private static final String[][] ourImagePluginInfos =
          new String[][]{{"org.mustbe.consulo.csharp", "org.mustbe.consulo.csharp.CSharpIcons"}, {"org.consulo.java", "org.mustbe.consulo.java.JavaIcons"},
                  {"com.intellij", "com.intellij.icons.AllIcons"}};

  private AtomicNotNullLazyValue<Map<String, byte[]>> myCacheValue = new AtomicNotNullLazyValue<Map<String, byte[]>>() {
    @NotNull
    @Override
    protected Map<String, byte[]> compute() {
      return buildMap();
    }
  };

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String path = req.getParameter("path");
    if (path == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    Map<String, byte[]> map = myCacheValue.getValue();

    byte[] bytes = map.get(StringUtil.unquoteString(path));
    if (bytes == null) {
      bytes = map.get("/toolbar/unknown.png");
    }

    assert bytes != null;

    resp.setContentType("image/png");
    resp.setContentLength(bytes.length);

    ServletOutputStream outputStream = resp.getOutputStream();
    outputStream.write(bytes);
    outputStream.close();
  }

  @NotNull
  private Map<String, byte[]> buildMap() {
    final Map<String, byte[]> cache = new HashMap<String, byte[]>();

    for (String[] info : ourImagePluginInfos) {
      String pluginId = info[0];
      String classInfo = info[1];

      IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.findId(pluginId));
      if (plugin == null) {
        continue;
      }

      try {
        Class<?> aClass = plugin.getPluginClassLoader().loadClass(classInfo);

        process(aClass, new PairConsumer<String, byte[]>() {
          @Override
          public void consume(String s, byte[] bytes) {
            cache.put(s, bytes);
          }
        });
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    return cache;
  }

  private void process(Class clazz, PairConsumer<String, byte[]> consumer) throws Exception {
    Field[] declaredFields = clazz.getDeclaredFields();
    for (Field declaredField : declaredFields) {
      if (BitUtil.isSet(declaredField.getModifiers(), Modifier.STATIC) && declaredField.getType() == Icon.class) {
        declaredField.setAccessible(true);


        Icon o = (Icon)declaredField.get(null);
        if (o != null) {
          try {
            String maybeUrl = o.toString();
            URL url = new URL(maybeUrl);

            InputStream stream = url.openStream();
            byte[] bytes = FileUtil.loadBytes(stream);
            stream.close();

            int i = maybeUrl.indexOf("!/");
            if (i != -1) {
              String path = maybeUrl.substring(i + 1, maybeUrl.length());
              consumer.consume(path, bytes);
            }
          }
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }

    Class[] classes = clazz.getClasses();
    for (Class aClass : classes) {
      process(aClass, consumer);
    }
  }
}
