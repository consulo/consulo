// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution.wsl;

import consulo.application.util.AtomicNullableLazyValue;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents legacy bash.exe WSL, see RUBY-20359
 */
public class WSLDistributionLegacy extends WSLDistribution {
  private static final WslDistributionDescriptor LEGACY_WSL = new WslDistributionDescriptor("UBUNTU_LEGACY", "ubuntu_bash", "bash.exe", "Ubuntu (Legacy)");

  private static final String WSL_ROOT_CHUNK = "\\lxss\\rootfs";

  private static final AtomicNullableLazyValue<String> WSL_ROOT_IN_WINDOWS_PROVIDER = AtomicNullableLazyValue.createValue(() -> {
    String localAppDataPath = System.getenv().get("LOCALAPPDATA");
    return StringUtil.isEmpty(localAppDataPath) ? null : localAppDataPath + WSL_ROOT_CHUNK;
  });

  @Nullable
  private static Path getExecutableRootPath() {
    String windir = System.getenv().get("windir");
    return StringUtil.isEmpty(windir) ? null : Paths.get(windir, "System32");
  }

  /**
   * @return legacy WSL ("Bash-on-Windows") if it's available, {@code null} otherwise
   */
  @Nullable
  public static WSLDistributionLegacy getInstance() {
    final Path executableRoot = getExecutableRootPath();
    if (executableRoot == null) return null;

    final Path executablePath = executableRoot.resolve(LEGACY_WSL.getExecutablePath());
    if (Files.exists(executablePath, LinkOption.NOFOLLOW_LINKS)) {
      return new WSLDistributionLegacy(executablePath);
    }
    return null;
  }

  private WSLDistributionLegacy(@Nonnull Path executablePath) {
    super(LEGACY_WSL, executablePath);
  }

  @Nonnull
  @Override
  protected String getRunCommandLineParameter() {
    return "-c";
  }

  @Nullable
  @Override
  public String getWslPath(@Nonnull String windowsPath) {
    String wslRootInHost = WSL_ROOT_IN_WINDOWS_PROVIDER.getValue();
    if (wslRootInHost == null) {
      return null;
    }

    if (FileUtil.isAncestor(wslRootInHost, windowsPath, true)) {  // this is some internal WSL file
      return FileUtil.toSystemIndependentName(windowsPath.substring(wslRootInHost.length()));
    }

    return super.getWslPath(windowsPath);
  }

  @Nullable
  @Override
  public String getWindowsPath(@Nonnull String wslPath) {
    String windowsPath = super.getWindowsPath(wslPath);
    if (windowsPath != null) {
      return windowsPath;
    }

    String wslRootInHost = WSL_ROOT_IN_WINDOWS_PROVIDER.getValue();
    if (wslRootInHost == null) {
      return null;
    }
    return FileUtil.toSystemDependentName(wslRootInHost + wslPath);
  }
}
