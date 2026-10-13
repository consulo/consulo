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
package consulo.remoteServer.agent.impl.internal.platform;

import consulo.remoteServer.agent.impl.internal.RemoteAgentConnection;
import consulo.platform.*;
import consulo.remoteServer.agent.impl.internal.protocol.SystemInfo;
import consulo.remoteServer.agent.impl.internal.protocol.UserInfo;
import consulo.util.dataholder.UserDataHolderBase;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.Map;

/**
 * Platform implementation for a remote machine connected via the remote agent.
 * <p>
 * This is NOT a singleton - each remote connection produces its own Platform instance.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemotePlatform extends UserDataHolderBase implements Platform {
    private final RemoteAgentConnection myConnection;
    private final String myId;
    private final String myName;
    private final RemotePlatformOperatingSystem myOperatingSystem;
    private final RemotePlatformFileSystem myFileSystem;
    private final RemotePlatformJvm myJvm;
    private final RemotePlatformUser myUser;
    private final Charset myConsoleCharset;

    public RemotePlatform(RemoteAgentConnection connection, FileSystem nioFileSystem) {
        myConnection = connection;

        SystemInfo systemInfo = connection.getSystemInfo();
        UserInfo userInfo = connection.getUserInfo();
        Map<String, String> envVariables = connection.getEnvVariables();

        myId = connection.getPlatformId();
        myName = "Remote: " + systemInfo.getHostname();

        myOperatingSystem = new RemotePlatformOperatingSystem(connection, systemInfo, Collections.unmodifiableMap(envVariables));
        myFileSystem = new RemotePlatformFileSystem(myOperatingSystem, nioFileSystem);
        myJvm = new RemotePlatformJvm(systemInfo.getArch());
        myUser = new RemotePlatformUser(userInfo, nioFileSystem);
        myConsoleCharset = findCharset(systemInfo.getConsoleEncoding());
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public PlatformFileSystem fs() {
        return myFileSystem;
    }

    @Override
    public PlatformOperatingSystem os() {
        return myOperatingSystem;
    }

    @Override
    public PlatformJvm jvm() {
        return myJvm;
    }

    @Override
    public PlatformUser user() {
        return myUser;
    }

    @Override
    public void openInBrowser(URL url) {
        // Remote platform has no browser
    }

    @Override
    public void openFileInFileManager(File file, consulo.ui.UIAccess uiAccess) {
        // Remote platform has no file manager UI
    }

    @Override
    public void openDirectoryInFileManager(File file, consulo.ui.UIAccess uiAccess) {
        // Remote platform has no file manager UI
    }

    public RemoteAgentConnection getConnection() {
        return myConnection;
    }

    public Charset getConsoleCharset() {
        return myConsoleCharset;
    }

    private static Charset findCharset(String name) {
        try {
            return Charset.forName(name);
        }
        catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }
}
