/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.errorreport.itn;

import com.intellij.diagnostic.DiagnosticBundle;
import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.errorreport.error.InternalEAPException;
import com.intellij.errorreport.error.NoSuchEAPUserException;
import com.intellij.errorreport.error.UpdateAvailableException;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.net.HttpConfigurable;
import consulo.ide.webService.WebServiceApi;
import org.jetbrains.annotations.NonNls;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: stathik
 * Date: Aug 4, 2003
 * Time: 8:12:00 PM
 * To change this template use Options | File Templates.
 */
public class ITNProxy {
  @NonNls public static final String ENCODING = "UTF8";
  public static final String POST_DELIMITER = "&";
  public static int postNewThread (String login, String password, ErrorBean error, String compilationTimestamp)
    throws IOException, NoSuchEAPUserException, InternalEAPException, UpdateAvailableException {

    @NonNls List<Pair<String, String>> params = createParametersFor(login,
                                                                    password,
                                                                    error,
                                                                    compilationTimestamp,
                                                                    ApplicationManager.getApplication(),
                                                                    (ApplicationInfoEx) ApplicationInfo.getInstance(),
                                                                    ApplicationNamesInfo.getInstance());

    HttpURLConnection connection = doPut(new URL(WebServiceApi.ERROR_REPORTER_API.buildUrl("create")), join(params));
    int responseCode = connection.getResponseCode();

    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new InternalEAPException(DiagnosticBundle.message("error.http.result.code", responseCode));
    }

    String reply;

    try (InputStream is = new BufferedInputStream(connection.getInputStream())) {
      reply = readFrom(is);
    }

    if ("unauthorized".equals(reply)) {
      throw new NoSuchEAPUserException(login);
    }

    if (reply.startsWith("update ")) {
      throw new UpdateAvailableException(reply.substring(7));
    }

    if (reply.startsWith("message ")) {
      throw new InternalEAPException(reply.substring(8));
    }

    try {
      return Integer.valueOf(reply.trim());
    } catch (NumberFormatException ex) {
      throw new InternalEAPException(DiagnosticBundle.message("error.itn.returns.wrong.data"));
    }
  }

  private static List<Pair<String, String>> createParametersFor(String login,
                                                                String password,
                                                                ErrorBean error,
                                                                String compilationTimestamp, Application application, ApplicationInfoEx appInfo,
                                                                ApplicationNamesInfo namesInfo) {
    @NonNls List<Pair<String,String>> params = new ArrayList<>();

    params.add(Pair.create("protocol.version", "1"));

    params.add(Pair.create("user.login", login));
    params.add(Pair.create("user.password", password));

    params.add(Pair.create("os.name", SystemProperties.getOsName()));

    params.add(Pair.create("java.version", SystemProperties.getJavaVersion()));
    params.add(Pair.create("java.vm.vendor", SystemProperties.getJavaVmVendor()));

    params.add(Pair.create("app.name", namesInfo.getProductName()));
    params.add(Pair.create("app.name.full", namesInfo.getFullProductName()));
    params.add(Pair.create("app.name.version", appInfo.getVersionName()));
    params.add(Pair.create("app.eap", Boolean.toString(appInfo.isEAP())));
    params.add(Pair.create("app.internal", Boolean.toString(application.isInternal())));
    params.add(Pair.create("app.build", appInfo.getBuild().asString()));
    params.add(Pair.create("app.version.major", appInfo.getMajorVersion()));
    params.add(Pair.create("app.version.minor", appInfo.getMinorVersion()));
    params.add(Pair.create("app.build.date", format(appInfo.getBuildDate())));
    params.add(Pair.create("app.build.date.release", format(appInfo.getMajorReleaseBuildDate())));
    params.add(Pair.create("app.compilation.timestamp", compilationTimestamp));

    params.add(Pair.create("update.channel.status", consulo.ide.updateSettings.UpdateSettings.getInstance().getChannel().name()));

    params.add(Pair.create("plugin.name", error.getPluginName()));
    params.add(Pair.create("plugin.version", error.getPluginVersion()));

    params.add(Pair.create("last.action", error.getLastAction()));
    params.add(Pair.create("previous.exception",
                           error.getPreviousException() == null ? null : Integer.toString(error.getPreviousException())));

    params.add(Pair.create("error.message", error.getMessage()));
    params.add(Pair.create("error.stacktrace", error.getStackTrace()));

    params.add(Pair.create("error.description", error.getDescription()));

    params.add(Pair.create("assignee.id", error.getAssigneeId() == null ? null : Integer.toString(error.getAssigneeId())));

    for (Attachment attachment : error.getAttachments()) {
      params.add(Pair.create("attachment.name", attachment.getName()));
      params.add(Pair.create("attachment.value", attachment.getEncodedBytes()));
    }

    return params;
  }

  private static String readFrom(InputStream is) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int c;
    while ((c = is.read()) != -1) {
      out.write(c);
    }
    String s = out.toString();
    out.close();
    return s;
  }

  private static String format(Calendar calendar) {
    return calendar == null ?  null : Long.toString(calendar.getTime().getTime());
  }

  private static HttpURLConnection doPut(URL url, byte[] bytes) throws IOException {
    HttpURLConnection connection = (HttpURLConnection)HttpConfigurable.getInstance().openConnection(url.toString());

    connection.setReadTimeout(10 * 1000);
    connection.setConnectTimeout(10 * 1000);
    connection.setRequestMethod("PUT");
    connection.setDoInput(true);
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type", String.format("%s; charset=%s", "application/x-www-form-urlencoded", ENCODING));
    connection.setRequestProperty("Content-Length", Integer.toString(bytes.length));

    try (OutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
      out.write(bytes);
      out.flush();
    }

    return connection;
  }

  private static byte[] join(List<Pair<String, String>> params) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();

    Iterator<Pair<String, String>> it = params.iterator();

    while (it.hasNext()) {
      Pair<String, String> param = it.next();

      if (StringUtil.isEmpty(param.first))
        throw new IllegalArgumentException(param.toString());

      if (StringUtil.isNotEmpty(param.second))
        builder.append(param.first).append("=").append(URLEncoder.encode(param.second, ENCODING));

      if (it.hasNext())
        builder.append(POST_DELIMITER);
    }

    return builder.toString().getBytes();
  }
}
