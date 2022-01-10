/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.util.net;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import consulo.net.util.NetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

public class NetUtils {
  private NetUtils() {
  }

  public static InetSocketAddress loopbackSocketAddress() throws IOException {
    return NetUtil.loopbackSocketAddress();
  }

  public static InetSocketAddress loopbackSocketAddress(int port) throws IOException {
    return NetUtil.loopbackSocketAddress(port);
  }

  public static boolean canConnectToSocket(String host, int port) {
    return NetUtil.canConnectToSocket(host, port);
  }

  public static boolean canConnectToSocketOpenedByJavaProcess(String host, int port) {
    return NetUtil.canConnectToRemoteSocket(host, port);
  }

  public static InetAddress getLoopbackAddress() {
    return NetUtil.getLoopbackAddress();
  }

  public static boolean isLocalhost(@Nonnull String host) {
    return NetUtil.isLocalhost(host);
  }

  public static boolean canConnectToRemoteSocket(String host, int port) {
    return NetUtil.canConnectToRemoteSocket(host, port);
  }

  public static int findAvailableSocketPort() throws IOException {
    return NetUtil.findAvailableSocketPort();
  }

  public static int tryToFindAvailableSocketPort(int defaultPort) {
    return NetUtil.tryToFindAvailableSocketPort(defaultPort);
  }

  public static int tryToFindAvailableSocketPort() {
    return NetUtil.tryToFindAvailableSocketPort();
  }

  public static int[] findAvailableSocketPorts(int capacity) throws IOException {
    return NetUtil.findAvailableSocketPorts(capacity);
  }

  public static String getLocalHostString() {
    return NetUtil.getLocalHostString();
  }

  /**
   * @param indicator           Progress indicator.
   * @param inputStream         source stream
   * @param outputStream        destination stream
   * @param expectedContentSize expected content size, used in progress indicator. can be -1.
   * @return bytes copied
   * @throws IOException                                            if IO error occur
   * @throws com.intellij.openapi.progress.ProcessCanceledException if process was canceled.
   */

  // TODO [VISTALL] move to another util
  public static int copyStreamContent(@Nullable ProgressIndicator indicator, @Nonnull InputStream inputStream, @Nonnull OutputStream outputStream, int expectedContentSize)
          throws IOException, ProcessCanceledException {
    return copyStreamContent(indicator, null, inputStream, outputStream, expectedContentSize);
  }

  /**
   * @param indicator           Progress indicator.
   * @param digest              MessageDigest for updating while dowloading
   * @param inputStream         source stream
   * @param outputStream        destination stream
   * @param expectedContentSize expected content size, used in progress indicator. can be -1.
   * @return bytes copied
   * @throws IOException                                            if IO error occur
   * @throws com.intellij.openapi.progress.ProcessCanceledException if process was canceled.
   */

  // TODO [VISTALL] move to another util
  public static int copyStreamContent(@Nullable ProgressIndicator indicator,
                                      @Nullable MessageDigest digest,
                                      @Nonnull InputStream inputStream,
                                      @Nonnull OutputStream outputStream,
                                      int expectedContentSize) throws IOException, ProcessCanceledException {
    if (indicator != null) {
      indicator.checkCanceled();
      indicator.setIndeterminate(expectedContentSize < 0);
    }

    final byte[] buffer = new byte[8 * 1024];
    int count;
    int total = 0;
    while ((count = inputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, count);
      if(digest != null) {
        digest.update(buffer, 0, count);
      }
      
      total += count;

      if (indicator != null) {
        indicator.checkCanceled();

        if (expectedContentSize > 0) {
          indicator.setFraction((double)total / expectedContentSize);
        }
      }
    }

    if (indicator != null) {
      indicator.checkCanceled();
    }

    return total;
  }
}
