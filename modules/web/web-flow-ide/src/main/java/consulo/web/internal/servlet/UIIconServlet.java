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
package consulo.web.internal.servlet;

import consulo.ui.image.ImageKey;
import consulo.web.internal.ui.image.WebDataImageImpl;
import consulo.web.internal.ui.image.WebImageKeyImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
@WebServlet(urlPatterns = "/image")
public class UIIconServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String groupId = req.getParameter("groupId");
    String imageId = req.getParameter("imageId");
    if (groupId == null || imageId == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    WebImageKeyImpl imageKey = (WebImageKeyImpl)ImageKey.of(groupId, imageId, 0, 0);

    WebDataImageImpl image = (WebDataImageImpl)imageKey.calcImage();
    if(image == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (image.isSVG()) {
      resp.setContentType("image/svg+xml");
    }
    else {
      resp.setContentType("image/png");
    }

    byte[] bytes = image.getData();

    resp.setContentLength(bytes.length);

    ServletOutputStream outputStream = resp.getOutputStream();
    outputStream.write(bytes);
    outputStream.close();
  }
}
