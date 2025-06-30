/**
 * @author VISTALL
 * @since 2025-06-30
 */
module consulo.execution.coverage.impl {
    requires consulo.execution.coverage.api;
    requires transitive consulo.xcoverage.rt;
    
    requires it.unimi.dsi.fastutil;
}