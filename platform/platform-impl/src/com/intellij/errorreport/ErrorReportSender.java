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
import com.intellij.errorreport.error.AuthorizationFailedException;
import com.intellij.errorreport.error.UpdateAvailableException;
import com.intellij.errorreport.error.WebServiceException;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.net.HttpConfigurable;
import consulo.ide.webService.WebServiceApi;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author stathik
 * @since 8:57:19 PM May 22, 2003
 */
public class ErrorReportSender {
  private static enum ResultType {
    OK,
    PLATFORM_UPDATE_REQUIRED,
    PLUGIN_UPDATE_REQUIRED,
    BAD_REPORT
  }

  private ErrorReportSender() {
  }

  public static void sendReport(Project project,
                                String logic,
                                ErrorBean errorBean,
                                final java.util.function.Consumer<String> callback,
                                final Consumer<Exception> errback) {
    Task.Backgroundable.queue(project, DiagnosticBundle.message("title.submitting.error.report"), indicator -> {
      try {
        HttpConfigurable.getInstance().prepareURL(WebServiceApi.MAIN.buildUrl());

        String id = sendAndHandleResult(logic, errorBean);

        callback.accept(id);
      }
      catch (Exception ex) {
        errback.consume(ex);
      }
    });
  }

  public static String sendAndHandleResult(String login, ErrorBean error) throws IOException, AuthorizationFailedException, UpdateAvailableException {
    String reply = doPost(WebServiceApi.ERROR_REPORTER_API.buildUrl("create"), error);

    Map<String, String> map = new Gson().fromJson(reply, new TypeToken<Map<String, String>>() {
    }.getType());

    String type = map.get("type");
    if (type == null) {
      throw new WebServiceException();
    }

    //TODO [VISTALL]  throw new AuthorizationFailedException(login);

    try {
      ResultType resultType = ResultType.valueOf(type);
      switch (resultType) {
        case OK:
          String id = map.get("id");
          if (id == null) {
            throw new WebServiceException();
          }
          return id;
        case PLATFORM_UPDATE_REQUIRED:
        case PLUGIN_UPDATE_REQUIRED:
          throw new UpdateAvailableException(reply.substring(7));
        case BAD_REPORT:
        default:
          throw new WebServiceException();
      }
    }
    catch (IllegalArgumentException e) {
      throw new WebServiceException();
    }
  }

  private static String doPost(String url, ErrorBean errorBean) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost post = new HttpPost(url);
    post.setEntity(new StringEntity(new Gson().toJson(errorBean), ContentType.APPLICATION_JSON));

    return httpClient.execute(post, response -> {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpURLConnection.HTTP_OK) {
        throw new WebServiceException(DiagnosticBundle.message("error.http.result.code", statusCode));
      }

      return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    });
  }
}
