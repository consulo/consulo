package consulo.http.impl.internal.ssl;

import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import org.jspecify.annotations.Nullable;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import static consulo.http.impl.internal.ssl.CertificateWrapper.CommonField.COMMON_NAME;
import static consulo.http.impl.internal.ssl.CertificateWrapper.CommonField.ORGANIZATION;

/**
 * Certificates grouped by the organization of their subject. Tree values are the organization names ({@link String})
 * and the certificates themselves ({@link CertificateWrapper}).
 *
 * @author Mikhail Golubev
 */
public class CertificateTreeModel implements TreeModel<Object> {
    private static final TextAttribute STRIKEOUT_ATTRIBUTES = TextAttribute.REGULAR.withEffect(TextEffect.STRIKEOUT);
    private static final Comparator<CertificateWrapper> BY_COMMON_NAME = Comparator.comparing(it -> it.getSubjectField(COMMON_NAME));

    private final Map<String, List<CertificateWrapper>> myCertificates = new TreeMap<>();

    public void reset(Collection<X509Certificate> certificates) {
        myCertificates.clear();
        for (X509Certificate certificate : certificates) {
            addCertificate(certificate);
        }
    }

    public CertificateWrapper addCertificate(X509Certificate certificate) {
        CertificateWrapper wrapper = new CertificateWrapper(certificate);
        List<CertificateWrapper> list = myCertificates.computeIfAbsent(wrapper.getSubjectField(ORGANIZATION), it -> new ArrayList<>());
        if (!list.contains(wrapper)) {
            list.add(wrapper);
            list.sort(BY_COMMON_NAME);
        }
        return wrapper;
    }

    /**
     * Remove specified certificate and corresponding organization, if after removal it contains no certificates.
     */
    public void removeCertificate(X509Certificate certificate) {
        CertificateWrapper wrapper = new CertificateWrapper(certificate);
        String organization = wrapper.getSubjectField(ORGANIZATION);
        List<CertificateWrapper> list = myCertificates.get(organization);
        if (list != null) {
            list.remove(wrapper);
            if (list.isEmpty()) {
                myCertificates.remove(organization);
            }
        }
    }

    public boolean isEmpty() {
        return myCertificates.isEmpty();
    }

    public @Nullable CertificateWrapper getFirstCertificate() {
        for (List<CertificateWrapper> list : myCertificates.values()) {
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        return null;
    }

    /**
     * Returns certificates of the given tree values. For an organization value all its certificates are returned.
     */
    public Set<X509Certificate> getCertificates(Collection<?> values) {
        Set<X509Certificate> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (value instanceof CertificateWrapper wrapper) {
                result.add(wrapper.getCertificate());
            }
            else if (value instanceof String organization) {
                for (CertificateWrapper wrapper : myCertificates.getOrDefault(organization, List.of())) {
                    result.add(wrapper.getCertificate());
                }
            }
        }
        return result;
    }

    @Override
    public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
        if (parentValue == null) {
            for (String organization : myCertificates.keySet()) {
                TreeNode<Object> node = nodeFactory.apply(organization);
                node.setLeaf(false);
                node.setRenderer(CertificateTreeModel::render);
            }
        }
        else if (parentValue instanceof String organization) {
            for (CertificateWrapper wrapper : myCertificates.getOrDefault(organization, List.of())) {
                TreeNode<Object> node = nodeFactory.apply(wrapper);
                node.setLeaf(true);
                node.setRenderer(CertificateTreeModel::render);
            }
        }
    }

    private static void render(Object value, TextItemPresentation presentation) {
        if (value instanceof String organization) {
            presentation.append(LocalizeValue.of(organization), TextAttribute.REGULAR_BOLD);
        }
        else if (value instanceof CertificateWrapper wrapper) {
            TextAttribute attributes = wrapper.isValid() ? TextAttribute.REGULAR : STRIKEOUT_ATTRIBUTES;
            presentation.append(LocalizeValue.of(wrapper.getSubjectField(COMMON_NAME)), attributes);
        }
    }
}
