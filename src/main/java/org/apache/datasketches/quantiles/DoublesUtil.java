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

package org.apache.datasketches.quantiles;

import static org.apache.datasketches.common.Util.LS;
import static org.apache.datasketches.quantiles.ClassicUtil.DOUBLES_SER_VER;
import static org.apache.datasketches.quantiles.ClassicUtil.computeCombinedBufferItemCapacity;
import static org.apache.datasketches.quantiles.ClassicUtil.computeNumLevelsNeeded;
import static org.apache.datasketches.quantiles.ClassicUtil.computeTotalLevels;
import static org.apache.datasketches.quantiles.ClassicUtil.computeValidLevels;
import static org.apache.datasketches.quantiles.ClassicUtil.getNormalizedRankError;

import java.util.Arrays;

import org.apache.datasketches.common.SketchesArgumentException;
import org.apache.datasketches.memory.Memory;

/**
 * Utilities that support the doubles quantiles algorithms.
 *
 * <p>This class contains a highly specialized sort called blockyTandemMergeSort().
 * It also contains methods that are used while building histograms and other common
 * functions.</p>
 *
 * @author Lee Rhodes
 */
final class DoublesUtil {

  private DoublesUtil() {}

  /**
   * Returns an on-heap copy of the given sketch
   * @param sketch the given sketch
   * @return a copy of the given sketch
   */
  static HeapUpdateDoublesSketch copyToHeap(final DoublesSketch sketch) {
    final HeapUpdateDoublesSketch qsCopy;
    qsCopy = HeapUpdateDoublesSketch.newInstance(sketch.getK());
    qsCopy.putN(sketch.getN());
    qsCopy.putMinItem(sketch.isEmpty() ? Double.NaN : sketch.getMinItem());
    qsCopy.putMaxItem(sketch.isEmpty() ? Double.NaN : sketch.getMaxItem());
    qsCopy.putBaseBufferCount(sketch.getBaseBufferCount());
    qsCopy.putBitPattern(sketch.getBitPattern());

    if (sketch.isCompact()) {
      final int combBufItems = computeCombinedBufferItemCapacity(sketch.getK(), sketch.getN());
      final double[] combBuf = new double[combBufItems];
      qsCopy.putCombinedBuffer(combBuf);
      final DoublesSketchAccessor sketchAccessor = DoublesSketchAccessor.wrap(sketch);
      final DoublesSketchAccessor copyAccessor = DoublesSketchAccessor.wrap(qsCopy);
      // start with BB
      copyAccessor.putArray(sketchAccessor.getArray(0, sketchAccessor.numItems()),
              0, 0, sketchAccessor.numItems());

      long bitPattern = sketch.getBitPattern();
      for (int lvl = 0; bitPattern != 0L; ++lvl, bitPattern >>>= 1) {
        if ((bitPattern & 1L) > 0L) {
          sketchAccessor.setLevel(lvl);
          copyAccessor.setLevel(lvl);
          copyAccessor.putArray(sketchAccessor.getArray(0, sketchAccessor.numItems()),
                  0, 0, sketchAccessor.numItems());
        }
      }
    } else {
      final double[] combBuf = sketch.getCombinedBuffer();
      qsCopy.putCombinedBuffer(Arrays.copyOf(combBuf, combBuf.length));
    }
    return qsCopy;
  }

  /**
   * Check the validity of the given serialization version
   * @param serVer the given serialization version
   * @param minSupportedSerVer the oldest serialization version supported
   */
  static void checkDoublesSerVer(final int serVer, final int minSupportedSerVer) {
    final int max = DOUBLES_SER_VER;
    if ((serVer > max) || (serVer < minSupportedSerVer)) {
      throw new SketchesArgumentException(
          "Possible corruption: Unsupported Serialization Version: " + serVer);
    }
  }

  static String toString(final boolean sketchSummary, final boolean dataDetail,
      final DoublesSketch sketch) {
    final StringBuilder sb = new StringBuilder();
    if (dataDetail) {
      sb.append(getDataDetail(sketch));
    }
    if (sketchSummary) {
      sb.append(getSummary(sketch));
    }
    return sb.toString();
  }

  static String memToString(final boolean sketchSummary, final boolean dataDetail,
      final Memory mem) {
    final DoublesSketch ds = DoublesSketch.heapify(mem);
    return ds.toString(sketchSummary, dataDetail);
  }

  private static String getSummary(final DoublesSketch sk) {
    final StringBuilder sb = new StringBuilder();
    final String thisSimpleName = sk.getClass().getSimpleName();
    final int k = sk.getK();
    final String kStr = String.format("%,d", k);
    final long n = sk.getN();
    final String nStr = String.format("%,d", n);
    final String bbCntStr = String.format("%,d", sk.getBaseBufferCount());
    final String combBufCapStr = String.format("%,d", sk.getCombinedBufferItemCapacity());
    final long bitPattern = sk.getBitPattern();
    final int neededLevels = computeNumLevelsNeeded(k, n);
    final int totalLevels = computeTotalLevels(bitPattern);
    final int validLevels = computeValidLevels(bitPattern);
    final String retItemsStr = String.format("%,d", sk.getNumRetained());
    final String cmptBytesStr = String.format("%,d", sk.getCurrentCompactSerializedSizeBytes());
    final String updtBytesStr = String.format("%,d", sk.getCurrentUpdatableSerializedSizeBytes());
    final double epsPmf = getNormalizedRankError(k, true);
    final String epsPmfPctStr = String.format("%.3f%%", epsPmf * 100.0);
    final double eps =  getNormalizedRankError(k, false);
    final String epsPctStr = String.format("%.3f%%", eps * 100.0);
    final String memCap = sk.hasMemory() ? Long.toString(sk.getMemory().getCapacity()) : "";
    final double minItem = sk.isEmpty() ? Double.NaN : sk.getMinItem();
    final double maxItem = sk.isEmpty() ? Double.NaN : sk.getMaxItem();

    sb.append(LS).append("### Quantiles ").append(thisSimpleName).append(" SUMMARY: ")
      .append(LS);
    sb.append("   Empty                        : ").append(sk.isEmpty()).append(LS);
    sb.append("   Memory, Capacity bytes       : ").append(sk.hasMemory())
      .append(", ").append(memCap).append(LS);
    sb.append("   Estimation Mode              : ").append(sk.isEstimationMode()).append(LS);
    sb.append("   K                            : ").append(kStr).append(LS);
    sb.append("   N                            : ").append(nStr).append(LS);
    sb.append("   Levels (Needed, Total, Valid): ")
      .append(neededLevels + ", " + totalLevels + ", " + validLevels).append(LS);
    sb.append("   Level Bit Pattern            : ")
      .append(Long.toBinaryString(bitPattern)).append(LS);
    sb.append("   BaseBufferCount              : ").append(bbCntStr).append(LS);
    sb.append("   Combined Buffer Capacity     : ").append(combBufCapStr).append(LS);
    sb.append("   Retained Items               : ").append(retItemsStr).append(LS);
    sb.append("   Compact Storage Bytes        : ").append(cmptBytesStr).append(LS);
    sb.append("   Updatable Storage Bytes      : ").append(updtBytesStr).append(LS);
    sb.append("   Normalized Rank Error        : ").append(epsPctStr).append(LS);
    sb.append("   Normalized Rank Error (PMF)  : ").append(epsPmfPctStr).append(LS);
    sb.append("   Min Item                     : ")
      .append(String.format("%12.6e", minItem)).append(LS);
    sb.append("   Max Item                     : ")
      .append(String.format("%12.6e", maxItem)).append(LS);
    sb.append("### END SKETCH SUMMARY").append(LS);
    return sb.toString();
  }

  private static String getDataDetail(final DoublesSketch sketchIn) {
    final DoublesSketch sketch = sketchIn.isCompact() ? copyToHeap(sketchIn) : sketchIn;
    final StringBuilder sb = new StringBuilder();
    final String skName = sketch.getClass().getSimpleName();
    sb.append(LS).append("### Quantiles ").append(skName).append(" DATA DETAIL: ").append(LS);

    final int k = sketch.getK();
    final long n = sketch.getN();
    final int bbCount = sketch.getBaseBufferCount();
    final long bitPattern = sketch.getBitPattern();
    final double[] combBuf = sketch.getCombinedBuffer();

    //output the base buffer

    sb.append("   BaseBuffer   : ");
    for (int i = 0; i < bbCount; i++) {
      sb.append(String.format("%10.1f", combBuf[i]));
    }
    sb.append(LS);

    //output all the levels
    final int combBufSize = combBuf.length;
    if (n >= (2 * k)) {
      sb.append("   Valid | Level");
      for (int j = 2 * k; j < combBufSize; j++) { //output level data starting at 2K
        if ((j % k) == 0) { //start output of new level
          final int levelNum = (j / k) - 2;
          final String validLvl = ((1L << levelNum) & bitPattern) > 0 ? "    T  " : "    F  ";
          final String lvl = String.format("%5d", levelNum);
          sb.append(LS).append("   ").append(validLvl).append(" ").append(lvl).append(": ");
        }
        sb.append(String.format("%10.1f", combBuf[j]));
      }
      sb.append(LS);
    }
    sb.append("### END DATA DETAIL").append(LS);
    return sb.toString();
  }

}
