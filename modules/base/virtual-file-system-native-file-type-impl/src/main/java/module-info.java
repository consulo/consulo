import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2026-03-31
 */
@NullMarked
module consulo.virtual.file.system.nativefiletype.impl {
    requires consulo.virtual.file.system.api;
    requires format.ripper.jvm.file.type.detector;
}