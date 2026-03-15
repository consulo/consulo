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

    @Nullable
    public abstract String getPath();

    
    public abstract String getBrowserNotFoundMessage();

    @Nullable
    public abstract BrowserSpecificSettings getSpecificSettings();

    public void addOpenUrlParameter(List<? super String> command, String url) {
        command.add(url);
    }
}