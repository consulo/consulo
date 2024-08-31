package consulo.webBrowser;

import consulo.application.AllIcons;
import consulo.component.util.Iconable;
import consulo.platform.Platform;
import consulo.ui.image.Image;
import consulo.webBrowser.chrome.ChromeSettings;
import consulo.webBrowser.firefox.FirefoxSettings;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public enum BrowserFamily implements Iconable {
    CHROME(WebBrowserBundle.message("browsers.chrome"), "chrome", "google-chrome", "Google Chrome", AllIcons.Xml.Browsers.Chrome16) {
        @Override
        public BrowserSpecificSettings createBrowserSpecificSettings() {
            return new ChromeSettings();
        }
    },
    FIREFOX(WebBrowserBundle.message("browsers.firefox"), "firefox", "firefox", "Firefox", AllIcons.Xml.Browsers.Firefox16) {
        @Override
        public BrowserSpecificSettings createBrowserSpecificSettings() {
            return new FirefoxSettings();
        }
    },
    EXPLORER(WebBrowserBundle.message("browsers.explorer"), "iexplore", null, null, AllIcons.Xml.Browsers.Explorer16),
    OPERA(WebBrowserBundle.message("browsers.opera"), "opera", "opera", "Opera", AllIcons.Xml.Browsers.Opera16),
    SAFARI(WebBrowserBundle.message("browsers.safari"), "safari", null, "Safari", AllIcons.Xml.Browsers.Safari16);

    private final String myName;
    private final String myWindowsPath;
    private final String myUnixPath;
    private final String myMacPath;
    private final Image myIcon;

    BrowserFamily(
        @Nonnull String name,
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