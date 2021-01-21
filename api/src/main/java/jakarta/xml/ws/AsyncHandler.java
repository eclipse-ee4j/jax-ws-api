/*
 * Copyright (c) 2005, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package jakarta.xml.ws;

/** The {@code AsyncHandler} interface is implemented by
 * clients that wish to receive callback notification of the completion of
 * service endpoint operations invoked asynchronously.
 *
 * @param <T> The type of the message or payload
 *  @since 1.6, JAX-WS 2.0
**/
@FunctionalInterface
public interface AsyncHandler<T> {

    /** Called when the response to an asynchronous operation is available.
     *
     * @param res The response to the operation invocation.
     *
    **/
    void handleResponse(Response<T> res);
}
