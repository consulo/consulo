package consulo.remoteServer.runtime;

import consulo.util.lang.StringUtil;

import java.util.Locale;

/**
 * @author nik
 */
public enum ConnectionStatus {
  DISCONNECTED, CONNECTED, CONNECTING;

  public String getPresentableText() {
    return StringUtil.capitalize(name().toLowerCase(Locale.ROOT));
  }
}
