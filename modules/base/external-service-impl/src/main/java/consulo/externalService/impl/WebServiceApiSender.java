/*
 * Copyright 2013-2020 consulo.io
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
package consulo.externalService.impl;

import com.google.gson.Gson;
import com.intellij.openapi.components.ServiceManager;
import consulo.externalService.*;
import consulo.platform.base.localize.DiagnosticLocalize;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ThreeState;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
public class WebServiceApiSender {
  private static final Gson ourGson = new Gson();

  @Nullable
  public static <T> T doGet(WebServiceApi serviceApi, String url, Class<T> beanClass) throws IOException {
    return doGet(serviceApi, url, Map.of(), beanClass);
  }

  @Nullable
  public static <T> T doGet(WebServiceApi serviceApi, String url, Map<String, String> parameters, Type beanClass) throws IOException {
    byte[] bytes = doGetBytes(serviceApi, url, parameters);
    if (bytes == null) {
      return null;
    }
    return ourGson.fromJson(new String(bytes, StandardCharsets.UTF_8), beanClass);
  }

  @Nullable
  public static byte[] doGetBytes(WebServiceApi serviceApi, String url, Map<String, String> parameters) throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet request;
      try {
        URIBuilder urlBuilder = new URIBuilder(serviceApi.buildUrl(url));
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
          urlBuilder.addParameter(entry.getKey(), entry.getValue());
        }

        request = new HttpGet(urlBuilder.build());

      }
      catch (URISyntaxException e1) {
        throw new RuntimeException(e1);
      }

      String oAuthKey = findOAuthKey(serviceApi);

      if (oAuthKey != null) {
        request.addHeader("Authorization", "Bearer " + oAuthKey);
      }

      return httpClient.execute(request, response -> {
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
          case HttpURLConnection.HTTP_OK:
            return EntityUtils.toByteArray(response.getEntity());
          case HttpURLConnection.HTTP_UNAUTHORIZED:
            throw new AuthorizationFailedException();
          case HttpURLConnection.HTTP_NOT_MODIFIED:
            throw new NotModifiedException();
          case HttpURLConnection.HTTP_NOT_FOUND:
            throw new NotFoundException();
          case HttpURLConnection.HTTP_NO_CONTENT:
            throw new NoContentException();
          default:
            throw new WebServiceException(DiagnosticLocalize.errorHttpResultCode(statusCode).get(), statusCode);
        }
      });
    }
  }

  @Nullable
  public static <T> T doPost(WebServiceApi serviceApi, String url, Object bean, Type resultType) throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPost request = new HttpPost(serviceApi.buildUrl(url));
      request.setHeader("Content-Type", "application/json");
      request.setEntity(new StringEntity(new Gson().toJson(bean), ContentType.APPLICATION_JSON));

      String oAuthKey = findOAuthKey(serviceApi);

      if (oAuthKey != null) {
        request.addHeader("Authorization", "Bearer " + oAuthKey);
      }

      return httpClient.execute(request, response -> {
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
          case HttpURLConnection.HTTP_OK:
            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return ourGson.fromJson(json, resultType);
          case HttpURLConnection.HTTP_UNAUTHORIZED:
            throw new AuthorizationFailedException();
          default:
            throw new WebServiceException(DiagnosticLocalize.errorHttpResultCode(statusCode).get(), statusCode);
        }
      });
    }
  }

  private static String findOAuthKey(WebServiceApi api) {
    ExternalService service = map(api);
    if (service == null) {
      return null;
    }

    ExternalServiceConfigurationImpl externalServiceConfiguration = (ExternalServiceConfigurationImpl)ServiceManager.getService(ExternalServiceConfiguration.class);

    ThreeState state = externalServiceConfiguration.getState(service);
    switch (state) {
      case NO:
        throw new UnsupportedOperationException("Can't send any data for api: " + api);
      case YES:
        return ObjectUtil.notNull(externalServiceConfiguration.getOAuthKey(), "bad-key");
      case UNSURE:
      default:
        return null;
    }
  }

  @Nullable
  private static ExternalService map(WebServiceApi api) {
    switch (api) {
      case ERROR_REPORTER_API:
        return ExternalService.ERROR_REPORTING;
      case STATISTICS_API:
        return ExternalService.STATISTICS;
      case DEVELOPER_API:
        return ExternalService.DEVELOPER_LIST;
      case STORAGE_API:
        return ExternalService.STORAGE;
      case REPOSITORY_API:
        return null;
      default:
        throw new IllegalArgumentException(api.name() + " not supported");
    }
  }
}
