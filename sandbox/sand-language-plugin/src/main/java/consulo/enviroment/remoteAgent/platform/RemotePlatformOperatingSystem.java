/*
 * Copyright 2013-2026 consulo.io
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
package consulo.enviroment.remoteAgent.platform;

import consulo.enviroment.remoteAgent.RemoteAgentConnection;
import consulo.enviroment.remoteAgent.protocol.SystemInfo;
import consulo.platform.LineSeparator;
import consulo.platform.PlatformOperatingSystem;
import consulo.platform.ProcessInfo;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemotePlatformOperatingSystem implements PlatformOperatingSystem {
    private final RemoteAgentConnection myConnection;
    private final SystemInfo mySystemInfo;
    private final Map<String, String> myEnvironmentVariables;

    private final boolean myIsWindows;
    private final boolean myIsMac;
    private final boolean myIsLinux;
    private final boolean myIsFreeBSD;
    private final boolean myHaiku;

    public RemotePlatformOperatingSystem(RemoteAgentConnection connection,
                                         SystemInfo systemInfo,
                                         Map<String, String> environmentVariables) {
        myConnection = connection;
        mySystemInfo = systemInfo;
        myEnvironmentVariables = environmentVariables;

        String osNameLowered = systemInfo.getOsName().toLowerCase(Locale.ROOT);
        myIsWindows = osNameLowered.startsWith("windows");
        myIsMac = osNameLowered.startsWith("mac");
        myIsLinux = osNameLowered.startsWith("linux");
        myIsFreeBSD = osNameLowered.startsWith("freebsd");
        myHaiku = osNameLowered.startsWith("haiku");
    }

    @Override
    public Collection<ProcessInfo> processes() {
        List<consulo.enviroment.remoteAgent.protocol.ProcessInfo> remoteProcesses =
            myConnection.execute(client -> client.listProcesses());

        List<ProcessInfo> result = new ArrayList<>();
        for (consulo.enviroment.remoteAgent.protocol.ProcessInfo rp : remoteProcesses) {
            String command = rp.getCommand() != null ? rp.getCommand() : "";
            result.add(new ProcessInfo((int) rp.getPid(), command, command, ""));
        }
        return result;
    }

    @Override
    public boolean isWindows() {
        return myIsWindows;
    }

    @Override
    public boolean isMac() {
        return myIsMac;
    }

    @Override
    public boolean isLinux() {
        return myIsLinux;
    }

    @Override
    public boolean isFreeBSD() {
        return myIsFreeBSD;
    }

    @Override
    public boolean isHaiku() {
        return myHaiku;
    }

    @Override
    public boolean isUnix() {
        return !myIsWindows && !myIsMac;
    }

    @Override
    public LineSeparator lineSeparator() {
        return myIsWindows ? LineSeparator.CRLF : LineSeparator.LF;
    }

    @Override
    public String name() {
        return mySystemInfo.getOsName();
    }

    @Override
    public String version() {
        return mySystemInfo.getOsVersion();
    }

    @Override
    public String arch() {
        return mySystemInfo.getArch();
    }

    @Override
    public Map<String, String> environmentVariables() {
        return myEnvironmentVariables;
    }

    @Nullable
    @Override
    public String getEnvironmentVariable(String key) {
        return myEnvironmentVariables.get(key);
    }

    @Override
    public String fileNamePrefix() {
        if (myIsWindows) {
            return "windows";
        }
        if (myIsMac) {
            return "mac";
        }
        return "linux";
    }
}
