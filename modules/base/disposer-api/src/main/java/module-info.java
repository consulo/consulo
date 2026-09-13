import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("module")
module consulo.disposer.api {
    requires transitive consulo.annotation;

    exports consulo.disposer;
    exports consulo.disposer.util;

    exports consulo.disposer.internal to
        consulo.application.impl,
        consulo.disposer.impl;

    uses consulo.disposer.internal.DisposerInternal;
    uses consulo.disposer.internal.DiposerRegisterChecker;
}