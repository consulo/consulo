package consulo.http.impl.internal.ssl;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.StandardColors;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.HexFormat;
import java.util.Map;

import static consulo.http.impl.internal.ssl.CertificateWrapper.CommonField;

/**
 * @author Mikhail Golubev
 */
public class CertificateInfoPanel {
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.SHORT);

    private final CertificateWrapper myCertificateWrapper;
    private final VerticalLayout myComponent;

    @RequiredUIAccess
    public CertificateInfoPanel(X509Certificate certificate) {
        myCertificateWrapper = new CertificateWrapper(certificate);

        myComponent = VerticalLayout.create();
        myComponent.add(createSection("Issued To", createPrincipalForm(myCertificateWrapper.getSubjectFields())));
        myComponent.add(createSection("Issued By", createPrincipalForm(myCertificateWrapper.getIssuerFields())));

        String notBefore = DATE_FORMAT.format(myCertificateWrapper.getNotBefore());
        String notAfter = DATE_FORMAT.format(myCertificateWrapper.getNotAfter());
        FormBuilder validity = FormBuilder.create()
            .addLabeled(
                LocalizeValue.localizeTODO("Valid from:"),
                createColoredLabel(notBefore, "not yet valid", myCertificateWrapper.isNotYetValid())
            )
            .addLabeled(
                LocalizeValue.localizeTODO("Valid until:"),
                createColoredLabel(notAfter, "expired", myCertificateWrapper.isExpired())
            );
        myComponent.add(createSection("Validity Period", validity.build()));

        FormBuilder fingerprints = FormBuilder.create()
            .addLabeled(LocalizeValue.of("SHA-256:"), createTextPane(formatHex(myCertificateWrapper.getSha256Fingerprint())))
            .addLabeled(LocalizeValue.of("SHA-1:"), createTextPane(formatHex(myCertificateWrapper.getSha1Fingerprint())));
        myComponent.add(createSection("Fingerprints", fingerprints.build()));
    }

    public Component getComponent() {
        return myComponent;
    }

    private static final HexFormat HEX_FORMAT = HexFormat.ofDelimiter(" ").withUpperCase();
    private static final int HEX_BYTES_PER_LINE = 16;

    private static String formatHex(byte @Nullable [] bytes) {
        if (bytes == null) {
            return CertificateWrapper.NOT_AVAILABLE;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i += HEX_BYTES_PER_LINE) {
            if (i > 0) {
                builder.append('\n');
            }
            HEX_FORMAT.formatHex(builder, bytes, i, Math.min(i + HEX_BYTES_PER_LINE, bytes.length));
        }
        return builder.toString();
    }

    public X509Certificate getCertificate() {
        return myCertificateWrapper.getCertificate();
    }

    @RequiredUIAccess
    private static Component createPrincipalForm(Map<String, String> fields) {
        FormBuilder builder = FormBuilder.create();
        for (CommonField field : CommonField.values()) {
            String value = fields.get(field.getShortName());
            if (value == null) {
                continue;
            }
            LocalizeValue label = LocalizeValue.of(String.format("%s (%s):", field.getShortName(), field.getLongName()));
            builder.addLabeled(label, Label.create(LocalizeValue.of(value)));
        }
        return builder.build();
    }

    @RequiredUIAccess
    private static Component createSection(String title, Component content) {
        return LabeledLayout.create(LocalizeValue.localizeTODO(title), content);
    }

    @RequiredUIAccess
    private static Component createTextPane(String text) {
        TextArea area = TextArea.create(text);
        area.setEditable(false);
        return area;
    }

    @RequiredUIAccess
    private static Component createColoredLabel(String mainText, String errorText, boolean hasError) {
        if (hasError) {
            Label label = Label.create(LocalizeValue.of(mainText + " (" + errorText + ")"));
            label.setForegroundColor(StandardColors.RED);
            return label;
        }
        return Label.create(LocalizeValue.of(mainText));
    }
}
