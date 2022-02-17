/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.base.localize.library {
  requires consulo.localize.api;

  exports consulo.platform.base.localize;
  exports consulo.editor.ui.api.localize;
  exports consulo.vcs.api.localize;
}