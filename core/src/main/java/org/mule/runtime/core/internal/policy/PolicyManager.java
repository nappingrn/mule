/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

/**
 * Manager for handling policies in Mule.
 *
 * Implementation of this class will be used to lookup for {@code Policy}s that must be applied to {@code MessageSource}es or
 * {@code Processor}s.
 *
 * @since 4.0
 */
public interface PolicyManager {

  /**
   * Creates a policy to be applied to a source. The creation must have into consideration the {@code sourceIdentifier} to find
   * specific policies applied to that source and also the {@code sourceEvent} which will be used to extract data to match against
   * the policies pointcuts.
   *
   * @param source                                   the source where the policy is being applied.
   * @param sourceEvent                              the event generated from the source.
   * @param messageSourceResponseParametersProcessor processor to generate the response and error response parameters of the
   *                                                 source.
   * @return a {@link SourcePolicy} associated to that source.
   */
  SourcePolicy createSourcePolicyInstance(Component source, CoreEvent sourceEvent,
                                          ReactiveProcessor flowExecutionProcessor,
                                          MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor);

  /**
   * Creates and generates the {@link PolicyPointcutParameters} for the given {@code source} and {@code attributes}, and adds it
   * as an internal parameter of the event to be built with {@code eventBuilder}.
   *
   * @return the created source parameters.
   */
  PolicyPointcutParameters addSourcePointcutParametersIntoEvent(Component source, TypedValue<?> attributes,
                                                                InternalEvent event);

  /**
   * Creates a policy to be applied to an operation. The creation must have into consideration the {@code operationIdentifier} to
   * find specific policies applied to that operation and also the {@code operationParameters} which will be used to extract data
   * to match against the policies pointcuts.
   *
   * @param operation           the operation where the policy is being applied.
   * @param operationEvent      the event used to execute the operation.
   * @param operationParameters the set of parameters to use to execute the operation.
   * @return a {@link OperationPolicy} associated to that source.
   */
  OperationPolicy createOperationPolicy(Component operation, CoreEvent operationEvent,
                                        OperationParametersProcessor operationParameters);


}
