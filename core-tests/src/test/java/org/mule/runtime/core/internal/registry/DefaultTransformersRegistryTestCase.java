/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.TransfromersStory.TRANSFORMERS;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.BloodOrange;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.Peach;
import org.mule.tck.testmodels.fruit.Seed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(REGISTRY)
@Story(TRANSFORMERS)
public class DefaultTransformersRegistryTestCase extends AbstractMuleContextTestCase {

  private static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);
  private static final DataType BLOOD_ORANGE_DATA_TYPE = DataType.fromType(BloodOrange.class);
  private static final DataType FRUIT_DATA_TYPE = DataType.fromType(Fruit.class);

  private static final DataType PEACH_DATA_TYPE = DataType.fromType(Peach.class);
  private static final DataType SEED_DATA_TYPE = DataType.fromType(Seed.class);
  private static final DataType APPLE_DATA_TYPE = DataType.fromType(Apple.class);
  private static final DataType BANANA_DATA_TYPE = DataType.fromType(Banana.class);

  private Transformer t1;
  private Transformer t2;

  private TransformersRegistry transformersRegistry;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    t1 = new MockConverterBuilder().named("t1").from(ORANGE_DATA_TYPE).to(FRUIT_DATA_TYPE).build();
    t2 = new MockConverterBuilder().named("t2").from(DataType.OBJECT).to(FRUIT_DATA_TYPE).build();

    Map<String, Object> startupRegistry = new HashMap<>();
    startupRegistry.put(t1.getName(), t1);
    startupRegistry.put(t2.getName(), t2);
    return startupRegistry;
  }

  @Before
  public void setUp() throws Exception {
    transformersRegistry = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(TransformersRegistry.class);
  }

  @Test
  public void lookupsTransformersByType() throws Exception {
    List<Transformer> trans = transformersRegistry.lookupTransformers(BLOOD_ORANGE_DATA_TYPE, FRUIT_DATA_TYPE);
    assertEquals(2, trans.size());
    assertTrue(trans.contains(t1));
    assertTrue(trans.contains(t2));
  }

  @Test
  public void lookupsTransformerByPriority() throws Exception {
    Transformer result = transformersRegistry.lookupTransformer(BLOOD_ORANGE_DATA_TYPE, FRUIT_DATA_TYPE);
    assertNotNull(result);
    assertEquals(t1, result);
  }

  @Test
  public void findsCompositeTransformerEvenIfDirectNotFound() throws Exception {
    Transformer fruitToSeed = new MockConverterBuilder().named("fruitToSeed").from(FRUIT_DATA_TYPE).to(SEED_DATA_TYPE).build();
    Transformer seedToApple = new MockConverterBuilder().named("seedToApple").from(SEED_DATA_TYPE).to(APPLE_DATA_TYPE).build();
    Transformer appleToBanana =
        new MockConverterBuilder().named("appleToBanana").from(APPLE_DATA_TYPE).to(BANANA_DATA_TYPE).build();
    Transformer bananaToBloodOrange =
        new MockConverterBuilder().named("bananaToBloodOrange").from(BANANA_DATA_TYPE).to(BLOOD_ORANGE_DATA_TYPE).build();
    transformersRegistry.registerTransformer(fruitToSeed);
    transformersRegistry.registerTransformer(seedToApple);
    transformersRegistry.registerTransformer(appleToBanana);
    transformersRegistry.registerTransformer(bananaToBloodOrange);

    Transformer trans = transformersRegistry.lookupTransformer(FRUIT_DATA_TYPE, BLOOD_ORANGE_DATA_TYPE);
    assertThat(trans, is(notNullValue()));
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));

    // The same should be returned if we ask for it with compatible data types
    trans = transformersRegistry.lookupTransformer(FRUIT_DATA_TYPE, ORANGE_DATA_TYPE);
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));

    trans = transformersRegistry.lookupTransformer(PEACH_DATA_TYPE, BLOOD_ORANGE_DATA_TYPE);
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));

    trans = transformersRegistry.lookupTransformer(PEACH_DATA_TYPE, ORANGE_DATA_TYPE);
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));
  }

  @Test
  public void closestToTypesTransformerIsFoundEvenIfWeightIsLess() throws Exception {
    Transformer bananaToBloodOrange = new MockConverterBuilder().named("bananaToBloodOrange").from(BANANA_DATA_TYPE)
        .to(BLOOD_ORANGE_DATA_TYPE).weighting(10).build();
    Transformer bananaToOrange =
        new MockConverterBuilder().named("bananaToOrange").from(BANANA_DATA_TYPE).to(ORANGE_DATA_TYPE).weighting(1).build();
    TransformersRegistry transfromersRegistry =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(TransformersRegistry.class);
    transfromersRegistry.registerTransformer(bananaToBloodOrange);
    transfromersRegistry.registerTransformer(bananaToOrange);

    Transformer trans =
        transformersRegistry.lookupTransformer(BANANA_DATA_TYPE, ORANGE_DATA_TYPE);

    assertThat(trans, is(notNullValue()));
    assertThat(trans.getName(), is("bananaToOrange"));
  }

}
