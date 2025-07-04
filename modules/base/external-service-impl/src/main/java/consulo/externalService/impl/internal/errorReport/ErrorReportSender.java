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
package consulo.externalService.impl.internal.errorReport;

import com.google.gson.reflect.TypeToken;
import consulo.application.progress.Task;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.externalService.impl.internal.WebServiceApiSender;
import consulo.externalService.impl.internal.WebServiceException;
import consulo.externalService.impl.internal.repository.AuthorizationFailedException;
import consulo.externalService.impl.internal.repository.UpdateAvailableException;
import consulo.externalService.impl.internal.repository.api.ErrorReportBean;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author stathik
 * @since 2003-05-22
 */
public class ErrorReportSender {
  private static final Logger LOG = Logger.getInstance(ErrorReportSender.class);

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
    Task.Backgroundable.queue(project, ExternalServiceLocalize.titleSubmittingErrorReport().get(), indicator -> {
      try {
        String id = sendAndHandleResult(errorBean, assignUserId);

        callback.accept(id);
      }
      catch (Exception ex) {
        LOG.warn(ex);
        
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
