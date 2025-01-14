/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.routing;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.List;

/**
 * An SPI interface where custom logic can be plugged in to control how collections and single messages are returned from routers.
 */
@NoImplement
public interface RouterResultsHandler {

  CoreEvent aggregateResults(List<CoreEvent> results, CoreEvent previous);
}
