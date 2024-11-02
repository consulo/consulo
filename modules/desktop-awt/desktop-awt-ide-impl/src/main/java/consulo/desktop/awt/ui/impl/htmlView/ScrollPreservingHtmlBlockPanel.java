package consulo.desktop.awt.ui.impl.htmlView;

import org.cobraparser.html.HtmlRendererContext;
import org.cobraparser.html.domimpl.NodeImpl;
import org.cobraparser.html.gui.HtmlBlockPanel;
import org.cobraparser.html.renderer.FrameContext;
import org.cobraparser.html.renderer.RBlock;
import org.cobraparser.html.renderer.RBlockViewport;
import org.cobraparser.ua.UserAgentContext;

import java.awt.*;

class ScrollPreservingHtmlBlockPanel extends HtmlBlockPanel {
    public ScrollPreservingHtmlBlockPanel(Color background,
                                          boolean opaque,
                                          UserAgentContext pcontext,
                                          HtmlRendererContext rcontext,
                                          FrameContext frameContext) {
        super(background, opaque, pcontext, rcontext, frameContext);
    }

    @Override
    public void setRootNode(NodeImpl node) {
        if (node != null) {
            int oldX = 32768;
            int oldY = 32768;
            if (rblock != null) {
                final RBlockViewport viewport = rblock.getRBlockViewport();
                oldX = viewport.getX();
                oldY = viewport.getY();
            }
            final RBlock block = new RBlock(node, 0, this.ucontext, this.rcontext,
                this.frameContext, this);
            // block.setDefaultPaddingInsets(this.defaultPaddingInsets);
            block.setDefaultOverflowX(this.defaultOverflowX);
            block.setDefaultOverflowY(this.defaultOverflowY);

            block.getRBlockViewport().setX(oldX);
            block.getRBlockViewport().setY(oldY);

            node.setUINode(block);
            this.rblock = block;
        }
        else {
            this.rblock = null;
        }
        this.invalidate();
        this.validateAll();
        this.repaint();
    }
}
