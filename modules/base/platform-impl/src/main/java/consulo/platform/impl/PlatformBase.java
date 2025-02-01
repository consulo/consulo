/*
 * Copyright 2013-2017 consulo.io
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

import consulo.platform.*;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public abstract class PlatformBase extends UserDataHolderBase implements Platform {
    private final PlatformFileSystem myFileSystem;
    private final PlatformOperatingSystem myOperatingSystem;
    private final PlatformJvm myJvm;
    private final PlatformUser myUser;
    private final String myId;
    private final String myName;

    protected PlatformBase(@Nonnull String id, @Nonnull String name, @Nonnull Map<String, String> jvmProperties) {
        myId = id;
        myName = name;
        myOperatingSystem = createOS(jvmProperties);
        myFileSystem = createFS(jvmProperties);
        myJvm = createJVM(jvmProperties);
        myUser = createUser(jvmProperties);
    }

    protected static Map<String, String> getSystemJvmProperties() {
        Properties properties = System.getProperties();
        Map<String, String> map = new LinkedHashMap<>();
        for (Object key : properties.keySet()) {
            if (key instanceof String keyString) {
                map.put(keyString, properties.getProperty(keyString, ""));
            }
        }
        return map;
    }

    @Nonnull
    protected PlatformFileSystem createFS(Map<String, String> jvmProperties) {
        return new PlatformFileSystemImpl(this, jvmProperties);
    }

    @Nonnull
    protected PlatformOperatingSystem createOS(Map<String, String> jvmProperties) {
        String osNameLowered = jvmProperties.get("os.name").toLowerCase(Locale.ROOT);
        if (osNameLowered.startsWith("windows")) {
            return createWindowsOperatingSystem(jvmProperties, System::getenv, System::getenv);
        }

        if (osNameLowered.startsWith("mac")) {
            return createMacOperatingSystem(jvmProperties, System::getenv, System::getenv);
        }

        return new UnixOperationSystemImpl(jvmProperties, System::getenv, System::getenv);
    }

    @Nonnull
    protected MacOperatingSystemImpl createMacOperatingSystem(Map<String, String> jvmProperties,
                                                                      Function<String, String> getEnvFunc,
                                                                      Supplier<Map<String, String>> getEnvsSup) {
        return new LocalMacOperatingSystemImpl(jvmProperties, getEnvFunc, getEnvsSup);
    }

    @Nonnull
    protected WindowsOperatingSystemImpl createWindowsOperatingSystem(Map<String, String> jvmProperties,
                                                                      Function<String, String> getEnvFunc,
                                                                      Supplier<Map<String, String>> getEnvsSup) {
        return new LocalWindowsOperationSystemImpl(jvmProperties, getEnvFunc, getEnvsSup);
    }

    @Nonnull
    protected PlatformJvm createJVM(@Nonnull Map<String, String> jvmProperties) {
        return new PlatformJvmImpl(jvmProperties);
    }

    @Nonnull
    protected PlatformUser createUser(Map<String, String> jvmProperties) {
        return new PlatformUserImpl(jvmProperties);
    }

    @Nonnull
    @Override
    public String getId() {
        return myId;
    }

    @Nonnull
    @Override
    public String getName() {
        return myName;
    }

    @Nonnull
    @Override
    public PlatformJvm jvm() {
        return myJvm;
    }

    @Nonnull
    @Override
    public PlatformFileSystem fs() {
        return myFileSystem;
    }

    @Nonnull
    @Override
    public PlatformOperatingSystem os() {
        return myOperatingSystem;
    }

    @Nonnull
    @Override
    public PlatformUser user() {
        return myUser;
    }
}
