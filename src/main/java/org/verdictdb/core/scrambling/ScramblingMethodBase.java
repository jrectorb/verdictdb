/*
 *    Copyright 2018 University of Michigan
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.verdictdb.core.scrambling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ScramblingMethodBase implements ScramblingMethod {

  protected long blockSize;

  protected final int maxBlockCount;

  protected final double relativeSize; // size relative to original table (0.0 ~ 1.0)

  private final Map<Integer, List<Double>> storedProbDist = new HashMap<>();

  public ScramblingMethodBase(long blockSize, int maxBlockCount, double relativeSize) {
    this.blockSize = blockSize;
    this.maxBlockCount = maxBlockCount;
    this.relativeSize = relativeSize;
  }

  long getBlockSize() {
    return blockSize;
  }

  protected void storeCumulativeProbabilityDistribution(int tier, List<Double> dist) {
    storedProbDist.put(tier, dist);
  }

  @Override
  public List<Double> getStoredCumulativeProbabilityDistributionForTier(int tier) {
    return storedProbDist.get(tier);
  }
}
