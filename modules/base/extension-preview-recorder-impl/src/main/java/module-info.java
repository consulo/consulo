/**
 * @author VISTALL
 * @since 29/04/2023
 */
module consulo.extension.preview.recorder.impl {
    requires consulo.annotation;
    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.compiler.artifact.api;
    requires consulo.component.api;
    requires consulo.container.api;
    requires consulo.execution.api;
    requires consulo.module.content.api;
    requires consulo.module.creation.api;
    requires consulo.project.api;
    requires consulo.task.api;
    requires consulo.util.lang;
    requires consulo.version.control.system.api;
    requires consulo.virtual.file.system.api;
    requires consulo.virtual.file.system.impl;

    exports consulo.extensionPreviewRecorder.impl;
}