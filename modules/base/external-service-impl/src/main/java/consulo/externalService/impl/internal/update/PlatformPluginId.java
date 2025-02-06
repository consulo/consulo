/*
 * Copyright 2013-2025 consulo.io
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
package consulo.externalService.impl.internal.update;

import consulo.container.plugin.PluginId;
import consulo.platform.CpuArchitecture;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2025-02-05
 */
public enum PlatformPluginId {
    WINDOWS_NO_JRE_ANY("consulo.dist.windows.no.jre", PlatformOperatingSystem::isWindows, null, false),
    WINDOWS_JRE_X86("consulo.dist.windows", PlatformOperatingSystem::isWindows, CpuArchitecture.X86, true),
    WINDOWS_JRE_X64("consulo.dist.windows64", PlatformOperatingSystem::isWindows, CpuArchitecture.X86_64, true),
    WINDOWS_JRE_AARCH64("consulo.dist.windows.aarch64", PlatformOperatingSystem::isWindows, CpuArchitecture.AARCH64, true),

    WINDOWS_NO_JRE_ANY_ZIP("consulo.dist.windows.no.jre.zip", o -> false, null, false),
    WINDOWS_JRE_X86_ZIP("consulo.dist.windows.zip", o -> false, CpuArchitecture.X86, true),
    WINDOWS_JRE_X64_ZIP("consulo.dist.windows64.zip", o -> false, CpuArchitecture.X86_64, true),
    WINDOWS_JRE_AARCH64_ZIP("consulo.dist.windows.aarch64.zip", o -> false, CpuArchitecture.AARCH64, true),

    WINDOWS_JRE_X64_INSTALLER("consulo.dist.windows64.installer", o -> false, CpuArchitecture.X86_64, true),

    MAC_NO_JRE_X64("consulo.dist.mac64.no.jre", PlatformOperatingSystem::isMac, CpuArchitecture.X86_64, false),
    MAC_NO_JRE_AARCH64("consulo.dist.macA64.no.jre", PlatformOperatingSystem::isMac, CpuArchitecture.AARCH64, false),
    MAC_JRE_X64("consulo.dist.mac64", PlatformOperatingSystem::isMac, CpuArchitecture.X86_64, true),
    MAC_JRE_AARCH64("consulo.dist.macA64", PlatformOperatingSystem::isMac, CpuArchitecture.AARCH64, true),

    LINUX_NO_JRE_ANY("consulo.dist.linux.no.jre", PlatformOperatingSystem::isUnix, null, false),
    LINUX_JRE_X86("consulo.dist.linux", PlatformOperatingSystem::isUnix, CpuArchitecture.X86, true),
    LINUX_JRE_X64("consulo.dist.linux64", PlatformOperatingSystem::isUnix, CpuArchitecture.X86_64, true),
    LINUX_JRE_AARCH64("consulo.dist.linux.aarch64", PlatformOperatingSystem::isUnix, CpuArchitecture.AARCH64, true),
    LINUX_JRE_LOONGARCH64("consulo.dist.linux.loongarch64", PlatformOperatingSystem::isUnix, CpuArchitecture.LOONGARCH64, true),
    LINUX_JRE_RISCV64("consulo.dist.linux.riscv64", PlatformOperatingSystem::isUnix, CpuArchitecture.RISCV64, true);

    private final PluginId myPluginId;
    private final Predicate<PlatformOperatingSystem> myOsPredicate;
    private final CpuArchitecture myArchitecture;
    private final boolean myWithJre;

    PlatformPluginId(@Nonnull String id,
                     @Nonnull Predicate<PlatformOperatingSystem> osPredicate,
                     CpuArchitecture architecture,
                     boolean withJre) {
        myOsPredicate = osPredicate;
        myArchitecture = architecture;
        myWithJre = withJre;
        myPluginId = PluginId.getId(id);
    }

    @Nonnull
    public static PlatformPluginId find() {
        boolean jreBuild = PlatformOrPluginUpdateChecker.isJreBuild();
        Platform platform = Platform.current();
        PlatformOperatingSystem os = platform.os();
        CpuArchitecture arch = platform.jvm().arch();

        PlatformPluginId target = null;

        for (PlatformPluginId platformPluginId : values()) {
            if (!platformPluginId.myOsPredicate.test(os)) {
                continue;
            }

            if (jreBuild != platformPluginId.myWithJre) {
                continue;
            }

            if (platformPluginId.myArchitecture == null || platformPluginId.myArchitecture == arch)  {
                target = platformPluginId;
                break;
            }

        }

        if (target != null) {
            return target;
        }

        throw new IllegalArgumentException("OS: " + os.name() + ", arch: " + arch + ", jreBuild: " + jreBuild);
    }

    public PluginId getPluginId() {
        return myPluginId;
    }
}
