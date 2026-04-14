/**
 * @author VISTALL
 * @since 29/04/2023
 */
module consulo.extension.preview.recorder.impl {
  requires consulo.ide.api;
    requires consulo.annotation;
    requires consulo.compiler.artifact.api;
    requires consulo.task.api;

    exports consulo.extensionPreviewRecorder.impl;
}