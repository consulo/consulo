package consulo.webBrowser;

import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public abstract class WebBrowser {
    
    public abstract String getName();

    
    public abstract UUID getId();

    
    public abstract BrowserFamily getFamily();

    
    public abstract Image getIcon();

    public abstract @Nullable String getPath();

    
    public abstract String getBrowserNotFoundMessage();

    public abstract @Nullable BrowserSpecificSettings getSpecificSettings();

    public void addOpenUrlParameter(List<? super String> command, String url) {
        command.add(url);
    }
}