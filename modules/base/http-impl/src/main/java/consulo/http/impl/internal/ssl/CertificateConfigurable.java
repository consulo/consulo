package consulo.http.impl.internal.ssl;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileTypeDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static consulo.http.impl.internal.ssl.CertificateUtil.getCommonName;
import static consulo.http.impl.internal.ssl.HttpConfirmingTrustManagerImplHttp.MutableTrustManagerHttp;

/**
 * @author Mikhail Golubev
 */
@ExtensionImpl
public class CertificateConfigurable
    implements SearchableConfigurable, Configurable.NoScroll, CertificateListener, ApplicationConfigurable {
    private static final FileTypeDescriptor CERTIFICATE_DESCRIPTOR = new FileTypeDescriptor("Choose Certificate", ".crt", ".cer", ".pem");

    public static final String EMPTY_PANEL = "empty.panel";

    private JPanel myRootPanel;

    private CheckBox myAcceptAutomatically;
    private CheckBox myCheckHostname;
    private CheckBox myCheckValidityPeriod;

    private JPanel myCertificatesListPanel;
    private JPanel myDetailsPanel;
    private JPanel myEmptyPanel;
    private MutableTrustManagerHttp myTrustManager;

    private Tree myTree;
    private CertificateTreeBuilder myTreeBuilder;
    private Set<X509Certificate> myCertificates = new HashSet<>();

    @RequiredUIAccess
    public CertificateConfigurable() {
        myRootPanel = new JPanel();
        myRootPanel.setLayout(new GridLayoutManager(4, 1, JBUI.emptyInsets(), -1, -1));
        myCertificatesListPanel = new JPanel();
        myCertificatesListPanel.setLayout(new BorderLayout(0, 0));
        myCertificatesListPanel.putClientProperty("BorderFactoryClass", "consulo.ui.ex.awt.IdeBorderFactory$PlainSmallWithoutIndent");
        myRootPanel.add(
            myCertificatesListPanel,
            new GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        myCertificatesListPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Accepted certificates"));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 4, JBUI.emptyInsets(), -1, -1));
        panel1.putClientProperty("BorderFactoryClass", "consulo.ui.ex.awt.IdeBorderFactory$PlainSmallWithIndent");
        myRootPanel.add(
            panel1,
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        myCheckHostname = CheckBox.create(LocalizeValue.localizeTODO("Check &hostname"));
        panel1.add(
            TargetAWT.to(myCheckHostname),
            new GridConstraints(
                0,
                1,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myCheckValidityPeriod = CheckBox.create(LocalizeValue.localizeTODO("Check &validity period"));
        panel1.add(
            TargetAWT.to(myCheckValidityPeriod),
            new GridConstraints(
                0,
                2,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        Spacer spacer1 = new Spacer();
        panel1.add(
            spacer1,
            new GridConstraints(
                0,
                3,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                1,
                null,
                null,
                null,
                0,
                false
            )
        );
        myAcceptAutomatically = CheckBox.create(LocalizeValue.localizeTODO("&Accept non-trusted certificates automatically"));
        panel1.add(
            TargetAWT.to(myAcceptAutomatically),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        myDetailsPanel = new JPanel();
        myDetailsPanel.setLayout(new CardLayout(0, 0));
        myRootPanel.add(
            myDetailsPanel,
            new GridConstraints(
                3,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                new Dimension(499, 24),
                null,
                0,
                false
            )
        );
        myEmptyPanel = new JPanel();
        myEmptyPanel.setLayout(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        myDetailsPanel.add(myEmptyPanel, "Card1");
        Label jBLabel1 = Label.create(LocalizeValue.localizeTODO("No certificate selected"));
        myEmptyPanel.add(
            TargetAWT.to(jBLabel1),
            new GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                null,
                null,
                0,
                false
            )
        );
        Splitter splitter1 = new Splitter();
        splitter1.setOrientation(true);
        splitter1.setProportion(0.3f);
        splitter1.setShowDividerControls(false);
        myRootPanel.add(
            splitter1,
            new GridConstraints(
                2,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                null,
                null,
                0,
                false
            )
        );
        splitter1.setSecondComponent(myDetailsPanel);
        splitter1.setFirstComponent(myCertificatesListPanel);
    }

    @RequiredUIAccess
    private void initializeUI() {
        myTree = new Tree();
        myTreeBuilder = new CertificateTreeBuilder(myTree);

        // are not fully functional by now
        myCheckHostname.setVisible(false);
        myCheckValidityPeriod.setVisible(false);

        myTrustManager = HttpCertificateManagerImpl.getInstance().getCustomTrustManager();
        // show newly added certificates
        myTrustManager.addListener(this);

        myTree.getEmptyText().setText("No certificates");
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        myTree.setRootVisible(false);
        //myTree.setShowsRootHandles(false);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTree).disableUpDownActions();
        decorator.setAddAction(button -> {
            // show choose file dialog, add certificate
            IdeaFileChooser.chooseFile(
                CERTIFICATE_DESCRIPTOR,
                null,
                null,
                file -> {
                    String path = file.getPath();
                    X509Certificate certificate = CertificateUtil.loadX509Certificate(path);
                    if (certificate == null) {
                        Messages.showErrorDialog(myRootPanel, "Malformed X509 server certificate", "Not Imported");
                    }
                    else if (myCertificates.contains(certificate)) {
                        Messages.showWarningDialog(myRootPanel, "Certificate already exists", "Not Imported");
                    }
                    else {
                        myCertificates.add(certificate);
                        myTreeBuilder.addCertificate(certificate);
                        addCertificatePanel(certificate);
                        myTreeBuilder.selectCertificate(certificate);
                    }
                }
            );
        }).setRemoveAction(button -> {
            // allow to delete several certificates at once
            for (X509Certificate certificate : myTreeBuilder.getSelectedCertificates(true)) {
                myCertificates.remove(certificate);
                myTreeBuilder.removeCertificate(certificate);
            }
            if (myCertificates.isEmpty()) {
                showCard(EMPTY_PANEL);
            }
            else {
                myTreeBuilder.selectFirstCertificate();
            }
        });

        myTree.addTreeSelectionListener(e -> {
            X509Certificate certificate = myTreeBuilder.getFirstSelectedCertificate(true);
            if (certificate != null) {
                showCard(getCardName(certificate));
            }
        });
        myCertificatesListPanel.add(decorator.createPanel(), BorderLayout.CENTER);
    }

    private void showCard(String cardName) {
        ((CardLayout) myDetailsPanel.getLayout()).show(myDetailsPanel, cardName);
    }

    private void addCertificatePanel(X509Certificate certificate) {
        String uniqueName = getCardName(certificate);
        JPanel infoPanel = new CertificateInfoPanel(certificate);
        UIUtil.addInsets(infoPanel, UIUtil.PANEL_REGULAR_INSETS);
        JBScrollPane scrollPane = new JBScrollPane(infoPanel);
        //scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        myDetailsPanel.add(scrollPane, uniqueName);
    }

    private static String getCardName(X509Certificate certificate) {
        return certificate.getSubjectX500Principal().getName();
    }

    @Override
    public String getId() {
        return "http.certificates";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }


    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Server Certificates");
    }

    @Override
    @RequiredUIAccess
    public @Nullable JComponent createComponent() {
        // lazily initialized to ensure that disposeUIResources() will be called, if
        // tree builder was created
        initializeUI();
        return myRootPanel;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        HttpCertificateManagerImpl.Config state = HttpCertificateManagerImpl.getInstance().getState();
        return myAcceptAutomatically.getValue() != state.ACCEPT_AUTOMATICALLY
            || myCheckHostname.getValue() != state.CHECK_HOSTNAME
            || myCheckValidityPeriod.getValue() != state.CHECK_VALIDITY
            || !myCertificates.equals(new HashSet<>(myTrustManager.getCertificates()));
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        List<X509Certificate> existing = myTrustManager.getCertificates();

        Set<X509Certificate> added = new HashSet<>(myCertificates);
        added.removeAll(existing);

        Set<X509Certificate> removed = new HashSet<>(existing);
        removed.removeAll(myCertificates);

        for (X509Certificate certificate : added) {
            if (!myTrustManager.addCertificate(certificate)) {
                throw new ConfigurationException("Cannot add certificate for " + getCommonName(certificate), "Cannot Add Certificate");
            }
        }

        for (X509Certificate certificate : removed) {
            if (!myTrustManager.removeCertificate(certificate)) {
                throw new ConfigurationException(
                    "Cannot remove certificate for " + getCommonName(certificate),
                    "Cannot Remove Certificate"
                );
            }
        }
        HttpCertificateManagerImpl.Config state = HttpCertificateManagerImpl.getInstance().getState();

        state.ACCEPT_AUTOMATICALLY = myAcceptAutomatically.getValue();
        state.CHECK_HOSTNAME = myCheckHostname.getValue();
        state.CHECK_VALIDITY = myCheckValidityPeriod.getValue();
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        List<X509Certificate> original = myTrustManager.getCertificates();
        myTreeBuilder.reset(original);

        myCertificates.clear();
        myCertificates.addAll(original);

        myDetailsPanel.removeAll();
        myDetailsPanel.add(myEmptyPanel, EMPTY_PANEL);

        // fill lower panel with cards
        for (X509Certificate certificate : original) {
            addCertificatePanel(certificate);
        }

        if (!myCertificates.isEmpty()) {
            myTreeBuilder.selectFirstCertificate();
        }

        HttpCertificateManagerImpl.Config state = HttpCertificateManagerImpl.getInstance().getState();
        myAcceptAutomatically.setValue(state.ACCEPT_AUTOMATICALLY);
        myCheckHostname.setValue(state.CHECK_HOSTNAME);
        myCheckValidityPeriod.setValue(state.CHECK_VALIDITY);
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        if (myTreeBuilder != null) {
            Disposer.dispose(myTreeBuilder);
        }
        if (myTrustManager != null) {
            myTrustManager.removeListener(this);
        }
    }

    @Override
    public void certificateAdded(X509Certificate certificate) {
        UIUtil.invokeLaterIfNeeded(() -> {
            if (myTreeBuilder != null && !myCertificates.contains(certificate)) {
                myCertificates.add(certificate);
                myTreeBuilder.addCertificate(certificate);
                addCertificatePanel(certificate);
            }
        });
    }

    @Override
    public void certificateRemoved(X509Certificate certificate) {
        UIUtil.invokeLaterIfNeeded(() -> {
            if (myTreeBuilder != null && myCertificates.contains(certificate)) {
                myCertificates.remove(certificate);
                myTreeBuilder.removeCertificate(certificate);
            }
        });
    }

    public JComponent $$$getRootComponent$$$() {
        return myRootPanel;
    }
}
