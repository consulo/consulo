// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.xdebugger.attach.osHandlers;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;
import com.intellij.xdebugger.attach.EnvironmentAwareHost;
import com.intellij.xdebugger.attach.LocalAttachHost;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;

/**
 * this class allows to obtain os-specific data from {@link EnvironmentAwareHost}
 */
public abstract class AttachOSHandler {

  private static final Logger LOGGER = Logger.getInstance(AttachOSHandler.class);
  private static final GeneralCommandLine ENV_COMMAND_LINE = new GeneralCommandLine("env");

  private Map<String, String> myEnvironment;
  @Nonnull
  private final OSType myOSType;

  @Nonnull
  protected final EnvironmentAwareHost myHost;

  public AttachOSHandler(@Nonnull EnvironmentAwareHost host, @Nonnull final OSType osType) {
    myHost = host;
    myOSType = osType;
  }

  @Nonnull
  public OSType getOSType() {
    return myOSType;
  }

  @Nullable
  protected String getenv(String name) throws Exception {
    if (myHost instanceof LocalAttachHost) {
      return EnvironmentUtil.getValue(name);
    }

    if (myEnvironment == null) {
      myEnvironment = EnvironmentUtil.parseEnv(myHost.getProcessOutput(ENV_COMMAND_LINE).getStdout().split("\n"));
    }

    return myEnvironment.get(name);
  }

  @Nonnull
  public static AttachOSHandler getAttachOsHandler(@Nonnull EnvironmentAwareHost host) {

    try {
      final OSType osType = computeOsType(host);

      if (osType == OSType.LINUX) {
        return new LinuxAttachOSHandler(host);
      }

      if (osType == OSType.MACOSX) {
        return new MacAttachOSHandler(host);
      }
    }
    catch (ExecutionException e) {
      LOGGER.warn("Error while obtaining host operating system", e);
    }

    return new GenericAttachOSHandler(host);
  }

  @Nonnull
  private static OSType localComputeOsType() {
    if (SystemInfo.isLinux) {
      return OSType.LINUX;
    }

    if (SystemInfo.isMac) {
      return OSType.MACOSX;
    }

    if (SystemInfo.isWindows) {
      return OSType.WINDOWS;
    }

    return OSType.UNKNOWN;
  }

  @Nonnull
  private static OSType computeOsType(@Nonnull EnvironmentAwareHost host) throws ExecutionException {
    if (host instanceof LocalAttachHost) {
      return localComputeOsType();
    }

    try {
      GeneralCommandLine getOsCommandLine = new GeneralCommandLine("uname", "-s");
      final String osString = host.getProcessOutput(getOsCommandLine).getStdout().trim();

      OSType osType;

      //TODO [viuginick] handle remote windows
      switch (osString) {
        case "Linux":
          osType = OSType.LINUX;
          break;
        case "Darwin":
          osType = OSType.MACOSX;
          break;
        default:
          osType = OSType.UNKNOWN;
          break;
      }
      return osType;
    }
    catch (ExecutionException ex) {
      throw new ExecutionException("Error while calculating the remote operating system", ex);
    }
  }

  public enum OSType {
    LINUX,
    MACOSX,
    WINDOWS,
    UNKNOWN
  }
}
