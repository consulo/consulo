package consulo.http.impl.internal.ssl;

import consulo.disposer.Disposable;
import consulo.http.localize.HttpLocalize;
import consulo.logging.Logger;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.AsyncTreeModel;
import consulo.ui.ex.awt.tree.StructureTreeModel;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import org.jspecify.annotations.Nullable;

import java.security.cert.X509Certificate;
import java.util.*;

import static consulo.http.impl.internal.ssl.CertificateWrapper.CommonField.COMMON_NAME;
import static consulo.http.impl.internal.ssl.CertificateWrapper.CommonField.ORGANIZATION;

/**
 * @author Mikhail Golubev
 */
public final class CertificateTreeBuilder implements Disposable {
    private static final SimpleTextAttributes STRIKEOUT_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, null);
    private static final RootDescriptor ROOT_DESCRIPTOR = new RootDescriptor();

    private static final Comparator<NodeDescriptor<?>> NODE_COMPARATOR = (o1, o2) -> {
        if (o1 instanceof OrganizationDescriptor od1 && o2 instanceof OrganizationDescriptor od2) {
            return od1.getElement().compareTo(od2.getElement());
        }
        else if (o1 instanceof CertificateDescriptor cd1 && o2 instanceof CertificateDescriptor cd2) {
            String cn1 = cd1.getElement().getSubjectField(COMMON_NAME);
            String cn2 = cd2.getElement().getSubjectField(COMMON_NAME);
            return cn1.compareTo(cn2);
        }
        return 0;
    };

    private final MultiMap<String, CertificateWrapper> myCertificates = new MultiMap<>();

    private final StructureTreeModel<MyTreeStructure> myStructureTreeModel;
    private final Tree myTree;

    public CertificateTreeBuilder(Tree tree) {
        myTree = tree;
        MyTreeStructure treeStructure = new MyTreeStructure();
        myStructureTreeModel = new StructureTreeModel(treeStructure, NODE_COMPARATOR, this);
        AsyncTreeModel asyncTreeModel = new AsyncTreeModel(myStructureTreeModel, this);
        tree.setModel(asyncTreeModel);
    }

    public void reset(Collection<? extends X509Certificate> certificates) {
        myCertificates.clear();
        for (X509Certificate certificate : certificates) {
            addCertificate(certificate);
        }
        // expand organization nodes at the same time
        //initRootNode();
        myStructureTreeModel.invalidateAsync();
        TreeUtil.expandAll(myTree);
    }

    public void addCertificate(X509Certificate certificate) {
        CertificateWrapper wrapper = new CertificateWrapper(certificate);
        myCertificates.putValue(wrapper.getSubjectField(ORGANIZATION), wrapper);
        myStructureTreeModel.invalidateAsync();
    }

    /**
     * Remove specified certificate and corresponding organization, if after removal it contains no certificates.
     */
    public void removeCertificate(X509Certificate certificate) {
        CertificateWrapper wrapper = new CertificateWrapper(certificate);
        myCertificates.remove(wrapper.getSubjectField(ORGANIZATION), wrapper);
        myStructureTreeModel.invalidateAsync();
    }

    public List<X509Certificate> getCertificates() {
        return ContainerUtil.map(myCertificates.values(), CertificateWrapper::getCertificate);
    }

    public boolean isEmpty() {
        return myCertificates.isEmpty();
    }

    public void selectCertificate(X509Certificate certificate) {
        myStructureTreeModel.select(new CertificateWrapper(certificate), myTree, path -> {});
    }

    public void selectFirstCertificate() {
        TreeUtil.promiseSelectFirstLeaf(myTree);
    }

    /**
     * Returns certificates selected in the tree. If organization node is selected, all its certificates
     * will be returned.
     *
     * @return - selected certificates
     */
    public Set<X509Certificate> getSelectedCertificates(boolean addFromOrganization) {
        Set<X509Certificate> selected = new HashSet<>();
        TreeUtil.collectSelectedUserObjects(myTree).forEach(o -> {
            if (o instanceof CertificateDescriptor certDescr) {
                selected.add(certDescr.getElement().getCertificate());
            }
            else if (o instanceof OrganizationDescriptor orgDescr) {
                if (addFromOrganization) {
                    selected.addAll(getCertificatesByOrganization(orgDescr.getElement()));
                }
            }
            else if (o instanceof RootDescriptor) {
                // nop
            }
            else {
                Logger.getInstance(getClass()).error("Unknown tree node object of type: " + o.getClass().getName());
            }
        });
        return selected;
    }

    public @Nullable X509Certificate getFirstSelectedCertificate(boolean addFromOrganization) {
        Set<X509Certificate> certificates = getSelectedCertificates(addFromOrganization);
        return certificates.isEmpty() ? null : certificates.iterator().next();
    }

    public List<X509Certificate> getCertificatesByOrganization(String organizationName) {
        Collection<CertificateWrapper> wrappers = myCertificates.get(organizationName);
        return extract(wrappers);
    }

    @Override
    public void dispose() {
    }

    private static List<X509Certificate> extract(Collection<CertificateWrapper> wrappers) {
        return ContainerUtil.map(wrappers, CertificateWrapper::getCertificate);
    }

    final class MyTreeStructure extends AbstractTreeStructure {
        @Override
        public Object getRootElement() {
            return RootDescriptor.ROOT;
        }

        @Override
        public Object[] getChildElements(Object element) {
            if (element == RootDescriptor.ROOT) {
                return ArrayUtil.toStringArray(myCertificates.keySet());
            }
            else if (element instanceof String key) {
                return ArrayUtil.toObjectArray(myCertificates.get(key));
            }
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public @Nullable Object getParentElement(Object element) {
            if (element == RootDescriptor.ROOT) {
                return null;
            }
            else if (element instanceof String) {
                return RootDescriptor.ROOT;
            }
            return ((CertificateWrapper) element).getSubjectField(ORGANIZATION);
        }

        @Override
        public NodeDescriptor<?> createDescriptor(Object element, NodeDescriptor parentDescriptor) {
            if (element == RootDescriptor.ROOT) {
                return ROOT_DESCRIPTOR;
            }
            else if (element instanceof String key) {
                return new OrganizationDescriptor(parentDescriptor, key);
            }
            return new CertificateDescriptor(parentDescriptor, (CertificateWrapper) element);
        }

        @Override
        public void commit() {
            // do nothing
        }

        @Override
        public boolean hasSomethingToCommit() {
            return false;
        }
    }

    // Auxiliary node descriptors

    abstract static class MyNodeDescriptor<T> extends PresentableNodeDescriptor<T> {
        private final T myObject;

        MyNodeDescriptor(@Nullable NodeDescriptor parentDescriptor, T object) {
            super(parentDescriptor);
            myObject = object;
        }

        @Override
        public T getElement() {
            return myObject;
        }
    }

    static final class RootDescriptor extends MyNodeDescriptor<Object> {
        public static final Object ROOT = new Object();

        private RootDescriptor() {
            super(null, ROOT);
        }

        @Override
        protected void update(PresentationData presentation) {
            presentation.addText(HttpLocalize.labelCertificateRoot(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    static final class OrganizationDescriptor extends MyNodeDescriptor<String> {
        private OrganizationDescriptor(@Nullable NodeDescriptor parentDescriptor, String object) {
            super(parentDescriptor, object);
        }

        @Override
        protected void update(PresentationData presentation) {
            presentation.addText(getElement(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
    }

    static final class CertificateDescriptor extends MyNodeDescriptor<CertificateWrapper> {
        private CertificateDescriptor(@Nullable NodeDescriptor parentDescriptor, CertificateWrapper object) {
            super(parentDescriptor, object);
        }

        @Override
        protected void update(PresentationData presentation) {
            CertificateWrapper wrapper = getElement();
            SimpleTextAttributes attr = wrapper.isValid() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : STRIKEOUT_ATTRIBUTES;
            presentation.addText(wrapper.getSubjectField(COMMON_NAME), attr);
        }
    }
}
