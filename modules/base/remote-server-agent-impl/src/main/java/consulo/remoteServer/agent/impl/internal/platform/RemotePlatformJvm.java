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

import consulo.platform.CpuArchitecture;
import consulo.platform.JavaVersion;
import consulo.platform.PlatformJvm;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Stub JVM implementation for remote agent (which is a Rust binary, not a JVM).
 *
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemotePlatformJvm implements PlatformJvm {
    private final CpuArchitecture myArch;

    public RemotePlatformJvm(String archString) {
        myArch = CpuArchitecture.of(archString);
    }

    @Override
    public JavaVersion version() {
        return JavaVersion.compose(25);
    }

    @Override
    public String rawVersion() {
        return "n/a";
    }

    @Override
    public String vendor() {
        return "n/a";
    }

    @Override
    public String name() {
        return "Remote Agent (Rust)";
    }

    @Nullable
    @Override
    public String getRuntimeProperty(String key) {
        return null;
    }

    @Override
    public Map<String, String> getRuntimeProperties() {
        return Map.of();
    }

    @Override
    public CpuArchitecture arch() {
        return myArch;
    }
}
