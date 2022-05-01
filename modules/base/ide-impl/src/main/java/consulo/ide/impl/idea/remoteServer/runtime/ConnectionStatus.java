package consulo.ide.impl.idea.remoteServer.runtime;

import consulo.ide.impl.idea.openapi.util.text.StringUtil;

/**
 * @author nik
 */
public enum ConnectionStatus {
  DISCONNECTED, CONNECTED, CONNECTING;

  public String getPresentableText() {
    return StringUtil.capitalize(name().toLowerCase());
  }
}
