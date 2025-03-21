/**
 * @author VISTALL
 * @since 2024-12-11
 */
module consulo.web.browser.impl {
    requires consulo.web.browser.api;
    requires consulo.process.api;
    requires consulo.configurable.api;
    requires consulo.language.api;
    requires consulo.project.ui.view.api;

    // TODO remove in future
    requires java.desktop;
    requires consulo.ui.ex.awt.api;

    opens consulo.webBrowser.impl.internal to consulo.util.xml.serializer;
}