/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.builtinWebServer.impl.xml;

import consulo.annotation.component.ServiceImpl;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.impl.http.HttpRequestImpl;
import consulo.builtinWebServer.xml.XmlRpcServer;
import consulo.http.HTTPMethod;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.server.XmlRpcStreamServer;
import org.xml.sax.SAXParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Singleton
@ServiceImpl
public class XmlRpcServerImpl implements XmlRpcServer {
  private static class ApacheXmlRpcImpl extends XmlRpcStreamServer {
    @Override
    public XmlRpcRequest getRequest(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {
      return super.getRequest(pConfig, pStream);
    }

    @Override
    public void writeResponse(XmlRpcStreamRequestConfig pConfig, OutputStream pStream, Object pResult) throws XmlRpcException {
      super.writeResponse(pConfig, pStream, pResult);
    }
  }

  private static final Logger LOG = Logger.getInstance(XmlRpcServerImpl.class);

  private final Map<String, Object> handlerMapping = new HashMap<>();

  private final XmlRpcHttpRequestConfigImpl myXmlRpcConfig = new XmlRpcHttpRequestConfigImpl();

  private final ApacheXmlRpcImpl myApacheXmlRpc = new ApacheXmlRpcImpl();

  @Inject
  public XmlRpcServerImpl() {
    myXmlRpcConfig.setEncoding("UTF-8");
  }

  @Override
  public boolean hasHandler(String name) {
    return handlerMapping.containsKey(name);
  }

  @Override
  public void addHandler(String name, Object handler) {
    handlerMapping.put(name, handler);
  }

  @Override
  public void removeHandler(String name) {
    handlerMapping.remove(name);
  }

  @Nonnull
  @Override
  public HttpResponse process(@Nonnull String path, @Nonnull HttpRequest request, @Nullable Map<String, Object> handlers) {
    if (!(path.isEmpty() || (path.length() == 1 && path.charAt(0) == '/') || path.equalsIgnoreCase("/rpc2"))) {
      return HttpResponse.notFound();
    }

    if (request.method() != HTTPMethod.POST) {
      return HttpResponse.notFound();
    }

    FullHttpRequest fullHttpRequest = ((HttpRequestImpl)request).getFullHttpRequest();
    ByteBuf content = fullHttpRequest.content();
    if (content.readableBytes() == 0) {
      return HttpResponse.badRequest();
    }

    byte[] result;
    try (ByteBufInputStream in = new ByteBufInputStream(content)) {
      XmlRpcRequest xmlRpcServerRequest = myApacheXmlRpc.getRequest(myXmlRpcConfig, in);
      if (StringUtil.isEmpty(xmlRpcServerRequest.getMethodName())) {
        LOG.warn("method name empty");
        return HttpResponse.badRequest();
      }

      Object response = invokeHandler(getHandler(xmlRpcServerRequest.getMethodName(), handlers == null ? handlerMapping : handlers), xmlRpcServerRequest);

      ByteArrayOutputStream stream = new ByteArrayOutputStream();

      myApacheXmlRpc.writeResponse(myXmlRpcConfig, stream, response);

      result = stream.toByteArray();
    }
    catch (SAXParseException e) {
      LOG.warn(e);
      return HttpResponse.badRequest();
    }
    catch (Throwable e) {
      request.terminate();
      LOG.error(e);
      return HttpResponse.badRequest();
    }

    return HttpResponse.ok("text/xml", result);
  }

  private static Object getHandler(@Nonnull String methodName, @Nonnull Map<String, Object> handlers) {
    Object handler = null;
    String handlerName = null;
    int dot = methodName.lastIndexOf('.');
    if (dot > -1) {
      handlerName = methodName.substring(0, dot);
      handler = handlers.get(handlerName);
    }

    if (handler != null) {
      return handler;
    }

    if (dot > -1) {
      throw new IllegalStateException("RPC handler object \"" + handlerName + "\" not found");
    }
    else {
      throw new IllegalStateException("RPC handler object not found for \"" + methodName);
    }
  }

  @Nullable
  private static Object invokeHandler(@Nonnull Object handler, XmlRpcRequest request) throws Throwable {
    if (handler instanceof XmlRpcHandler) {
      return handler;
    }
    else {
      Object[] params = new Object[request.getParameterCount()];
      for (int i = 0; i < request.getParameterCount(); i++) {
        params[i] = request.getParameter(i);
      }
      return invoke(handler, request.getMethodName(), params);
    }
  }

  private static Object invoke(Object target, String methodName, Object[] params) throws Throwable {
    Class<?> targetClass = (target instanceof Class) ? (Class)target : target.getClass();
    Class[] argClasses = null;
    Object[] argValues = null;
    if (params != null) {
      argClasses = new Class[params.length];
      argValues = new Object[params.length];
      for (int i = 0; i < params.length; i++) {
        argValues[i] = params[i];
        if (argValues[i] instanceof Integer) {
          argClasses[i] = Integer.TYPE;
        }
        else if (argValues[i] instanceof Double) {
          argClasses[i] = Double.TYPE;
        }
        else if (argValues[i] instanceof Boolean) {
          argClasses[i] = Boolean.TYPE;
        }
        else {
          argClasses[i] = argValues[i].getClass();
        }
      }
    }

    Method method;
    int dot = methodName.lastIndexOf('.');
    if (dot > -1 && dot + 1 < methodName.length()) {
      methodName = methodName.substring(dot + 1);
    }
    method = targetClass.getMethod(methodName, argClasses);

    // Our policy is to make all public methods callable except the ones defined in java.lang.Object
    if (method.getDeclaringClass() == Object.class) {
      throw new XmlRpcException(0, "Invoker can't call methods defined in java.lang.Object");
    }

    Object returnValue = method.invoke(target, argValues);
    if (returnValue == null && method.getReturnType() == Void.TYPE) {
      // Not supported by the spec.
      throw new IllegalArgumentException("void return types for handler methods not supported, " + methodName);
    }
    return returnValue;
  }
}