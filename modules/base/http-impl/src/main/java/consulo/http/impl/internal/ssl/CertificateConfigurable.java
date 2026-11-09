package consulo.http.impl.internal.ssl;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileTypeDescriptor;
import consulo.http.localize.HttpLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.PlaceholderLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import org.jspecify.annotations.Nullable;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static consulo.http.impl.internal.ssl.CertificateUtil.getCommonName;
import static consulo.http.impl.internal.ssl.HttpConfirmingTrustManagerImplHttp.MutableTrustManagerHttp;

/**
 * @author Mikhail Golubev
 */
@ExtensionImpl
public class CertificateConfigurable implements SearchableConfigurable, Configurable.NoScroll, CertificateListener, ApplicationConfigurable {
    private static final FileTypeDescriptor CERTIFICATE_DESCRIPTOR =
        new FileTypeDescriptor(HttpLocalize.certificateChooseCertificate(), ".crt", ".cer", ".pem");

    private @Nullable CheckBox myAcceptAutomatically;
    private @Nullable MutableTrustManagerHttp myTrustManager;

    private @Nullable Tree<Object> myTree;
    private final CertificateTreeModel myTreeModel = new CertificateTreeModel();
    private @Nullable PlaceholderLayout myDetailsPanel;
    private @Nullable Label myDetailsPlaceholder;
    private final Map<X509Certificate, Component> myInfoPanels = new HashMap<>();
    private final Set<X509Certificate> myCertificates = new HashSet<>();
    private @Nullable X509Certificate mySelectedCertificate;

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
    public Component createUIComponent(Disposable parentDisposable) {
        myTrustManager = HttpCertificateManagerImpl.getInstance().getCustomTrustManager();
        // show newly added certificates
        myTrustManager.addListener(this);

        myAcceptAutomatically = CheckBox.create(HttpLocalize.certificateAcceptNonTrustedCertificatesAutomatically());

        Tree<Object> tree = Tree.create(myTreeModel);
        tree.setAllowMultipleSelect(false);
        tree.addSelectListener(event -> {
            Object value = event.getValue() == null ? null : event.getValue().getValue();
            showCertificate(myTreeModel.getCertificates(value == null ? List.of() : List.of(value)).stream().findFirst().orElse(null));
        });
        Disposer.register(parentDisposable, tree.destroyHook());
        myTree = tree;

        Component decorated = ToolbarDecoratorBuilderFactory.getInstance()
            .create(tree)
            .addOrReplaceAction(new AddAction<>() {
                @Override
                @RequiredUIAccess
                protected void doAdd(AnActionEvent e) {
                    addCertificateFromFile();
                }
            })
            .addOrReplaceAction(new RemoveAction<>() {
                @Override
                @RequiredUIAccess
                public void actionPerformed(AnActionEvent e) {
                    removeSelectedCertificates();
                }
            })
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP)
            .build();

        myDetailsPlaceholder = Label.create(HttpLocalize.certificateNoCertificateSelected());
        PlaceholderLayout detailsPanel = PlaceholderLayout.create();
        detailsPanel.setPlaceholder(DockLayout.create().center(myDetailsPlaceholder));
        detailsPanel.setPlaceholderVisibility(() -> mySelectedCertificate == null);
        myDetailsPanel = detailsPanel;

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        splitLayout.setFirstComponent(LabeledLayout.create(HttpLocalize.certificateAcceptedCertificates(), decorated));
        splitLayout.setSecondComponent(detailsPanel);
        splitLayout.setProportion(30);

        DockLayout root = DockLayout.create();
        root.top(myAcceptAutomatically);
        root.center(splitLayout);
        return root;
    }

    @RequiredUIAccess
    private void addCertificateFromFile() {
        UIAccess uiAccess = UIAccess.current();
        FileChooser.chooseFile(CERTIFICATE_DESCRIPTOR, (Project) null, null).whenCompleteAsync((file, error) -> {
            if (file == null || myTree == null) {
                return;
            }
            X509Certificate certificate = CertificateUtil.loadX509Certificate(file.getPath());
            if (certificate == null) {
                MessageBoxes.okError(HttpLocalize.certificateMalformedX509ServerCertificate())
                    .title(HttpLocalize.certificateNotImported())
                    .showAsync(myTree);
            }
            else if (myCertificates.contains(certificate)) {
                MessageBoxes.okWarning(HttpLocalize.certificateCertificateAlreadyExists())
                    .title(HttpLocalize.certificateNotImported())
                    .showAsync(myTree);
            }
            else {
                myCertificates.add(certificate);
                myTreeModel.addCertificate(certificate);
                refreshTree(certificate);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void removeSelectedCertificates() {
        Tree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        List<Object> selected = tree.getSelectedNodes().stream().map(TreeNode::getValue).toList();
        // removing an organization removes all its certificates
        for (X509Certificate certificate : myTreeModel.getCertificates(selected)) {
            myCertificates.remove(certificate);
            myTreeModel.removeCertificate(certificate);
            myInfoPanels.remove(certificate);
        }

        CertificateWrapper first = myTreeModel.getFirstCertificate();
        refreshTree(first == null ? null : first.getCertificate());
    }

    @RequiredUIAccess
    private void refreshTree(@Nullable X509Certificate toSelect) {
        Tree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        if (toSelect == null) {
            showCertificate(null);
        }

        UIAccess uiAccess = UIAccess.current();
        tree.refreshAll()
            .thenCompose(it -> tree.expandAll())
            .whenCompleteAsync((it, error) -> {
                if (toSelect != null && myTree == tree) {
                    selectCertificate(tree, toSelect);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void selectCertificate(Tree<Object> tree, X509Certificate certificate) {
        TreeNode<Object> rootNode = tree.getRootNode();
        if (rootNode == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        rootNode.findChildDeep(value -> value instanceof CertificateWrapper wrapper && wrapper.getCertificate().equals(certificate))
            .whenCompleteAsync((node, error) -> {
                if (node != null && myTree == tree) {
                    tree.select(node);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    private void showCertificate(@Nullable X509Certificate certificate) {
        PlaceholderLayout detailsPanel = myDetailsPanel;
        Label placeholder = myDetailsPlaceholder;
        if (detailsPanel == null || placeholder == null) {
            return;
        }

        mySelectedCertificate = certificate;
        placeholder.setText(myTreeModel.isEmpty() ? HttpLocalize.certificateNoCertificates() : HttpLocalize.certificateNoCertificateSelected());
        if (certificate != null) {
            Component infoPanel = myInfoPanels.computeIfAbsent(certificate, it -> {
                Component info = new CertificateInfoPanel(it).getComponent();
                DockLayout padded = DockLayout.create();
                padded.top(info);
                return ScrollableLayout.create(padded);
            });
            detailsPanel.setContent(infoPanel);
        }
        detailsPanel.testPlaceholder();
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        if (myAcceptAutomatically == null || myTrustManager == null) {
            return false;
        }

        HttpCertificateManagerState state = HttpCertificateManagerImpl.getInstance().getState();
        return myAcceptAutomatically.getValue() != state.ACCEPT_AUTOMATICALLY
            || !myCertificates.equals(new HashSet<>(myTrustManager.getCertificates()));
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        MutableTrustManagerHttp trustManager = myTrustManager;
        CheckBox acceptAutomatically = myAcceptAutomatically;
        if (trustManager == null || acceptAutomatically == null) {
            return;
        }

        List<X509Certificate> existing = trustManager.getCertificates();

        Set<X509Certificate> added = new HashSet<>(myCertificates);
        added.removeAll(existing);

        Set<X509Certificate> removed = new HashSet<>(existing);
        removed.removeAll(myCertificates);

        for (X509Certificate certificate : added) {
            if (!trustManager.addCertificate(certificate)) {
                throw new ConfigurationException(
                    HttpLocalize.certificateCannotAddCertificateFor(getCommonName(certificate)),
                    HttpLocalize.certificateCannotAddCertificate()
                );
            }
        }

        for (X509Certificate certificate : removed) {
            if (!trustManager.removeCertificate(certificate)) {
                throw new ConfigurationException(
                    HttpLocalize.certificateCannotRemoveCertificateFor(getCommonName(certificate)),
                    HttpLocalize.certificateCannotRemoveCertificate()
                );
            }
        }
        HttpCertificateManagerState state = HttpCertificateManagerImpl.getInstance().getState();

        state.ACCEPT_AUTOMATICALLY = acceptAutomatically.getValue();
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        MutableTrustManagerHttp trustManager = myTrustManager;
        CheckBox acceptAutomatically = myAcceptAutomatically;
        if (trustManager == null || acceptAutomatically == null) {
            return;
        }

        List<X509Certificate> original = trustManager.getCertificates();
        myTreeModel.reset(original);

        myCertificates.clear();
        myCertificates.addAll(original);
        myInfoPanels.clear();

        CertificateWrapper first = myTreeModel.getFirstCertificate();
        showCertificate(null);
        refreshTree(first == null ? null : first.getCertificate());

        HttpCertificateManagerState state = HttpCertificateManagerImpl.getInstance().getState();
        acceptAutomatically.setValue(state.ACCEPT_AUTOMATICALLY);
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        if (myTrustManager != null) {
            myTrustManager.removeListener(this);
        }
        myTrustManager = null;
        myTree = null;
        myDetailsPanel = null;
        myDetailsPlaceholder = null;
        myAcceptAutomatically = null;
        mySelectedCertificate = null;
        myInfoPanels.clear();
    }

    @Override
    public void certificateAdded(X509Certificate certificate) {
        giveToTree(() -> {
            if (!myCertificates.contains(certificate)) {
                myCertificates.add(certificate);
                myTreeModel.addCertificate(certificate);
                refreshTree(mySelectedCertificate);
            }
        });
    }

    @Override
    public void certificateRemoved(X509Certificate certificate) {
        giveToTree(() -> {
            if (myCertificates.contains(certificate)) {
                myCertificates.remove(certificate);
                myTreeModel.removeCertificate(certificate);
                myInfoPanels.remove(certificate);
                refreshTree(certificate.equals(mySelectedCertificate) ? null : mySelectedCertificate);
            }
        });
    }

    private void giveToTree(@RequiredUIAccess Runnable runnable) {
        Tree<Object> tree = myTree;
        UIAccess uiAccess = tree == null ? null : tree.getUIAccess();
        if (uiAccess != null && uiAccess.isValid()) {
            uiAccess.giveIfNeed(() -> {
                if (myTree == tree) {
                    runnable.run();
                }
            });
        }
    }
}
