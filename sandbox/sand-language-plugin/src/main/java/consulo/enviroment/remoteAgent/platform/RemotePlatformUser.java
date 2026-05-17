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

import consulo.enviroment.remoteAgent.protocol.UserInfo;
import consulo.platform.PlatformUser;

import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemotePlatformUser implements PlatformUser {
    private final UserInfo myUserInfo;
    private final FileSystem myNioFileSystem;

    public RemotePlatformUser(UserInfo userInfo, FileSystem nioFileSystem) {
        myUserInfo = userInfo;
        myNioFileSystem = nioFileSystem;
    }

    @Override
    public boolean superUser() {
        return "root".equals(myUserInfo.getUserName());
    }

    @Override
    public String name() {
        return myUserInfo.getUserName();
    }

    @Override
    public Path homePath() {
        return myNioFileSystem.getPath(myUserInfo.getHomePath());
    }
}
