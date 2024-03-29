package consulo.ide.impl.idea.remote;

import consulo.ide.impl.idea.remote.ext.CredentialsManager;
import consulo.ide.impl.idea.util.PathMappingSettings;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author traff
 */
public class RemoteSdkCredentialsHolder extends RemoteCredentialsHolder implements RemoteSdkCredentials {

  @Nonnull
  private final RemoteSdkPropertiesHolder myRemoteSdkProperties;

  public RemoteSdkCredentialsHolder(@Nonnull final String defaultHelpersDirName) {
    myRemoteSdkProperties = new RemoteSdkPropertiesHolder(defaultHelpersDirName);
  }

  public static String constructSshCredentialsSdkFullPath(@Nonnull RemoteSdkCredentials cred) {
    return getCredentialsString(cred) + cred.getInterpreterPath();
  }

  /**
   * Extracts interpreter path from full path generated by method getFullInterpreterPath
   * Returns fullPath as fallback
   * <p/>
   * Based on the statement that host can't contain colon(:) symbol
   */
  public static String getInterpreterPathFromFullPath(String fullPath) {
    if (fullPath.startsWith(SSH_PREFIX)) {
      fullPath = fullPath.substring(SSH_PREFIX.length());
      int index = fullPath.indexOf(":");
      if (index != -1 && index < fullPath.length()) {
        fullPath = fullPath.substring(index + 1); // it is like 8080/home/user or 8080C:\Windows
        index = 0;
        while (index < fullPath.length() && Character.isDigit(fullPath.charAt(index))) {
          index++;
        }
        if (index < fullPath.length()) {
          return fullPath.substring(index);
        }
      }
    }

    return fullPath;
  }


  @Nonnull
  public RemoteSdkPropertiesHolder getRemoteSdkProperties() {
    return myRemoteSdkProperties;
  }

  @Override
  public String getInterpreterPath() {
    return myRemoteSdkProperties.getInterpreterPath();
  }

  @Override
  public void setInterpreterPath(String interpreterPath) {
    myRemoteSdkProperties.setInterpreterPath(interpreterPath);
  }

  @Override
  public String getHelpersPath() {
    return myRemoteSdkProperties.getHelpersPath();
  }

  @Override
  public void setHelpersPath(String helpersPath) {
    myRemoteSdkProperties.setHelpersPath(helpersPath);
  }

  public String getDefaultHelpersName() {
    return myRemoteSdkProperties.getDefaultHelpersName();
  }

  @Override
  public void addRemoteRoot(String remoteRoot) {
    myRemoteSdkProperties.addRemoteRoot(remoteRoot);
  }

  @Override
  public void clearRemoteRoots() {
    myRemoteSdkProperties.clearRemoteRoots();
  }

  @Override
  public List<String> getRemoteRoots() {
    return myRemoteSdkProperties.getRemoteRoots();
  }

  @Override
  public void setRemoteRoots(List<String> remoteRoots) {
    myRemoteSdkProperties.setRemoteRoots(remoteRoots);
  }

  @Nonnull
  @Override
  public PathMappingSettings getPathMappings() {
    return myRemoteSdkProperties.getPathMappings();
  }

  @Override
  public void setPathMappings(@Nullable PathMappingSettings pathMappings) {
    myRemoteSdkProperties.setPathMappings(pathMappings);
  }

  @Override
  public boolean isHelpersVersionChecked() {
    return myRemoteSdkProperties.isHelpersVersionChecked();
  }

  @Override
  public void setHelpersVersionChecked(boolean helpersVersionChecked) {
    myRemoteSdkProperties.setHelpersVersionChecked(helpersVersionChecked);
  }

  @Override
  public String getFullInterpreterPath() {
    return constructSshCredentialsSdkFullPath(this);
  }

  @Override
  public void setSdkId(String sdkId) {
    myRemoteSdkProperties.setSdkId(sdkId);
  }

  @Override
  public String getSdkId() {
    return myRemoteSdkProperties.getSdkId();
  }

  @Override
  public boolean isInitialized() {
    return myRemoteSdkProperties.isInitialized();
  }

  @Override
  public void setInitialized(boolean initialized) {
    myRemoteSdkProperties.setInitialized(initialized);
  }

  @Override
  public boolean isValid() {
    return myRemoteSdkProperties.isValid();
  }

  @Override
  public void setValid(boolean valid) {
    myRemoteSdkProperties.setValid(valid);
  }

  public static boolean isRemoteSdk(@Nullable String path) {
    if (path != null) {
      for (CredentialsType type : CredentialsManager.getInstance().getAllTypes()) {
        if (type.hasPrefix(path)) {
          return true;
        }
      }
    }
    return false;
  }


  @Override
  public void load(Element element) {
    super.load(element);

    myRemoteSdkProperties.load(element);
  }

  @Override
  public void save(Element rootElement) {
    super.save(rootElement);

    myRemoteSdkProperties.save(rootElement);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RemoteSdkCredentialsHolder holder = (RemoteSdkCredentialsHolder)o;

    if (isAnonymous() != holder.isAnonymous()) return false;
    if (getLiteralPort() != null ? !getLiteralPort().equals(holder.getLiteralPort()) : holder.getLiteralPort() != null) return false;
    if (isStorePassphrase() != holder.isStorePassphrase()) return false;
    if (isStorePassword() != holder.isStorePassword()) return false;
    if (isUseKeyPair() != holder.isUseKeyPair()) return false;
    if (getHost() != null ? !getHost().equals(holder.getHost()) : holder.getHost() != null) return false;
    if (getKnownHostsFile() != null ? !getKnownHostsFile().equals(holder.getKnownHostsFile()) : holder.getKnownHostsFile() != null) {
      return false;
    }
    if (getPassphrase() != null ? !getPassphrase().equals(holder.getPassphrase()) : holder.getPassphrase() != null) return false;
    if (getPassword() != null ? !getPassword().equals(holder.getPassword()) : holder.getPassword() != null) return false;
    if (getPrivateKeyFile() != null ? !getPrivateKeyFile().equals(holder.getPrivateKeyFile()) : holder.getPrivateKeyFile() != null) {
      return false;
    }
    if (getUserName() != null ? !getUserName().equals(holder.getUserName()) : holder.getUserName() != null) return false;

    if (!myRemoteSdkProperties.equals(holder.myRemoteSdkProperties)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = getHost() != null ? getHost().hashCode() : 0;
    result = 31 * result + (getLiteralPort() != null ? getLiteralPort().hashCode() : 0);
    result = 31 * result + (isAnonymous() ? 1 : 0);
    result = 31 * result + (getUserName() != null ? getUserName().hashCode() : 0);
    result = 31 * result + (getPassword() != null ? getPassword().hashCode() : 0);
    result = 31 * result + (isUseKeyPair() ? 1 : 0);
    result = 31 * result + (getPrivateKeyFile() != null ? getPrivateKeyFile().hashCode() : 0);
    result = 31 * result + (getKnownHostsFile() != null ? getKnownHostsFile().hashCode() : 0);
    result = 31 * result + (getPassphrase() != null ? getPassphrase().hashCode() : 0);
    result = 31 * result + (isStorePassword() ? 1 : 0);
    result = 31 * result + (isStorePassphrase() ? 1 : 0);
    result = 31 * result + myRemoteSdkProperties.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "RemoteSdkDataHolder" +
           "{getHost()='" +
           getHost() +
           '\'' +
           ", getLiteralPort()=" +
           getLiteralPort() +
           ", isAnonymous()=" +
           isAnonymous() +
           ", getUserName()='" +
           getUserName() +
           '\'' +
           ", myInterpreterPath='" +
           getInterpreterPath() +
           '\'' +
           ", myHelpersPath='" +
           getHelpersPath() +
           '\'' +
           '}';
  }

  public void copyRemoteSdkCredentialsTo(RemoteSdkCredentialsHolder to) {
    super.copyRemoteCredentialsTo(to);
    myRemoteSdkProperties.copyTo(to.getRemoteSdkProperties());
  }
}
