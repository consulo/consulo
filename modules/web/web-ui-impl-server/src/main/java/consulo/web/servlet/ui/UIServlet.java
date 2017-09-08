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

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public abstract class UIServlet extends HttpServlet {
  private static final String ourGwtModuleName = "consulo.web.gwt";

  private final Class<? extends UIBuilder> myClass;

  public UIServlet(Class<? extends UIBuilder> aClass) {
    myClass = aClass;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("text/html");

    String id = UUID.randomUUID().toString();
    response.addCookie(new Cookie("ConsuloSessionId", id));

    UISessionManager.ourInstance.registerInitialSession(id, myClass);

    final PrintWriter writer = response.getWriter();
    writer.println("<html>");
    writer.println("<head>");
    writer.println("<title>Project</title>");
    writer.println("</head>");
    writer.println("<body>");
    writer.println("<script type='text/javascript' src='/webResources/" + ourGwtModuleName + "/" + ourGwtModuleName + ".nocache.js?" + System.currentTimeMillis() +
                   "'></script>");
    writer.println("</body>");
    writer.println("</html>");
  }
}
