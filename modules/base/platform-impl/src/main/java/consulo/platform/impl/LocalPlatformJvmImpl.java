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
package consulo.platform.impl;

import consulo.platform.JavaVersion;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * @author VISTALL
 * @since 2026-05-09
 */
public class LocalPlatformJvmImpl extends PlatformJvmImpl {
    LocalPlatformJvmImpl(Map<String, String> jvmProperties) {
        super(jvmProperties);
    }

    @Override
    protected JavaVersion parseJavaVersion(String rawJavaVersion, String javaRuntimeVersion) {
        JavaVersion version = JavaVersion.tryParse(rawJavaVersion);

        if (version == null) {
            version = JavaVersion.tryParse(javaRuntimeVersion);
        }

        if (version == null) {
            version = rtVersion();
        }

        return version;
    }

    private static  JavaVersion rtVersion() {
        Runtime.Version version = Runtime.version();
        int feature = version.feature();
        int minor = version.interim();
        int security = version.update();
        Optional<Integer> buildOpt = version.build();
        int build = buildOpt.orElse(0);
        Optional<String> preOpt = version.pre();
        boolean ea = preOpt.isPresent();
        return JavaVersion.compose(feature, minor, security, build, ea);
    }
}
