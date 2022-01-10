package com.intellij.util.net.ssl;

import com.intellij.icons.AllIcons;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ui.UIUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.security.cert.X509Certificate;

/**
 * @author Mikhail Golubev
 */
public class CertificateWarningDialog extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(CertificateWarningDialog.class);

  public static CertificateWarningDialog createUntrustedCertificateWarning(@Nonnull X509Certificate certificate) {
    return new CertificateWarningDialog(certificate,
                                        "Untrusted Server's Certificate",
                                        "Server's certificate is not trusted");
  }

  public static CertificateWarningDialog createExpiredCertificateWarning(@Nonnull X509Certificate certificate) {
    throw new UnsupportedOperationException("Not supported");
  }

  public static CertificateWarningDialog createHostnameMismatchWarning(@Nonnull X509Certificate certificate,
                                                                       @Nonnull String hostname) {
    String message = String.format("Server's certificate common name doesn't match hostname in URL: '%s' != '%s'",
                                   new CertificateWrapper(certificate).getSubjectField(CertificateWrapper.CommonField.COMMON_NAME),
                                   hostname);
    return new CertificateWarningDialog(certificate, "Invalid hostname", message);
  }

  private JPanel myRootPanel;
  private JLabel myWarningSign;
  private JPanel myCertificateInfoPanel;
  private JTextPane myNoticePane;
  private JTextPane myMessagePane;
  private final X509Certificate myCertificate;

  public CertificateWarningDialog(@Nonnull X509Certificate certificate, @Nonnull String title, @Nonnull String message) {
    super((Project)null, false);

    myCertificate = certificate;

    CertificateManager manager = CertificateManager.getInstance();
    setTitle(title);
    myMessagePane.setText(String.format("<html><body><p>%s</p></body></html>", message));
    myMessagePane.setBackground(UIUtil.getPanelBackground());
    setOKButtonText("Accept");
    setCancelButtonText("Reject");
    myWarningSign.setIcon(TargetAWT.to(AllIcons.General.WarningDialog));

    Messages.installHyperlinkSupport(myNoticePane);
    //    myNoticePane.setFont(myNoticePane.getFont().deriveFont((float)FontSize.SMALL.getSize()));

    String path = FileUtil.toCanonicalPath(manager.getCacertsPath());
    String password = manager.getPassword();

    myNoticePane.setText(
      String.format("<html><p>" +
                    "Accepted certificate will be saved in truststore <code>%s</code> with default password <code>%s</code>" +
                    "</p><html>",
                    path, password
      )
    );
    myCertificateInfoPanel.add(new CertificateInfoPanel(certificate), BorderLayout.CENTER);
    setResizable(false);
    init();
    LOG.debug("Preferred size: " + getPreferredSize());
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myRootPanel;
  }
}
