// File: GpgToolWrapperImpl.java
package consulo.credentialStorage.impl.internal.gpg;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.process.util.ProcessOutput;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.io.FileUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GpgToolWrapperImpl implements GpgToolWrapper {
    private final String gpgPath;
    private final int timeoutInMilliseconds;

    // Default constructor uses "gpg" and a timeout of 5000ms.
    public GpgToolWrapperImpl() {
        this("gpg", 5000);
    }

    public GpgToolWrapperImpl(String gpgPath, int timeoutInMilliseconds) {
        this.gpgPath = gpgPath;
        this.timeoutInMilliseconds = timeoutInMilliseconds;
    }

    @Override
    public String version() {
        GeneralCommandLine commandLine = createCommandLine();
        commandLine.addParameter("--version");
        try {
            return doExecute(commandLine);
        }
        catch (consulo.process.ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String listSecretKeys() throws consulo.process.ExecutionException {
        GeneralCommandLine commandLine = createCommandLine();
        commandLine.addParameter("--list-secret-keys");
        return doExecute(commandLine);
    }

    @Override
    public byte[] encrypt(byte[] data, String recipient) {
        GeneralCommandLine commandLine = createCommandLineForEncodeOrDecode();
        commandLine.addParameter("--encrypt");
        commandLine.addParameter("--recipient");
        commandLine.addParameter(recipient);
        try {
            return doEncryptOrDecrypt(commandLine, data);
        }
        catch (consulo.process.ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) {
        GeneralCommandLine commandLine = createCommandLineForEncodeOrDecode();
        commandLine.addParameter("--decrypt");
        try {
            return doEncryptOrDecrypt(commandLine, data);
        }
        catch (consulo.process.ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private GeneralCommandLine createCommandLineForEncodeOrDecode() {
        // We disable options that affect string output to avoid errors during encode/decode.
        GeneralCommandLine result = createCommandLine(false);
        result.addParameter("--trust-model");
        result.addParameter("always");
        return result;
    }

    private byte[] doEncryptOrDecrypt(GeneralCommandLine commandLine, byte[] data) throws consulo.process.ExecutionException {
        Process process = commandLine.createProcess();
        BufferExposingByteArrayOutputStream output = new BufferExposingByteArrayOutputStream();
        BufferExposingByteArrayOutputStream errorOutput = new BufferExposingByteArrayOutputStream();

        try {
            CompletableFuture.allOf(
                runAsync(() -> {
                    try (OutputStream os = process.getOutputStream()) {
                        os.write(data);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }),
                runAsync(() -> {
                    try (InputStream is = process.getInputStream()) {
                        FileUtil.copy(is, output);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }),
                runAsync(() -> {
                    try (InputStream is = process.getErrorStream()) {
                        FileUtil.copy(is, errorOutput);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
            ).get(3, TimeUnit.MINUTES);
        }
        catch (ExecutionException e) {
            throw new RuntimeException("Cannot execute " + commandLine.getCommandLineString() + "\nerror output: " +
                new String(errorOutput.toByteArray(), StandardCharsets.UTF_8), e.getCause());
        }
        catch (Exception e) {
            throw new RuntimeException("Error executing command " + commandLine.getCommandLineString(), e);
        }

        // Wait for the process to finish (up to 5 seconds)
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Cannot execute " + gpgPath + ": timeout");
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for process to finish", e);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Cannot execute " + commandLine.getCommandLineString() + "\nexit code " +
                exitCode + ", error output: " +
                new String(errorOutput.toByteArray(), StandardCharsets.UTF_8));
        }

        byte[] resultBytes = output.toByteArray();
        byte[] internalBuffer = output.getInternalBuffer();
        if (resultBytes != internalBuffer) {
            // Ensure that if buffer was copied, clear the original internal buffer.
            Arrays.fill(internalBuffer, (byte) 0);
        }
        return resultBytes;
    }

    private String doExecute(GeneralCommandLine commandLine) throws consulo.process.ExecutionException {
        ProcessOutput processOutput = CapturingProcessUtil.execAndGetOutput(commandLine, timeoutInMilliseconds);
        if (processOutput.getExitCode() != 0) {
            throw new RuntimeException("Cannot execute " + gpgPath + ": exit code " + processOutput.getExitCode() +
                ", error output: " + processOutput.getStderr());
        }
        return processOutput.getStdout();
    }

    private GeneralCommandLine createCommandLine() {
        return createCommandLine(true);
    }

    private GeneralCommandLine createCommandLine(boolean isAddStringOutputRelatedOptions) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setExePath(gpgPath);
        if (isAddStringOutputRelatedOptions) {
            commandLine.addParameter("--with-colons");
            commandLine.addParameter("--fixed-list-mode");
        }
        commandLine.addParameter("--no-tty");
        commandLine.addParameter("--yes");
        commandLine.addParameter("--display-charset");
        commandLine.addParameter("utf-8");
        return commandLine;
    }

    private static CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, AppExecutorUtil.getAppExecutorService());
    }
}
