
/*
 * Copyright 2023 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jsdynsym.core;

import io.github.ericmedvet.jsdynsym.core.composed.OutStepped;

public class Main {
  public static void main(String[] args) {
    TimeInvariantStatelessSystem<Double, Double> f = x -> x * x;
    System.out.println(f.apply(3d));

    OutStepped<Double, Double, ?> rF = new OutStepped<>(f, 1d);
    OutStepped<Double, Double, ?> rrF = new OutStepped<>(rF, 1.5d);
    for (double t = 0; t < 4d; t = t + 0.25) {
      System.out.printf(
          "t=%5.2f f(t,sin(t))=%5.2f rF(t,sin(t))=%5.2f rrF(t,sin(t))=%5.2f\n",
          t, f.step(t, Math.sin(t)), rF.step(t, Math.sin(t)), rrF.step(t, Math.sin(t)));
    }

    System.out.println(rF.deepest());
    System.out.println(rrF.deepest());

    @SuppressWarnings("unchecked")
    TimeInvariantStatelessSystem<Double, Double> innerTISS =
        rrF.shallowest(TimeInvariantStatelessSystem.class).get();
    System.out.println(innerTISS);
  }
}
