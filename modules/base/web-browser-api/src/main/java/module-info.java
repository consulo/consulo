/**
 * @author VISTALL
 * @since 24-Apr-22
 */
module consulo.web.browser.api {
    // TODO obsolete dependency
    requires java.desktop;
    requires forms.rt;
    requires consulo.ui.ex.awt.api;

    requires transitive consulo.project.api;
    requires consulo.code.editor.api;
    requires consulo.configurable.api;
    requires consulo.virtual.file.system.api;
    requires consulo.language.api;
    requires consulo.ui.ex.api;
    requires consulo.process.api;

    exports consulo.webBrowser;
    exports consulo.webBrowser.action;
    exports consulo.webBrowser.firefox;
    exports consulo.webBrowser.chrome;
    exports consulo.webBrowser.localize;
    exports consulo.webBrowser.icon;

    opens consulo.webBrowser.chrome to consulo.util.xml.serializer;
    opens consulo.webBrowser.firefox to consulo.util.xml.serializer;
}