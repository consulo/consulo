/**
 * @author VISTALL
 * @since 2025-07-20
 */
module consulo.usage.impl {
    requires consulo.usage.api;

    opens consulo.usage.impl.internal.rule to consulo.util.xml.serializer;
}