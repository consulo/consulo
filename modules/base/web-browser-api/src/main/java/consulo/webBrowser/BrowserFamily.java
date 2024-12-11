package consulo.webBrowser;

import consulo.component.util.Iconable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.image.Image;
import consulo.webBrowser.chrome.ChromeSettings;
import consulo.webBrowser.firefox.FirefoxSettings;
import consulo.webBrowser.icon.WebBrowserIconGroup;
import consulo.webBrowser.localize.WebBrowserLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public enum BrowserFamily implements Iconable {
    CHROME(WebBrowserLocalize.browsersChrome(), "chrome", "google-chrome", "Google Chrome", WebBrowserIconGroup.chrome()) {
        @Override
        public BrowserSpecificSettings createBrowserSpecificSettings() {
            return new ChromeSettings();
        }
    },
    FIREFOX(WebBrowserLocalize.browsersFirefox(), "firefox", "firefox", "Firefox", WebBrowserIconGroup.firefox()) {
        @Override
        public BrowserSpecificSettings createBrowserSpecificSettings() {
            return new FirefoxSettings();
        }
    },
    EXPLORER(WebBrowserLocalize.browsersExplorer(), "iexplore", null, null, WebBrowserIconGroup.explorer()),
    OPERA(WebBrowserLocalize.browsersOpera(), "opera", "opera", "Opera", WebBrowserIconGroup.opera()),
    SAFARI(WebBrowserLocalize.browsersSafari(), "safari", null, "Safari", WebBrowserIconGroup.safari());

    private final LocalizeValue myName;
    private final String myWindowsPath;
    private final String myUnixPath;
    private final String myMacPath;
    private final Image myIcon;

    BrowserFamily(
        @Nonnull LocalizeValue name,
        @Nonnull final String windowsPath,
        @Nullable final String unixPath,
        @Nullable final String macPath,
        @Nonnull Image icon
    ) {
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
        if (Platform.current().os().isWindows()) {
            return myWindowsPath;
        }
        else if (Platform.current().os().isMac()) {
            return myMacPath;
        }
        else {
            return myUnixPath;
        }
    }

    public String getName() {
        return myName.get();
    }

    public Image getIcon() {
        return myIcon;
    }

    @Override
    public String toString() {
        return myName.get();
    }

    @Override
    public Image getIcon(@IconFlags int flags) {
        return getIcon();
    }
}