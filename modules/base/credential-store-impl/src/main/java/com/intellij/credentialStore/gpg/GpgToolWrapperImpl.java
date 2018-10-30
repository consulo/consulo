/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.credentialStore.gpg;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessIOExecutorService;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.util.io.FileUtilRt;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author VISTALL
 * @since 2018-10-28
 */
public class GpgToolWrapperImpl implements GpgToolWrapper {
  private String gpgPath;
  private int timeoutInMilliseconds;

  public GpgToolWrapperImpl() {
    this("gpg", 5000);
  }

  public GpgToolWrapperImpl(String gpgPath, int timeoutInMilliseconds) {
    this.gpgPath = gpgPath;
    this.timeoutInMilliseconds = timeoutInMilliseconds;
  }

  // http://git.gnupg.org/cgi-bin/gitweb.cgi?p=gnupg.git;a=blob_plain;f=doc/DETAILS
  // https://delim.co/#
  @Nonnull
  @Override
  public String listSecretKeys() {
    GeneralCommandLine commandLine = createCommandLine();
    commandLine.addParameter("--list-secret-keys");
    return doExecute(commandLine);
  }

  @Override
  public byte[] encrypt(byte[] data, String recipient) {
    GeneralCommandLine commandLine = createCommandLine();
    commandLine.addParameter("--encrypt");
    // key id is stored, --hidden-recipient doesn't make sense because if it will be used, key id should be specified somewhere else,
    // if it will be stored in master key metadata - for what to hide it then?
    commandLine.addParameter("--recipient");
    commandLine.addParameter(recipient);
    return doEncryptOrDecrypt(commandLine, data);
  }

  @Override
  public byte[] decrypt(byte[] data) {
    GeneralCommandLine commandLine = createCommandLine();
    commandLine.addParameter("--decrypt");
    return doEncryptOrDecrypt(commandLine, data);
  }

  private byte[] doEncryptOrDecrypt(GeneralCommandLine commandLine, byte[] data) {
    try {
      Process process = commandLine.createProcess();
      ByteArrayOutputStream result = new ByteArrayOutputStream();

      Future<?> future = ProcessIOExecutorService.INSTANCE.submit((Runnable)() -> {
        try {
          try (OutputStream stream = process.getOutputStream()) {
            stream.write(data);
          }
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }

        try (InputStream stream = process.getInputStream()) {
          FileUtilRt.copy(stream, result);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      // user need time to input passphrase/pin if need
      future.get(3, TimeUnit.MINUTES);
      process.waitFor(3, TimeUnit.MINUTES);
      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new RuntimeException("Cannot execute " + gpgPath + ": exit code " + exitCode + ", error output: " + new String(result.toByteArray(), StandardCharsets.UTF_8));
      }
      return result.toByteArray();
    }
    catch (ExecutionException | InterruptedException | java.util.concurrent.ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public String version() {
    GeneralCommandLine commandLine = createCommandLine();
    commandLine.addParameter("--version");
    return doExecute(commandLine);
  }

  private String doExecute(GeneralCommandLine commandLine) {
    try {
      ProcessOutput processOutput = ExecUtil.execAndGetOutput(commandLine, timeoutInMilliseconds);
      int exitCode = processOutput.getExitCode();
      if (exitCode != 0) {
        throw new RuntimeException("Cannot execute " + gpgPath + ": exit code " + exitCode + ", error output: " + processOutput.getStderr());
      }
      return processOutput.getStdout();
    }
    catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private GeneralCommandLine createCommandLine() {
    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(gpgPath);
    commandLine.addParameter("--with-colons");
    commandLine.addParameter("--no-tty");
    commandLine.addParameter("--yes");
    commandLine.addParameter("--quiet");
    commandLine.addParameter("--fixed-list-mode");
    commandLine.addParameter("--display-charset");
    commandLine.addParameter("utf-8");
    commandLine.addParameter("--no-greeting");
    return commandLine;
  }
}
