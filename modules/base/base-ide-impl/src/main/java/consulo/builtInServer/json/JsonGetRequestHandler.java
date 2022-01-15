/*
 * Copyright 2013-2016 consulo.io
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
package consulo.builtInServer.json;

import com.intellij.util.ExceptionUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import javax.annotation.Nonnull;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 27.10.2015
 */
public abstract class JsonGetRequestHandler extends JsonBaseRequestHandler {

  protected JsonGetRequestHandler(@Nonnull String apiUrl) {
    super(apiUrl);
  }

  @Nonnull
  public abstract JsonResponse handle();

  @Nonnull
  @Override
  protected HttpMethod getMethod() {
    return HttpMethod.GET;
  }

  @Override
  public boolean process(@Nonnull QueryStringDecoder urlDecoder, @Nonnull FullHttpRequest request, @Nonnull ChannelHandlerContext context) throws IOException {
    Object handle = null;
    try {
      handle = handle();
    }
    catch (Exception e) {
      handle = JsonResponse.asError(ExceptionUtil.getThrowableText(e));
    }
    return writeResponse(handle, request, context);
  }
}
