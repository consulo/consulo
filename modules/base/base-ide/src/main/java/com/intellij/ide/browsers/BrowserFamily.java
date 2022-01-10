package com.intellij.ide.browsers;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.browsers.chrome.ChromeSettings;
import com.intellij.ide.browsers.firefox.FirefoxSettings;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.SystemInfo;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum BrowserFamily implements Iconable {
  CHROME(IdeBundle.message("browsers.chrome"), "chrome", "google-chrome", "Google Chrome", AllIcons.Xml.Browsers.Chrome16) {
    @Override
    public BrowserSpecificSettings createBrowserSpecificSettings() {
      return new ChromeSettings();
    }
  },
  FIREFOX(IdeBundle.message("browsers.firefox"), "firefox", "firefox", "Firefox", AllIcons.Xml.Browsers.Firefox16) {
    @Override
    public BrowserSpecificSettings createBrowserSpecificSettings() {
      return new FirefoxSettings();
    }
  },
  EXPLORER(IdeBundle.message("browsers.explorer"), "iexplore", null, null, AllIcons.Xml.Browsers.Explorer16),
  OPERA(IdeBundle.message("browsers.opera"), "opera", "opera", "Opera", AllIcons.Xml.Browsers.Opera16),
  SAFARI(IdeBundle.message("browsers.safari"), "safari", null, "Safari", AllIcons.Xml.Browsers.Safari16);

  private final String myName;
  private final String myWindowsPath;
  private final String myUnixPath;
  private final String myMacPath;
  private final Image myIcon;

  BrowserFamily(@Nonnull String name,
                @Nonnull final String windowsPath,
                @Nullable final String unixPath,
                @Nullable final String macPath,
                @Nonnull Image icon) {
    myName = name;
    myWindowsPath = windowsPath;
    myUnixPath = unixPath;
    myMacPath = macPath;
    myIcon = icon;
  }

  @Nullable
  public BrowserSpecificSettings createBrowserSpecificSettings() {
    return null;
  }

  @Nullable
  public String getExecutionPath() {
    if (SystemInfo.isWindows) {
      return myWindowsPath;
    }
    else if (SystemInfo.isMac) {
      return myMacPath;
    }
    else {
      return myUnixPath;
    }
  }

  public String getName() {
    return myName;
  }

  public Image getIcon() {
    return myIcon;
  }

  @Override
  public String toString() {
    return myName;
  }

  @Override
  public Image getIcon(@IconFlags int flags) {
    return getIcon();
  }
}