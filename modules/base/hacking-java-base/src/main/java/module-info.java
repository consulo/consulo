import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("module")
module consulo.hacking.java.base {
    requires consulo.logging.api;

    exports consulo.hacking.java.base to
        consulo.component.api,
        consulo.disposer.impl;
}