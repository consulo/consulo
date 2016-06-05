package consulo.web.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class AppServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    response.setContentType("text/html");
    final PrintWriter writer = response.getWriter();

    writer.println("<html>");
    writer.println("<head>");
    writer.println("<title>Project</title>");
    writer.println("</head>");
    writer.println("<body>");
    writer.println("<script type='text/javascript' src='consulo/consulo.nocache.js?" + System.currentTimeMillis() + "'></script>");
    writer.println("</body>");
    writer.println("</html>");
  }
}
