/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.req;

import static org.apache.datasketches.QuantileSearchCriteria.INCLUSIVE;
import static org.apache.datasketches.QuantileSearchCriteria.NON_INCLUSIVE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class ReqSketchSortedViewTest {
  private final int k = 32;
  private final boolean hra = false;
  private final int numV = 3;
  private final int dup = 2;
  private final int n = numV * dup;

  @Test
  public void emptySketch() {
    ReqSketch sketch = ReqSketch.builder().build();
    ReqSketchSortedViewIterator itr =  sketch.getSortedView().iterator();
    Assert.assertFalse(itr.next());
  }

  @Test
  public void twoValueSketch() {
    ReqSketch sketch = ReqSketch.builder().build();
    sketch.update(1f);
    sketch.update(2f);
    ReqSketchSortedViewIterator itr =  sketch.getSortedView().iterator();

    assertTrue(itr.next());

    assertEquals(itr.getValue(), 1f);
    assertEquals(itr.getWeight(), 1);
    assertEquals(itr.getCumulativeWeight(NON_INCLUSIVE), 0);
    assertEquals(itr.getCumulativeWeight(INCLUSIVE), 1);
    assertEquals(itr.getNormalizedRank(NON_INCLUSIVE), 0);
    assertEquals(itr.getNormalizedRank(INCLUSIVE), 0.5);

    assertTrue(itr.next());

    assertEquals(itr.getValue(), 2f);
    assertEquals(itr.getWeight(), 1);
    assertEquals(itr.getCumulativeWeight(NON_INCLUSIVE), 1);
    assertEquals(itr.getCumulativeWeight(INCLUSIVE), 2);
    assertEquals(itr.getNormalizedRank(NON_INCLUSIVE), 0.5);
    assertEquals(itr.getNormalizedRank(INCLUSIVE), 1.0);
  }

  //@Test //visual only
  public void checkIterator() {
    println("");
    println("CHECK ReqSketchSortedViewIterator");
    println("  k: " + k + ", hra: " + hra);
    println("  numV: " + numV + ", dup: " + dup + ", Total n = " + n);
    ReqSketch sketch = buildDataLoadSketch();
    checkIterator(sketch);
  }

  private ReqSketch buildDataLoadSketch() {
    float[] fArr = new float[n];
    int h = 0;
    for (int i = 0; i < numV; i++) {
      float flt = (i + 1) * 10;
      for (int j = 1; j <= dup; j++) { fArr[h++] = flt; }
    }
    ReqSketchBuilder bldr = ReqSketch.builder();
    ReqSketch sketch = bldr.build();
    for (int i = 0; i < n; i++) { sketch.update(fArr[i]); }
    return sketch;
  }

  private void checkIterator(final ReqSketch sketch) {
    println("\nNot Deduped:");
    ReqSketchSortedView sv = sketch.getSortedView();
    ReqSketchSortedViewIterator itr = sv.iterator();
    printIterator(itr);
  }

  private void printIterator(final ReqSketchSortedViewIterator itr) {
    println("");
    String[] header = {"Value", "Wt", "CumWtNotInc", "NormRankNotInc", "CumWtInc", "NormRankInc"};
    String hfmt = "%8s%6s%16s%16s%16s%16s\n";
    String fmt = "%8.1f%6d%16d%16.3f%16d%16.3f\n";
    printf(hfmt, (Object[]) header);
    while (itr.next()) {
      float v = itr.getValue();
      long wt = itr.getWeight();
      long cumWtNotInc   = itr.getCumulativeWeight(NON_INCLUSIVE);
      double nRankNotInc = itr.getNormalizedRank(NON_INCLUSIVE);
      long cumWtInc      = itr.getCumulativeWeight(INCLUSIVE);
      double nRankInc    = itr.getNormalizedRank(INCLUSIVE);
      printf(fmt, v, wt, cumWtNotInc, nRankNotInc, cumWtInc, nRankInc);
    }
  }

  private final static boolean enablePrinting = false;

  /**
   * @param format the format
   * @param args the args
   */
  private static final void printf(final String format, final Object ...args) {
    if (enablePrinting) { System.out.printf(format, args); }
  }

  /**
   * @param o the Object to println
   */
  private static final void println(final Object o) {
    if (enablePrinting) { System.out.println(o.toString()); }
  }

}
