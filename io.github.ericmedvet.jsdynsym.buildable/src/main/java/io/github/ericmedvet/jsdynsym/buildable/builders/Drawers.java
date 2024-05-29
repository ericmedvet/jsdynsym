/*-
 * ========================LICENSE_START=================================
 * jsdynsym-buildable
 * %%
 * Copyright (C) 2023 - 2024 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.PointNavigationDrawer;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.drawer|d")
public class Drawers {
  private Drawers() {}

  @SuppressWarnings("unused")
  public static NavigationDrawer navigation() {
    return new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
  }

  @SuppressWarnings("unused")
  public static PointNavigationDrawer pointNavigation() {
    return new PointNavigationDrawer(PointNavigationDrawer.Configuration.DEFAULT);
  }
}
