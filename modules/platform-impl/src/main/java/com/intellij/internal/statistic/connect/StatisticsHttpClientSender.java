/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.internal.statistic.connect;

import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.util.text.StringUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatisticsHttpClientSender implements StatisticsDataSender {

  @Override
  public void send(@NotNull String url, @NotNull String content) throws StatServiceException {
    //HttpConfigurable.getInstance().prepareURL(url);
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

      HttpPost post = new HttpPost(url);
      List<NameValuePair> pairs = new ArrayList<>(2);
      pairs.add(new BasicNameValuePair("content", content));
      pairs.add(new BasicNameValuePair("uuid", PermanentInstallationID.get()));

      post.setEntity(new UrlEncodedFormEntity(pairs));
      httpClient.execute(post, new ResponseHandler<Object>() {
        @Override
        public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != HttpStatus.SC_OK) {
            throw new StatServiceException("Error during data sending... Code: " + statusCode);
          }

          final Header errors = response.getFirstHeader("errors");
          if (errors != null) {
            final String value = errors.getValue();

            throw new StatServiceException("Error during updating statistics " + (!StringUtil.isEmptyOrSpaces(value) ? " : " + value : ""));
          }
          return null;
        }
      });

    }
    catch (Exception e) {
      throw new StatServiceException("Error during data sending...", e);
    }
  }
}
