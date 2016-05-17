<html>
<head>
<title>Application Status</title>
</head>
<body bgcolor=white>

<%
response.setIntHeader("Refresh", 2);

boolean value = consulo.web.AppInit.init();
out.println("Application status: " + value);

if(value) {
  out.println("<br><a href=\"project\">Open project</a>");
}
%>

</body>
</html>
