/*
 * Copyright 2013-2016 consulo.io
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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.io.URLUtil;
import consulo.ui.internal.WGwtImageRefImpl;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class UIIconServlet extends HttpServlet {
  private static ConcurrentFactoryMap<URL, byte[]> ourCache = new ConcurrentFactoryMap<URL, byte[]>() {
    @Nullable
    @Override
    protected byte[] create(URL key) {
      try {
        final InputStream inputStream = URLUtil.openStream(key);
        return FileUtil.loadBytes(inputStream);
      }
      catch (IOException e) {
        return new byte[0];
      }
    }
  };

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String urlHash = req.getParameter("urlHash");
    if (urlHash == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    final URL url = WGwtImageRefImpl.ourURLCache.get(Integer.parseInt(StringUtil.unquoteString(urlHash)));
    if (url == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    byte[] bytes = ourCache.get(url);

    assert bytes != null;

    resp.setContentType("image/png");
    resp.setContentLength(bytes.length);

    ServletOutputStream outputStream = resp.getOutputStream();
    outputStream.write(bytes);
    outputStream.close();
  }
}
