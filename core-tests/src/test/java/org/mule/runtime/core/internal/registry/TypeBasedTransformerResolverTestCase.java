/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.TransfromersStory.TRANSFORMERS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.ResolverException;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(REGISTRY)
@Story(TRANSFORMERS)
public class TypeBasedTransformerResolverTestCase extends AbstractMuleTestCase {

  private final MuleContextWithRegistry muleContext = mock(MuleContextWithRegistry.class, RETURNS_DEEP_STUBS);
  private final MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);

  public static class A {
  }

  public static class B {
  }

  private final DataType dataTypeA = fromType(A.class);
  private final DataType dataTypeB = fromType(B.class);

  @Before
  public void setUp() throws Exception {
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
  }

  @Test
  public void doesNotFailIfCannotResolveType() throws ResolverException, TransformerException {
    TransformersRegistry transformersRegistry = mock(TransformersRegistry.class);
    List<Transformer> transformers = new ArrayList<>();
    when(transformersRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);
    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setTransformersRegistry(transformersRegistry);

    Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
    assertNull(resolvedTransformer);
  }

  @Test
  public void resolvesTypeWithOneMatchingTransformer() throws ResolverException, TransformerException {
    TransformersRegistry transformersRegistry = mock(TransformersRegistry.class);
    Transformer aToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).build();

    List<Transformer> transformers = new ArrayList<>();
    transformers.add(aToBConverter);
    when(transformersRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);

    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setTransformersRegistry(transformersRegistry);

    Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
    assertEquals(aToBConverter, resolvedTransformer);
  }

  @Test
  public void resolvesTypeWithTwoMatchingTransformer() throws ResolverException, TransformerException {
    TransformersRegistry transformersRegistry = mock(TransformersRegistry.class);
    Transformer aToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).weighting(1).build();
    Transformer betterAToBConverter = new MockConverterBuilder().from(dataTypeA).to(dataTypeB).weighting(2).build();

    List<Transformer> transformers = new ArrayList<>();
    transformers.add(aToBConverter);
    transformers.add(betterAToBConverter);
    when(transformersRegistry.lookupTransformers(dataTypeA, dataTypeB)).thenReturn(transformers);

    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setTransformersRegistry(transformersRegistry);

    Transformer resolvedTransformer = resolver.resolve(dataTypeA, dataTypeB);
    assertEquals(betterAToBConverter, resolvedTransformer);
  }

  @Test
  public void fallbacksNotRegistered() throws Exception {
    TransformersRegistry transformersRegistry = mock(TransformersRegistry.class);
    TypeBasedTransformerResolver resolver = new TypeBasedTransformerResolver();
    resolver.setTransformersRegistry(transformersRegistry);
    resolver.initialise();

    verify(muleContext, never()).getRegistry();
  }
}
