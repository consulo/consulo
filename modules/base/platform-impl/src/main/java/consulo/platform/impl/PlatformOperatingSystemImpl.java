/*
 * Copyright 2013-2023 consulo.io
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
package consulo.platform.impl;

import consulo.platform.PlatformOperatingSystem;
import consulo.platform.ProcessInfo;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public class PlatformOperatingSystemImpl implements PlatformOperatingSystem {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformOperatingSystemImpl.class);

    private final String myOSArch;
    protected final String OS_NAME;
    private final String OS_VERSION;

    private final boolean isWindows;
    private final boolean isMac;
    private final boolean isLinux;
    private final boolean isFreeBSD;

    private final Function<String, String> getEnvFunc;
    private final Supplier<Map<String, String>> getEnvsSup;

    private final String myFileNamePrefix;

    public PlatformOperatingSystemImpl(Map<String, String> jvmProperties,
                                       Function<String, String> getEnvFunc,
                                       Supplier<Map<String, String>> getEnvsSup) {
        this.getEnvFunc = getEnvFunc;
        this.getEnvsSup = getEnvsSup;

        myOSArch = StringUtil.notNullize(jvmProperties.get("os.arch"));
        OS_NAME = jvmProperties.get("os.name");
        OS_VERSION = jvmProperties.get("os.version").toLowerCase(Locale.ROOT);
        String osNameLowered = OS_NAME.toLowerCase(Locale.ROOT);
        isWindows = osNameLowered.startsWith("windows");
        isMac = osNameLowered.startsWith("mac");
        isLinux = osNameLowered.startsWith("linux");
        isFreeBSD = osNameLowered.startsWith("freebsd");

        if (isWindows) {
            myFileNamePrefix = "windows";
        }
        else if (isMac) {
            myFileNamePrefix = "mac";
        }
        else {
            myFileNamePrefix = "linux";
        }
    }

    @Override
    public boolean isUnix() {
        return !isWindows && !isMac;
    }

    @Nonnull
    @Override
    public String fileNamePrefix() {
        return myFileNamePrefix;
    }

    public boolean isOsVersionAtLeast(@Nonnull String version) {
        return StringUtil.compareVersionNumbers(OS_VERSION, version) >= 0;
    }

    @Nonnull
    @Override
    public Collection<ProcessInfo> processes() {
        List<ProcessInfo> processInfos = new ArrayList<>();

        ProcessHandle.allProcesses().forEach(it -> {
            long pid = it.pid();

            try {
                ProcessHandle.Info info = it.info();

                Optional<String> commandOptional = info.command();
                // no access to process info
                if (commandOptional.isEmpty() && info.user().isEmpty()) {
                    return;
                }

                String command = commandOptional.orElse("");
                String commandLine = info.commandLine().orElse("");
                String args = StringUtil.join(info.arguments().orElse(ArrayUtil.EMPTY_STRING_ARRAY), " ");

                processInfos.add(new ProcessInfo((int) pid, commandLine, new File(command).getName(), args, command));
            }
            catch (Throwable e) {
                LOG.warn("Failed to get #info() for processId: " + pid, e);
            }
        });

        return processInfos;
    }

    @Override
    public boolean isFreeBSD() {
        return isFreeBSD;
    }

    @Override
    public boolean isWindows() {
        return isWindows;
    }

    @Override
    public boolean isMac() {
        return isMac;
    }

    @Override
    public boolean isLinux() {
        return isLinux;
    }

    @Nonnull
    @Override
    public String name() {
        return OS_NAME;
    }

    @Nonnull
    @Override
    public String version() {
        return OS_VERSION;
    }

    @Nullable
    @Override
    public String getEnvironmentVariable(@Nonnull String key) {
        return getEnvFunc.apply(key);
    }

    @Nonnull
    @Override
    public Map<String, String> environmentVariables() {
        return getEnvsSup.get();
    }

    @Nonnull
    @Override
    public String arch() {
        return myOSArch;
    }
}
