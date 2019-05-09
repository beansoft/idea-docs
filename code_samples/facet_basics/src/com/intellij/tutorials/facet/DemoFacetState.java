// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tutorials.facet;

import org.jetbrains.annotations.NotNull;

/**
 * A simple class to store state for the DemoFacet.
 * In this case it is just a string containing a path to an SDK.
 *
 * @author John Hake
 */
public class DemoFacetState {
  static final String DEMO_FACET_INIT_PATH = "";

  public String myPathToSdk;
  
  DemoFacetState() {
    setDemoFacetState(DEMO_FACET_INIT_PATH);
  }
  
  @NotNull
  public String getDemoFacetState() {
    return myPathToSdk;
  }
  
  public void setDemoFacetState(@NotNull String newPath) {
    myPathToSdk = newPath;
  }
  
  
}
