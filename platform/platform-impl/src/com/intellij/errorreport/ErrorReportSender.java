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
package com.intellij.errorreport;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.diagnostic.DiagnosticBundle;
import com.intellij.errorreport.bean.ErrorBean;
import com.intellij.errorreport.error.InternalEAPException;
import com.intellij.errorreport.error.NoSuchEAPUserException;
import com.intellij.errorreport.error.UpdateAvailableException;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.util.net.HttpConfigurable;
import consulo.ide.webService.WebServiceApi;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author stathik
 * @since 8:57:19 PM May 22, 2003
 */
public class ErrorReportSender {
  private ErrorReportSender() {
  }

  static class SendTask {
    private final Project myProject;
    private String myLogin;
    private ErrorBean errorBean;

    public SendTask(final Project project, ErrorBean errorBean) {
      myProject = project;
      this.errorBean = errorBean;
    }

    public void setCredentials(String login) {
      myLogin = login;
    }

    public void sendReport(final Consumer<Integer> callback, final Consumer<Exception> errback) {
      Task.Backgroundable task = new Task.Backgroundable(myProject, DiagnosticBundle.message("title.submitting.error.report")) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          try {
            HttpConfigurable.getInstance().prepareURL(WebServiceApi.MAIN.buildUrl());

            if (!StringUtil.isEmpty(myLogin)) {
              int threadId = sendAndHandleResult(myLogin, errorBean);
              callback.consume(threadId);
            }
          }
          catch (Exception ex) {
            errback.consume(ex);
          }
        }
      };
      if (myProject == null) {
        task.run(new EmptyProgressIndicator());
      }
      else {
        ProgressManager.getInstance().run(task);
      }
    }
  }

  public static int sendAndHandleResult(String login, ErrorBean error) throws IOException, NoSuchEAPUserException, UpdateAvailableException {
    String reply = doPost(WebServiceApi.ERROR_REPORTER_API.buildUrl("create"), error);

    Map<String, String> map = new Gson().fromJson(reply, new TypeToken<Map<String, String>>() {
    }.getType());

    if ("unauthorized".equals(reply)) {
      throw new NoSuchEAPUserException(login);
    }

    if (reply.startsWith("update ")) {
      throw new UpdateAvailableException(reply.substring(7));
    }

    if (reply.startsWith("message ")) {
      throw new InternalEAPException(reply.substring(8));
    }

    return -1;
  }

  private static String doPost(String url, ErrorBean errorBean) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(new Gson().toJson(errorBean), ContentType.APPLICATION_JSON));

    return httpClient.execute(post, response -> {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new InternalEAPException(DiagnosticBundle.message("error.http.result.code", statusCode));
      }

      return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    });
  }

  public static void sendError(Project project, String login, ErrorBean error, Consumer<Integer> callback, Consumer<Exception> errback) {
    SendTask sendTask = new SendTask(project, error);
    sendTask.setCredentials(login);
    sendTask.sendReport(callback, errback);
  }
}
