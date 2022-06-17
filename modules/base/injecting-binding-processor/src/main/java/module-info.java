/**
 * @author VISTALL
 * @since 16-Jun-22
 */
module consulo.injecting.binding.processor {
  requires java.compiler;
  requires consulo.annotation;

  requires com.squareup.javapoet;

  provides javax.annotation.processing.Processor with consulo.internal.injecting.binding.InjectingBindingProcessor;
}