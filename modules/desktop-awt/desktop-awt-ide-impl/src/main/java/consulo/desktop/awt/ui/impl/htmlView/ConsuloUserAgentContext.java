package consulo.desktop.awt.ui.impl.htmlView;

import consulo.application.Application;
import org.cobraparser.ua.NetworkRequest;
import org.cobraparser.ua.UserAgentContext;

import java.net.URL;
import java.security.Policy;

class ConsuloUserAgentContext implements UserAgentContext {

    @Override
    public boolean isRequestPermitted(Request request) {
        return false;
    }

    @Override
    public NetworkRequest createHttpRequest() {
        if (ConsuloHtmlRendererContext.ENABLE_IMAGE_LOADING) {
            return new ConsuloNetworkRequest();
        }
        
        return null;
    }

    @Override
    public String getAppCodeName() {
        return "";
    }

    @Override
    public String getAppName() {
        return Application.get().getName().get();
    }

    @Override
    public String getAppVersion() {
        return "";
    }

    @Override
    public String getAppMinorVersion() {
        return "";
    }

    @Override
    public String getBrowserLanguage() {
        return "";
    }

    @Override
    public boolean isCookieEnabled() {
        return false;
    }

    @Override
    public boolean isScriptingEnabled() {
        return false;
    }

    @Override
    public boolean isExternalCSSEnabled() {
        return true;
    }

    @Override
    public boolean isInternalCSSEnabled() {
        return true;
    }

    @Override
    public String getPlatform() {
        return "";
    }

    @Override
    public String getUserAgent() {
        return "";
    }

    @Override
    public String getCookie(URL url) {
        return null;
    }

    @Override
    public void setCookie(URL url, String s) {

    }

    @Override
    public Policy getSecurityPolicy() {
        return null;
    }

    @Override
    public int getScriptingOptimizationLevel() {
        return 0;
    }

    @Override
    public boolean isMedia(String s) {
        return false;
    }

    @Override
    public String getVendor() {
        return "";
    }

    @Override
    public String getProduct() {
        return "";
    }
}
