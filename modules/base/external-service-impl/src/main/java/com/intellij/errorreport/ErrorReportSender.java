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
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.net.HttpConfigurable;
import consulo.external.api.ErrorReportBean;
import consulo.externalService.AuthorizationFailedException;
import consulo.externalService.UpdateAvailableException;
import consulo.externalService.impl.WebServiceApi;
import consulo.externalService.impl.WebServiceApiSender;
import consulo.externalService.impl.WebServiceException;
import consulo.platform.base.localize.DiagnosticLocalize;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author stathik
 * @since 8:57:19 PM May 22, 2003
 */
public class ErrorReportSender {
  private static enum ResultType {
    OK,
    PLATFORM_UPDATE_REQUIRED,
    PLUGIN_UPDATE_REQUIRED,
    BAD_REPORT,
    BAD_OAUTHK_KEY
  }

  private ErrorReportSender() {
  }

  public static void sendReport(Project project, ErrorReportBean errorBean, long assignUserId, Consumer<String> callback,  Consumer<Exception> errback) {
    Task.Backgroundable.queue(project, DiagnosticLocalize.titleSubmittingErrorReport().get(), indicator -> {
      try {
        HttpConfigurable.getInstance().prepareURL(WebServiceApi.MAIN.buildUrl());

        String id = sendAndHandleResult(errorBean, assignUserId);

        callback.accept(id);
      }
      catch (Exception ex) {
        errback.accept(ex);
      }
    });
  }

  @Nonnull
  public static String sendAndHandleResult(@Nonnull ErrorReportBean error, long assignUserId) throws IOException, AuthorizationFailedException, UpdateAvailableException {
    Map<String, String> map = WebServiceApiSender.doPost(WebServiceApi.ERROR_REPORTER_API, "create?assignUserId=" + assignUserId, error, new TypeToken<Map<String, String>>() {
    }.getType());

    assert map != null;
    
    String type = map.get("type");
    if (type == null) {
      throw new WebServiceException("No 'type' data", 500);
    }

    try {
      ResultType resultType = ResultType.valueOf(type);
      switch (resultType) {
        case OK:
          String id = map.get("id");
          if (id == null) {
            throw new WebServiceException("No 'id' data", 500);
          }
          return id;
        case BAD_OAUTHK_KEY:
          throw new AuthorizationFailedException();
        case PLATFORM_UPDATE_REQUIRED:
        case PLUGIN_UPDATE_REQUIRED:
          throw new UpdateAvailableException();
        case BAD_REPORT:
        default:
          throw new WebServiceException("Unknown error " + resultType, 500);
      }
    }
    catch (IllegalArgumentException e) {
      throw new WebServiceException(e.getMessage(), 500);
    }
  }
}
