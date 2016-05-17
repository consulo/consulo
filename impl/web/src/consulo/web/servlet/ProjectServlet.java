package consulo.web.servlet;

import consulo.web.AppInit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ProjectServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    response.setContentType("text/html");
    final PrintWriter writer = response.getWriter();

    writer.println("<html>");
    writer.println("<head>");
    writer.println("<title>Project</title>");
    writer.println(" <link rel=\"stylesheet\" href=\"main.css\">");
    writer.println("</head>");
    writer.println("<body>");
    if (!AppInit.init()) {
      writer.println("application is not inited");
      response.sendRedirect("index.jsp");
    }
    else {
      writer.println("<script type='text/javascript' src='consulo/consulo.nocache.js?" + System.currentTimeMillis() + "'></script>");
    }

    writer.println("</body>");
    writer.println("</html>");
  }
}
