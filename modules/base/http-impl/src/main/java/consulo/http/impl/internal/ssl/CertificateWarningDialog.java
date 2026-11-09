package consulo.http.impl.internal.ssl;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.io.FileUtil;
import consulo.util.lang.xml.XmlStringUtil;

import org.jspecify.annotations.Nullable;

import java.security.cert.X509Certificate;

/**
 * @author Mikhail Golubev
 */
public class CertificateWarningDialog extends DialogDescriptor {
    public static CertificateWarningDialog createUntrustedCertificateWarning(X509Certificate certificate) {
        return new CertificateWarningDialog(
            certificate,
            LocalizeValue.localizeTODO("Untrusted Server's Certificate"),
            LocalizeValue.localizeTODO("Server's certificate is not trusted")
        );
    }

    public static CertificateWarningDialog createExpiredCertificateWarning(X509Certificate certificate) {
        throw new UnsupportedOperationException("Not supported");
    }

    public static CertificateWarningDialog createHostnameMismatchWarning(X509Certificate certificate, String hostname) {
        String message = String.format(
            "Server's certificate common name doesn't match hostname in URL: '%s' != '%s'",
            new CertificateWrapper(certificate).getSubjectField(CertificateWrapper.CommonField.COMMON_NAME),
            hostname
        );
        return new CertificateWarningDialog(certificate, LocalizeValue.localizeTODO("Invalid hostname"), LocalizeValue.of(message));
    }

    private final X509Certificate myCertificate;
    private final LocalizeValue myMessage;

    public CertificateWarningDialog(X509Certificate certificate, LocalizeValue title, LocalizeValue message) {
        super(title);
        myCertificate = certificate;
        myMessage = message;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(70, 45);
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        Label message = Label.create(myMessage);
        message.setImage(PlatformIconGroup.generalWarningdialog());

        HttpCertificateManagerImpl manager = HttpCertificateManagerImpl.getInstance();
        String path = FileUtil.toCanonicalPath(manager.getCacertsPath());
        HtmlLabel notice = HtmlLabel.create(LocalizeValue.localizeTODO(String.format(
            "Accepted certificate will be saved in truststore <code>%s</code> with default password <code>%s</code>",
            XmlStringUtil.escapeText(path),
            XmlStringUtil.escapeText(manager.getPassword())
        )));

        DockLayout root = DockLayout.create(Space.LARGE);
        root.top(message);
        root.center(LabeledLayout.create(
            LocalizeValue.localizeTODO("Certificate details"),
            ScrollableLayout.create(new CertificateInfoPanel(myCertificate).getComponent())
        ));
        root.bottom(notice);
        return root;
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        DialogCancelAction reject = new DialogCancelAction(LocalizeValue.localizeTODO("Reject"));
        if (inverseOrder) {
            return new AnAction[]{reject, createOkAction()};
        }
        return new AnAction[]{createOkAction(), reject};
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(LocalizeValue.localizeTODO("Accept"));
    }
}
