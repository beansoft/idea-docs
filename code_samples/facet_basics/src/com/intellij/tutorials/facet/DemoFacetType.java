// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tutorials.facet;

import com.intellij.facet.*;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.*;
import org.jetbrains.annotations.*;

import javax.swing.*;

/**
 * Defines the type, id, and name of the DemoFacet. Provides creation of DemoFacet
 * and associated Configuration.
 * Allows application of this facet to all ModuleTypes.
 *
 * @author Anna Bulenkova
 */
public class DemoFacetType extends FacetType<DemoFacet, DemoFacetConfiguration> {
  public static final String FACET_ID = "DEMO_FACET_ID";
  public static final String FACET_NAME = "SDK Facet";
  public static final FacetTypeId<DemoFacet> DEMO_FACET_TYPE_ID = new FacetTypeId<DemoFacet>(FACET_ID);

  public DemoFacetType() {
    super(DEMO_FACET_TYPE_ID, FACET_ID, FACET_NAME);
  }

  @Override
  public DemoFacetConfiguration createDefaultConfiguration() {
    return new DemoFacetConfiguration();
  }

  @Override
  public DemoFacet createFacet(@NotNull Module module,
                               String s,
                               @NotNull DemoFacetConfiguration configuration,
                               Facet facet) {
    return new DemoFacet(this, module, s, configuration, facet);
  }

  @Override
  public boolean isSuitableModuleType(final ModuleType type) {
    return true;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return AllIcons.General.Information;
  }
}
