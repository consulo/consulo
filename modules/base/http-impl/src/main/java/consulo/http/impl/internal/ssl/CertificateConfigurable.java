package consulo.http.impl.internal.ssl;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileTypeDescriptor;
import consulo.http.localize.HttpLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.*;
import consulo.ui.layout.VerticalLayout;
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
    private static final FileChooserDescriptor CERTIFICATE_DESCRIPTOR =
        new FileTypeDescriptor("Choose Certificate", "crt", "cer", "pem", "der");

    public static final String EMPTY_PANEL = "empty.panel";

    private VerticalLayout myRootPanel;

    private CheckBox myAcceptAutomatically;

    private LabeledLayout myCertificatesListPanel;
    private JPanel myDetailsPanel;
    private DockLayout myEmptyPanel;
    private MutableTrustManagerHttp myTrustManager;

    private Tree myTree;
    private CertificateTreeBuilder myTreeBuilder;
    private Set<X509Certificate> myCertificates = new HashSet<>();

    @RequiredUIAccess
    public CertificateConfigurable() {
        myCertificatesListPanel = LabeledLayout.create(HttpLocalize.certificateAcceptedCertificates());
        myCertificatesListPanel.addStyle(LabeledLayoutStyle.NO_INDENT);

        VerticalLayout panel1 = VerticalLayout.create();
        Spacer spacer1 = new Spacer();
        panel1.add(TargetAWT.wrap(spacer1));
        myAcceptAutomatically = CheckBox.create(HttpLocalize.certificateAcceptNonTrustedCertificatesAutomatically());
        panel1.add(myAcceptAutomatically);
        myDetailsPanel = new JPanel();
        myDetailsPanel.setLayout(new CardLayout(0, 0));
        myEmptyPanel = DockLayout.create()
            .center(Label.create(HttpLocalize.certificateNoCertificateSelected()));
        myDetailsPanel.add(TargetAWT.to(myEmptyPanel), "Card1");
        TwoComponentSplitLayout splitter1 = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL)
            .setFirstComponent(myCertificatesListPanel)
            .setSecondComponent(TargetAWT.wrap(myDetailsPanel));
        splitter1.setProportion(30);

        myRootPanel = VerticalLayout.create()
            .add(myCertificatesListPanel)
            .add(panel1)
            .add(TargetAWT.wrap(myDetailsPanel))
            .add(splitter1);
    }

    @RequiredUIAccess
    private void initializeUI() {
        myTree = new Tree();
        myTreeBuilder = new CertificateTreeBuilder(myTree);

        myTrustManager = HttpCertificateManagerImpl.getInstance().getCustomTrustManager();
        // show newly added certificates
        myTrustManager.addListener(this);

        myTree.getEmptyText().setText(HttpLocalize.certificateNoCertificates());
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        myTree.setRootVisible(false);
        //myTree.setShowsRootHandles(false);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTree).disableUpDownActions();
        decorator.setAddAction(button -> {
            // show choose file dialog, add certificate
            FileChooser.chooseFile(CERTIFICATE_DESCRIPTOR, null, null).doWhenDone(file -> {
                String path = file.getPath();
                X509Certificate certificate = CertificateUtil.loadX509Certificate(path);
                if (certificate == null) {
                    Messages.showErrorDialog(
                        TargetAWT.to(myRootPanel),
                        HttpLocalize.certificateMalformedX509ServerCertificate().get(),
                        HttpLocalize.certificateNotImported().get()
                    );
                }
                else if (myCertificates.contains(certificate)) {
                    Messages.showWarningDialog(
                        TargetAWT.to(myRootPanel),
                        HttpLocalize.certificateCertificateAlreadyExists().get(),
                        HttpLocalize.certificateNotImported().get()
                    );
                }
                else {
                    myCertificates.add(certificate);
                    myTreeBuilder.addCertificate(certificate);
                    addCertificatePanel(certificate);
                    myTreeBuilder.selectCertificate(certificate);
                }
            });
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
        myCertificatesListPanel.add(TargetAWT.wrap(decorator.createPanel()), LayoutConstraint.NONE);
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
        return (JComponent) TargetAWT.to(myRootPanel);
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        HttpCertificateManagerImpl.Config state = HttpCertificateManagerImpl.getInstance().getState();
        return myAcceptAutomatically.getValue() != state.ACCEPT_AUTOMATICALLY
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
                throw new ConfigurationException(
                    HttpLocalize.certificateCannotAddCertificateFor(getCommonName(certificate)),
                    HttpLocalize.certificateCannotAddCertificate()
                );
            }
        }

        for (X509Certificate certificate : removed) {
            if (!myTrustManager.removeCertificate(certificate)) {
                throw new ConfigurationException(
                    HttpLocalize.certificateCannotRemoveCertificateFor(getCommonName(certificate)),
                    HttpLocalize.certificateCannotRemoveCertificate()
                );
            }
        }
        HttpCertificateManagerImpl.Config state = HttpCertificateManagerImpl.getInstance().getState();

        state.ACCEPT_AUTOMATICALLY = myAcceptAutomatically.getValue();
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        List<X509Certificate> original = myTrustManager.getCertificates();
        myTreeBuilder.reset(original);

        myCertificates.clear();
        myCertificates.addAll(original);

        myDetailsPanel.removeAll();
        myDetailsPanel.add(TargetAWT.to(myEmptyPanel), EMPTY_PANEL);

        // fill lower panel with cards
        for (X509Certificate certificate : original) {
            addCertificatePanel(certificate);
        }

        if (!myCertificates.isEmpty()) {
            myTreeBuilder.selectFirstCertificate();
        }

        HttpCertificateManagerImpl.Config state = HttpCertificateManagerImpl.getInstance().getState();
        myAcceptAutomatically.setValue(state.ACCEPT_AUTOMATICALLY);
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
        return (JComponent) TargetAWT.to(myRootPanel);
    }
}
