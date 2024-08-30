package consulo.http.impl.internal.ssl;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static consulo.http.impl.internal.ssl.CertificateWrapper.CommonField.COMMON_NAME;
import static consulo.http.impl.internal.ssl.CertificateWrapper.CommonField.ORGANIZATION;

/**
 * @author Mikhail Golubev
 */
public class CertificateTreeBuilder extends AbstractTreeBuilder {
    private static final SimpleTextAttributes STRIKEOUT_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_STRIKEOUT, null);
    private static final RootDescriptor ROOT_DESCRIPTOR = new RootDescriptor();

    private final MultiMap<String, CertificateWrapper> myCertificates = new MultiMap<String, CertificateWrapper>();

    public CertificateTreeBuilder(@Nonnull Tree tree) {
        init(tree, new DefaultTreeModel(new DefaultMutableTreeNode()), new MyTreeStructure(), new Comparator<NodeDescriptor>() {
            @Override
            public int compare(NodeDescriptor o1, NodeDescriptor o2) {
                if (o1 instanceof OrganizationDescriptor && o2 instanceof OrganizationDescriptor) {
                    return ((String)o1.getElement()).compareTo((String)o2.getElement());
                }
                else if (o1 instanceof CertificateDescriptor && o2 instanceof CertificateDescriptor) {
                    String cn1 = ((CertificateDescriptor)o1).getElement().getSubjectField(COMMON_NAME);
                    String cn2 = ((CertificateDescriptor)o2).getElement().getSubjectField(COMMON_NAME);
                    return cn1.compareTo(cn2);
                }
                return 0;
            }
        }, true);
        initRootNode();
    }

    public void reset(@Nonnull Collection<X509Certificate> certificates) {
        myCertificates.clear();
        for (X509Certificate certificate : certificates) {
            addCertificate(certificate);
        }
        // expand organization nodes at the same time
        //initRootNode();
        queueUpdateFrom(RootDescriptor.ROOT, true).doWhenDone(new Runnable() {
            @Override
            public void run() {
                CertificateTreeBuilder.this.expandAll(null);
            }
        });
    }

    public void addCertificate(@Nonnull X509Certificate certificate) {
        CertificateWrapper wrapper = new CertificateWrapper(certificate);
        myCertificates.putValue(wrapper.getSubjectField(ORGANIZATION), wrapper);
        queueUpdateFrom(RootDescriptor.ROOT, true);
    }

    /**
     * Remove specified certificate and corresponding organization, if after removal it contains no certificates.
     */
    public void removeCertificate(@Nonnull X509Certificate certificate) {
        CertificateWrapper wrapper = new CertificateWrapper(certificate);
        myCertificates.remove(wrapper.getSubjectField(ORGANIZATION), wrapper);
        queueUpdateFrom(RootDescriptor.ROOT, true);
    }

    public List<X509Certificate> getCertificates() {
        return ContainerUtil.map(
            myCertificates.values(),
            (Function<CertificateWrapper, X509Certificate>)wrapper -> wrapper.getCertificate()
        );
    }

    public boolean isEmpty() {
        return myCertificates.isEmpty();
    }

    public void selectCertificate(@Nonnull X509Certificate certificate) {
        select(new CertificateWrapper(certificate));
    }

    public void selectFirstCertificate() {
        if (!isEmpty()) {
            Tree tree = (Tree)getTree();
            TreePath path = TreeUtil.getFirstLeafNodePath(tree);
            tree.addSelectionPath(path);
        }
    }

    /**
     * Returns certificates selected in the tree. If organization node is selected, all its certificates
     * will be returned.
     *
     * @return - selected certificates
     */
    @Nonnull
    public Set<X509Certificate> getSelectedCertificates(boolean addFromOrganization) {
        Set<X509Certificate> selected = getSelectedElements(X509Certificate.class);
        if (addFromOrganization) {
            for (String s : getSelectedElements(String.class)) {
                selected.addAll(getCertificatesByOrganization(s));
            }
        }
        return selected;
    }

    @Nullable
    public X509Certificate getFirstSelectedCertificate(boolean addFromOrganization) {
        Set<X509Certificate> certificates = getSelectedCertificates(addFromOrganization);
        return certificates.isEmpty() ? null : certificates.iterator().next();
    }

    @Nonnull
    public List<X509Certificate> getCertificatesByOrganization(@Nonnull String organizationName) {
        Collection<CertificateWrapper> wrappers = myCertificates.get(organizationName);
        return extract(wrappers);
    }

    private static List<X509Certificate> extract(Collection<CertificateWrapper> wrappers) {
        return ContainerUtil.map(wrappers, wrapper -> wrapper.getCertificate());
    }

    @Override
    protected Object transformElement(Object object) {
        if (object instanceof CertificateWrapper) {
            return ((CertificateWrapper)object).getCertificate();
        }
        return object;
    }

    @Override
    protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
        return super.isAutoExpandNode(nodeDescriptor) || nodeDescriptor instanceof OrganizationDescriptor;
    }

    class MyTreeStructure extends AbstractTreeStructure {
        @Override
        public Object getRootElement() {
            return RootDescriptor.ROOT;
        }

        @Override
        public Object[] getChildElements(Object element) {
            if (element == RootDescriptor.ROOT) {
                return ArrayUtil.toStringArray(myCertificates.keySet());
            }
            else if (element instanceof String) {
                return ArrayUtil.toObjectArray(myCertificates.get((String)element));
            }
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        @Nullable
        @Override
        public Object getParentElement(Object element) {
            if (element == RootDescriptor.ROOT) {
                return null;
            }
            else if (element instanceof String) {
                return RootDescriptor.ROOT;
            }
            return ((CertificateWrapper)element).getSubjectField(ORGANIZATION);
        }

        @Nonnull
        @Override
        public NodeDescriptor createDescriptor(Object element, NodeDescriptor parentDescriptor) {
            if (element == RootDescriptor.ROOT) {
                return ROOT_DESCRIPTOR;
            }
            else if (element instanceof String) {
                return new OrganizationDescriptor(parentDescriptor, (String)element);
            }
            return new CertificateDescriptor(parentDescriptor, (CertificateWrapper)element);
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

    static abstract class MyNodeDescriptor<T> extends PresentableNodeDescriptor<T> {
        private final T myObject;

        MyNodeDescriptor(@Nullable NodeDescriptor parentDescriptor, @Nonnull T object) {
            super(parentDescriptor);
            myObject = object;
        }

        @Override
        public T getElement() {
            return myObject;
        }
    }

    static class RootDescriptor extends MyNodeDescriptor<Object> {
        public static final Object ROOT = new Object();

        private RootDescriptor() {
            super(null, ROOT);
        }

        @Override
        protected void update(PresentationData presentation) {
            presentation.addText("<root>", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    static class OrganizationDescriptor extends MyNodeDescriptor<String> {
        private OrganizationDescriptor(@Nullable NodeDescriptor parentDescriptor, @Nonnull String object) {
            super(parentDescriptor, object);
        }

        @Override
        protected void update(PresentationData presentation) {
            presentation.addText(getElement(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
    }

    static class CertificateDescriptor extends MyNodeDescriptor<CertificateWrapper> {
        private CertificateDescriptor(@Nullable NodeDescriptor parentDescriptor, @Nonnull CertificateWrapper object) {
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
