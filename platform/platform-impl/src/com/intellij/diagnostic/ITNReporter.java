/*
 * Copyright 2013 Jon S Akhtar (Sylvanaar)
 * Copyright 2013-2014 must-be.org
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.intellij.diagnostic;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.consulo.lombok.annotations.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.SharedConstants;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.intellij.openapi.diagnostic.SubmittedReportInfo.SubmissionStatus.*;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;

/**
 * @author Jon S Akhtar
 * @since 11:35:35 AM Oct 19, 2010
 */
@Logger
public class ITNReporter extends ErrorReportSubmitter {
  public static final String PROJECT = "CEP";
  @NonNls
  private static final String SERVER_URL = SharedConstants.BUG_TRACKER_URL;
  private static final String SERVER_REST_URL = SERVER_URL + "rest/";
  private static final String SERVER_ISSUE_URL = SERVER_REST_URL + "issue";
  private static final String LOGIN_URL = SERVER_REST_URL + "user/login";

  private final CookieManager cookieManager = new CookieManager();

  @Override
  public String getReportActionText() {
    return DiagnosticBundle.message("error.report.to.must-be.action");
  }

  @Override
  public SubmittedReportInfo submit(IdeaLoggingEvent[] ideaLoggingEvents, Component component) {
    return new SubmittedReportInfo(null, "0", SubmittedReportInfo.SubmissionStatus.FAILED);
  }

  @Override
  public boolean trySubmitAsync(final IdeaLoggingEvent[] events,
                                final String additionalInfo,
                                final Component parentComponent,
                                final Consumer<SubmittedReportInfo> consumer) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(parentComponent);
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);

    Task.Backgroundable task = new Task.Backgroundable(project, DiagnosticBundle.message("title.submitting.error.report")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        consumer.consume(doSubmit(events, additionalInfo, parentComponent));
      }
    };
    task.queue();
    return true;
  }

  private SubmittedReportInfo doSubmit(IdeaLoggingEvent[] ideaLoggingEvents, String addInfo, Component component) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(component);
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    final IdeaLoggingEvent ideaLoggingEvent = ideaLoggingEvents[0];
    final String throwableText = ideaLoggingEvent.getThrowableText();
    String description = throwableText.substring(0, Math.min(Math.max(80, throwableText.length()), 80));


    @SuppressWarnings("ThrowableResultOfMethodCallIgnored") Integer signature =
      ideaLoggingEvent.getThrowable().getStackTrace()[0].hashCode();

    String existing = findExisting(signature);
    if (existing != null) {
      final SubmittedReportInfo reportInfo = new SubmittedReportInfo(SERVER_URL + "issue/" + existing, existing, DUPLICATE);
      popupResultInfo(reportInfo, project);
      return reportInfo;
    }


    @NonNls StringBuilder descBuilder = new StringBuilder();

    descBuilder.append("Consulo Build: ").append(ApplicationInfo.getInstance().getBuild()).append('\n');

    String affectedVersion = null;
    Throwable t = ideaLoggingEvent.getThrowable();
    final PluginId pluginId = IdeErrorsDialog.findPluginId(t);
    if (pluginId != null) {
      final IdeaPluginDescriptor ideaPluginDescriptor = PluginManager.getPlugin(pluginId);
      if (ideaPluginDescriptor != null && !ideaPluginDescriptor.isBundled()) {
        descBuilder.append("Plugin ").append(ideaPluginDescriptor.getName()).append(" version: ")
          .append(ideaPluginDescriptor.getVersion()).append("\n");
        affectedVersion = ideaPluginDescriptor.getVersion();
      }
    }

    if (addInfo == null) {
      addInfo = "<none>";
    }

    descBuilder.append("\n\nDescription: ").append(addInfo);

    for (IdeaLoggingEvent e : ideaLoggingEvents) {
      descBuilder.append("\n\n").append(e.toString());
    }

    String result = submit(description, descBuilder.toString(), affectedVersion);
    LOGGER.info("Error submitted, response: " + result);

    if (result == null) {
      return new SubmittedReportInfo(SERVER_ISSUE_URL, "", FAILED);
    }

    String ResultString = null;
    try {
      Pattern regex = Pattern.compile("id=\"([^\"]+)\"", Pattern.DOTALL | Pattern.MULTILINE);
      Matcher regexMatcher = regex.matcher(result);
      if (regexMatcher.find()) {
        ResultString = regexMatcher.group(1);
      }
    }
    catch (PatternSyntaxException ex) {
      // Syntax error in the regular expression
    }

    SubmittedReportInfo.SubmissionStatus status = NEW_ISSUE;

    if (ResultString == null) {
      return new SubmittedReportInfo(SERVER_ISSUE_URL, "", FAILED);
    }


    final SubmittedReportInfo reportInfo = new SubmittedReportInfo(SERVER_URL + "issue/" + ResultString, ResultString, status);


    /* Now try to set the autosubmit user using a custom command */
   /* if (user != null) {
      runCommand(ResultString, "Autosubmit User " + user);
    }  */

    if (signature != 0) {
      runCommand(ResultString, "Exception Signature " + signature);
    }

    popupResultInfo(reportInfo, project);

    return reportInfo;
  }

  public String submit(String desc, String body, String affectedVersion) {
    if (isEmpty(desc)) {
      throw new RuntimeException(DiagnosticBundle.message("error.report.failure.message"));
    }

    String response = "";

    //Create Post String
    String data;
    try {
      // Log-In
      String userName = "reporter";
      data = URLEncoder.encode("login", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8");
      data += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode("reporter", "UTF-8");
      // Send Data To Page
      URL url = new URL(LOGIN_URL);
      URLConnection conn = url.openConnection();
      conn.setDoOutput(true);
      OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
      wr.write(data);
      wr.flush();


      // Get The Login Cookie
      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;

      while ((line = rd.readLine()) != null) {
        response += line;
      }

      LOGGER.info(response);
      cookieManager.storeCookies(conn);

      // project=TST&assignee=beto&summary=new issue&description=description of new issue
      // #&priority=show-stopper&type=feature&subsystem=UI&state=Reopened&affectsVersion=2.0,
      // 2.0.1&fixedVersions=2.0&fixedInBuild=2.0.1
      // POST /rest/issue?{project}&{assignee}&{summary}&{description}&{priority}&{type}&{subsystem}&{state
      // }&{affectsVersion}&{fixedVersions}&{attachments}&{fixedInBuild}

      // Make the description 1 line
      desc = desc.replaceAll("[\r\n]", "");

      // build the static post data for this issue
      data = URLEncoder.encode("project", "UTF-8") + "=" + URLEncoder.encode(PROJECT, "UTF-8");
      data += "&" + URLEncoder.encode("assignee", "UTF-8") + "=" + URLEncoder.encode("Unassigned", "UTF-8");
      data += "&" + URLEncoder.encode("summary", "UTF-8") + "=" + URLEncoder.encode(desc, "UTF-8");
      data += "&" + URLEncoder.encode("description", "UTF-8") + "=" +
              URLEncoder.encode(body, "UTF-8");
      data += "&" + URLEncoder.encode("priority", "UTF-8") + "=" + URLEncoder.encode("4", "UTF-8");
      data += "&" + URLEncoder.encode("type", "UTF-8") + "=" + URLEncoder.encode("Exception", "UTF-8");

      if (affectedVersion != null) {
        data += "&" + URLEncoder.encode("affectsVersion", "UTF-8") + "=" +
                URLEncoder.encode(affectedVersion, "UTF-8");
      }


      // We will use \n exclusively
      data = data.replaceAll("\r", "");


      // Send Data To Page
      url = new URL(SERVER_ISSUE_URL);
      conn = url.openConnection();

      conn.setDoOutput(true);
      cookieManager.setCookies(conn); // Use the login from earlier

      wr = new OutputStreamWriter(conn.getOutputStream());
      wr.write(data);
      wr.flush();

      // Get The Response
      rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      while ((line = rd.readLine()) != null) {
        response += line;
      }

    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return response;
  }

  //http://sylvanaar.myjetbrains.com/youtrack/rest/issue?filter=Exception%20Signature%3A801961033
  @Nullable
  private String findExisting(Integer signature) {
    try {
      LOGGER.debug(String.format("Run Query for signature <%s>", signature.toString()));
      URL url = new URL(String.format("%s?filter=Exception%%20Signature%%3A%s&with=id", SERVER_ISSUE_URL, signature.toString()));

      URLConnection conn = getUrlConnectionAndLogin(url);

      // Get The Response
      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      StringBuilder response = new StringBuilder(500);
      while ((line = rd.readLine()) != null) {
        response.append(line);
      }

      // <?xml version="1.0" encoding="UTF-8" standalone="yes"?><issueCompacts><issue
      // id="IDLua-1293"/></issueCompacts>
      LOGGER.debug(response.toString());

      String resultString = null;
      try {
        Pattern regex = Pattern.compile("<issue id=\"([^\"]+)\"/>", Pattern.MULTILINE);
        Matcher regexMatcher = regex.matcher(response.toString());
        if (regexMatcher.find()) {
          resultString = regexMatcher.group(1);
        }
      }
      catch (PatternSyntaxException ex) {
        // Syntax error in the regular expression
      }

      if (resultString != null) {
        LOGGER.debug("could be dumplicate of " + resultString);
        return resultString;
      }

    }
    catch (IOException e) {
      LOGGER.info("Query Failed", e);
    }

    return null;
  }

  private URLConnection getUrlConnectionAndLogin(URL url) throws IOException {
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    cookieManager.setCookies(conn);
    return conn;
  }

  // POST /rest/issue/{issue}/execute?{command}&{comment}&{group}&{disableNotifications}&{runAs}
  private void runCommand(String issueID, String command) {
    try {
      LOGGER.debug(String.format("Run Command <%s> on issue <%s>", command, issueID));
      URL url = new URL(SERVER_ISSUE_URL + "/" + issueID + "/execute");

      URLConnection conn = getUrlConnectionAndLogin(url);

      String data = URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command, "UTF-8");
      data += "&" + URLEncoder.encode("disableNotifications", "UTF-8") + "=" + URLEncoder.encode("true", "UTF-8");

      OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
      wr.write(data);
      wr.flush();


      // Get The Response
      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String line;
      String response = "";
      while ((line = rd.readLine()) != null) {
        response += line;
      }

      LOGGER.debug(response);

    }
    catch (IOException e) {
      LOGGER.info("Command Failed", e);
    }
  }

  private static void popupResultInfo(final SubmittedReportInfo reportInfo, final Project project) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        StringBuilder text = new StringBuilder("<html>");
        final String url = IdeErrorsDialog.getUrl(reportInfo, false);
        IdeErrorsDialog.appendSubmissionInformation(reportInfo, text, url);
        text.append(".");
        final SubmittedReportInfo.SubmissionStatus status = reportInfo.getStatus();
        if (status == SubmittedReportInfo.SubmissionStatus.NEW_ISSUE) {
          text.append("<br/>").append(DiagnosticBundle.message("error.report.gratitude"));
        }
        else if (status == SubmittedReportInfo.SubmissionStatus.DUPLICATE) {
          text.append("<br/>Possible duplicate report");
        }
        text.append("</html>");
        NotificationType type;
        if (status == SubmittedReportInfo.SubmissionStatus.FAILED) {
          type = NotificationType.ERROR;
        }
        else if (status == SubmittedReportInfo.SubmissionStatus.DUPLICATE) {
          type = NotificationType.WARNING;
        }
        else {
          type = NotificationType.INFORMATION;
        }
        NotificationListener listener = url != null ? new NotificationListener() {
          @Override
          public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
            BrowserUtil.launchBrowser(url);
            notification.expire();
          }
        } : null;
        ReportMessages.GROUP.createNotification(ReportMessages.ERROR_REPORT, text.toString(), type, listener).notify(project);
      }
    });
  }
}