/*****************************************************************************
 * Copyright (c) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package consulo.component.impl.internal.inject;

/**
 * Subclass of {@link PicoException} that is thrown when there is a problem registering a component with the container
 * or another part of the PicoContainer API, for example, when a request for a component is ambiguous.
 *
 * @version $Revision$
 * @since 1.0
 */
class PicoRegistrationException extends PicoException {

  /**
   * Construct a new exception with no cause and the specified detail message.  Note modern JVMs may still track the
   * exception that caused this one.
   *
   * @param message the message detailing the exception.
   */
  public PicoRegistrationException(String message) {
    super(message);
  }

  /**
   * Construct a new exception with the specified cause and no detail message.
   *
   * @param cause the exception that caused this one.
   */
  protected PicoRegistrationException(Throwable cause) {
    super(cause);
  }

  /**
   * Construct a new exception with the specified cause and the specified detail message.
   *
   * @param message the message detailing the exception.
   * @param cause   the exception that caused this one.
   */
  public PicoRegistrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
