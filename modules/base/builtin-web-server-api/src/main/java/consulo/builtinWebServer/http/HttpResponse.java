/*
 * Copyright 2013-2022 consulo.io
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
package consulo.builtinWebServer.http;

import org.jspecify.annotations.Nullable;
import java.net.HttpURLConnection;

/**
 * @author VISTALL
 * @see HttpURLConnection for codes
 * @since 13-Sep-22
 */
public final class HttpResponse {
  
  public static HttpResponse ok() {
    return create(HttpURLConnection.HTTP_OK, null, null);
  }

  
  public static HttpResponse ok(String contentType, byte[] content) {
    return create(HttpURLConnection.HTTP_OK, contentType, content);
  }

  
  public static HttpResponse notFound() {
    return create(HttpURLConnection.HTTP_NOT_FOUND, null, null);
  }

  
  public static HttpResponse badRequest() {
    return create(HttpURLConnection.HTTP_BAD_REQUEST, null, null);
  }

  
  public static HttpResponse create(int code, @Nullable String contentType, @Nullable byte[] content) {
    return new HttpResponse(code, contentType, content);
  }

  private final int myCode;
  private final String myContentType;
  private final byte[] myContent;

  private HttpResponse(int code, @Nullable String contentType, @Nullable byte[] content) {
    myCode = code;
    myContentType = contentType;
    myContent = content;
  }

  public int getCode() {
    return myCode;
  }

  public @Nullable String getContentType() {
    return myContentType;
  }

  public @Nullable byte[] getContent() {
    return myContent;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("HttpResponse{");
    sb.append("myCode=").append(myCode);
    sb.append('}');
    return sb.toString();
  }
}
