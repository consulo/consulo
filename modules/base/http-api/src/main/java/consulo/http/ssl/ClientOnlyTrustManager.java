package consulo.http.ssl;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
* @author Mikhail Golubev
*/
public interface ClientOnlyTrustManager extends X509TrustManager {
  @Override
  default void checkClientTrusted(X509Certificate[] certificates, String s) throws CertificateException {
    throw new UnsupportedOperationException("Should not be called by client");
  }
}
