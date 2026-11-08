/**
 * The packages editor - unified ui over {@link consulo.repository.ui.PackageManagementService}. Nothing is exported:
 * plugins use consulo.repository.ui.api only.
 */
module consulo.repository.ui.impl {
    requires consulo.repository.ui.api;
    requires consulo.file.editor.api;
    requires consulo.ui.ex.api;
    requires consulo.language.api;
}
