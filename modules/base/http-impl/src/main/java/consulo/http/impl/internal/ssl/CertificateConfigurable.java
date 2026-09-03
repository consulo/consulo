package consulo.http.impl.internal.ssl;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileTypeDescriptor;
import consulo.http.localize.HttpLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.LabelOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.*;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
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
    private SwipeLayout myDetailsPanel;
    private DockLayout myEmptyPanel;
    private MutableTrustManagerHttp myTrustManager;

    private Tree myTree;
    private CertificateTreeBuilder myTreeBuilder;
    private Set<X509Certificate> myCertificates = new HashSet<>();

    @RequiredUIAccess
    public CertificateConfigurable() {
        myAcceptAutomatically = CheckBox.create(HttpLocalize.certificateAcceptNonTrustedCertificatesAutomatically());

        myCertificatesListPanel = LabeledLayout.create(HttpLocalize.certificateAcceptedCertificates());
        myCertificatesListPanel.addStyle(LabeledLayoutStyle.NO_INDENT);

        myEmptyPanel = DockLayout.create()
            .center(Label.create(HttpLocalize.certificateNoCertificateSelected(), LabelOptions.builder().horizontalAlignment(HorizontalAlignment.CENTER).build()));

        myDetailsPanel = SwipeLayout.create()
            .register(EMPTY_PANEL, myEmptyPanel);

        TwoComponentSplitLayout splitter = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL)
            .withFirstComponent(myCertificatesListPanel)
            .withSecondComponent(myDetailsPanel)
            .withProportion(30);

        myRootPanel = VerticalLayout.create()
            .add(myAcceptAutomatically)
            .add(splitter);
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
        myDetailsPanel.swipeLeftTo(cardName);
    }

    private void addCertificatePanel(X509Certificate certificate) {
        String uniqueName = getCardName(certificate);
        JPanel infoPanel = new CertificateInfoPanel(certificate);
        UIUtil.addInsets(infoPanel, UIUtil.PANEL_REGULAR_INSETS);
        myDetailsPanel.register(uniqueName, ScrollableLayout.create(TargetAWT.wrap(infoPanel)));
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
        return HttpLocalize.certificateDisplayName();
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
        myDetailsPanel.register(EMPTY_PANEL, myEmptyPanel);

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
