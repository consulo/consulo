package consulo.desktop.awt.ui.impl.htmlView;

import consulo.ide.impl.idea.ide.BrowserUtil;
import org.cobraparser.html.AbstractHtmlRendererContext;
import org.cobraparser.html.FormInput;
import org.cobraparser.html.domimpl.HTMLDocumentImpl;
import org.cobraparser.html.gui.HtmlPanel;
import org.cobraparser.ua.UserAgentContext;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLElement;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Optional;

class ConsuloHtmlRendererContext extends AbstractHtmlRendererContext {
    private final HtmlPanel myHtmlPanel;

    private final UserAgentContext myUserAgentContext;

    public ConsuloHtmlRendererContext(HtmlPanel panel) {
        myHtmlPanel = panel;
        myUserAgentContext = new ConsuloUserAgentContext();
    }

    @Override
    public HTMLCollection getFrames() {
        Object rootNode = myHtmlPanel.getRootNode();
        if (rootNode instanceof HTMLDocumentImpl) {
            return ((HTMLDocumentImpl) rootNode).getFrames();
        }
        else {
            return null;
        }
    }

    @Override
    public void navigate(URL url, String s) {

    }

    @Override
    public void linkClicked(HTMLElement htmlElement, URL url, String s) {
        BrowserUtil.browse(url);
    }

    @Override
    public void submitForm(String s, URL url, String s1, String s2, FormInput[] formInputs) {

    }

    @Override
    public UserAgentContext getUserAgentContext() {
        return myUserAgentContext;
    }

    @Override
    public boolean onMiddleClick(HTMLElement htmlElement, MouseEvent mouseEvent) {
        return false;
    }

    @Override
    public boolean isImageLoadingEnabled() {
        return false;
    }

    @Override
    public void setCursor(Optional<Cursor> cursorOpt) {
        cursorOpt.ifPresentOrElse(myHtmlPanel::setCursor, () -> myHtmlPanel.setCursor(Cursor.getDefaultCursor()));
    }

    @Override
    public void jobsFinished() {

    }

    @Override
    public void setJobFinishedHandler(Runnable runnable) {
        runnable.run();
    }
}
