// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tutorials.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 */
public class DemoCodeInspection extends LocalInspectionTool {
  
  /**
   * This method is overridden to provide a custom visitor
   * The visitor must not be recursive and must be thread-safe.
   *
   * @param holder     object for visitor to register problems found.
   * @param isOnTheFly true if inspection was run in non-batch mode
   * @return DemoInspectionVisitor.
   */
  @NotNull
  @Override
  public DemoInspectionVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new DemoInspectionVisitor();
  }
}
