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
package consulo.ide.webService;

import com.google.gson.Gson;
import com.intellij.diagnostic.DiagnosticBundle;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
public class WebServiceApiSender {
  @Nonnull
  public static String doGet(WebServiceApi serviceApi, String url) throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet post = new HttpGet(serviceApi.buildUrl(url));

      String authKey = WebServicesConfiguration.getInstance().getOAuthKey(serviceApi);
      if (authKey != null) {
        post.addHeader("Authorization", "Bearer " + authKey);
      }
      return httpClient.execute(post, response -> {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
          throw new WebServiceException(DiagnosticBundle.message("error.http.result.code", statusCode), statusCode);
        }

        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      });
    }
  }

  @Nonnull
  public static String doPost(WebServiceApi serviceApi, String url, Object bean) throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpPost post = new HttpPost(serviceApi.buildUrl(url));
      post.setEntity(new StringEntity(new Gson().toJson(bean), ContentType.APPLICATION_JSON));

      String authKey = WebServicesConfiguration.getInstance().getOAuthKey(serviceApi);
      if (authKey != null) {
        post.addHeader("Authorization", "Bearer " + authKey);
      }
      return httpClient.execute(post, response -> {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
          throw new WebServiceException(DiagnosticBundle.message("error.http.result.code", statusCode), statusCode);
        }

        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
      });
    }
  }
}
