/**
 * @author VISTALL
 * @since 2026-01-29
 */
module consulo.util.concurrent.coroutine {
    requires consulo.util.concurrent;
    requires consulo.util.dataholder;
    requires consulo.util.collection;
    requires consulo.annotation;

    requires org.slf4j;

    exports consulo.util.concurrent.coroutine.step;
    exports consulo.util.concurrent.coroutine.step.nio;
    exports consulo.util.concurrent.coroutine;
}