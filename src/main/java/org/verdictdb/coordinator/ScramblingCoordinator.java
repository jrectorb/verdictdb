package org.verdictdb.coordinator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.core.execplan.ExecutablePlanRunner;
import org.verdictdb.core.scrambling.FastConvergeScramblingMethod;
import org.verdictdb.core.scrambling.ScrambleMeta;
import org.verdictdb.core.scrambling.ScramblingMethod;
import org.verdictdb.core.scrambling.ScramblingPlan;
import org.verdictdb.core.scrambling.UniformScramblingMethod;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.exception.VerdictDBValueException;

import com.google.common.base.Optional;

public class ScramblingCoordinator {

  private final Set<String> scramblingMethods = new HashSet<>(Arrays.asList("uniform", "fastconverge"));

  // default options
  private final Map<String, String> options = new HashMap<String, String>() {
    private static final long serialVersionUID = -4491518418086939738L;
    {
      put("tierColumnName", "verdictdbtier");
      put("blockColumnName", "verdictdbblock");
      put("scrambleTableSuffix", "_scrambled");
      put("scrambleTableBlockSize", "1e6");
    }
  };

  Optional<String> scrambleSchema;

  DbmsConnection conn;

  Optional<String> scratchpadSchema;

  public ScramblingCoordinator(DbmsConnection conn) {
    this(conn, null);
  }

  public ScramblingCoordinator(DbmsConnection conn, String scrambleSchema) {
    this(conn, scrambleSchema, scrambleSchema);           // uses the same schema
  }

  public ScramblingCoordinator(DbmsConnection conn, String scrambleSchema, String scratchpadSchema) {
    this(conn, scrambleSchema, scratchpadSchema, null);
  }

  public ScramblingCoordinator(DbmsConnection conn, String scrambleSchema, String scratchpadSchema, Long blockSize) {
    this.conn = conn;
    this.scratchpadSchema = Optional.fromNullable(scratchpadSchema);
    this.scrambleSchema = Optional.fromNullable(scrambleSchema);
    if (blockSize != null) {
      options.put("scrambleTableBlockSize", String.valueOf(blockSize));
    }
  }

  public ScrambleMeta scramble(String originalSchema, String originalTable) throws VerdictDBException {
    String newSchema;
    if (scrambleSchema.isPresent()) {
      newSchema = scrambleSchema.get();
    } else {
      newSchema = originalSchema;
    }
    String newTable = originalTable + options.get("scrambleTableSuffix");
    ScrambleMeta meta = scramble(originalSchema, originalTable, newSchema, newTable);
    return meta;
  }

  public ScrambleMeta scramble(
      String originalSchema, String originalTable, 
      String newSchema, String newTable) throws VerdictDBException {

    String methodName = "uniform";
    String primaryColumn = null;
    ScrambleMeta meta = 
        scramble(originalSchema, originalTable, newSchema, newTable, methodName, primaryColumn);
    return meta;
  }

  public ScrambleMeta scramble(
      String originalSchema, String originalTable, 
      String newSchema, String newTable,
      String methodName) throws VerdictDBException {

    String primaryColumn = null;
    ScrambleMeta meta = 
        scramble(originalSchema, originalTable, newSchema, newTable, methodName, primaryColumn);
    return meta;
  }

  public ScrambleMeta scramble(
      String originalSchema, String originalTable, 
      String newSchema, String newTable,
      String methodName, String primaryColumn) throws VerdictDBException {

    // copied options
    Map<String, String> customOptions = new HashMap<>(options);

    ScrambleMeta meta = 
        scramble(originalSchema, originalTable, newSchema, newTable, methodName, primaryColumn, customOptions);
    return meta;

  }

  public ScrambleMeta scramble(
      String originalSchema, String originalTable,
      String newSchema, String newTable,
      String methodName, String primaryColumn, Map<String, String> customOptions) throws VerdictDBException {

    // sanity check
    if (!scramblingMethods.contains(methodName.toLowerCase())) {
      throw new VerdictDBValueException("Not supported scrambling method: " + methodName);
    }
    
    // overwrite options with custom options.
    Map<String, String> effectiveOptions = new HashMap<String, String>();
    for (Entry<String, String> o : options.entrySet()) {
      effectiveOptions.put(o.getKey(), o.getValue());
    }
    for (Entry<String, String> o : customOptions.entrySet()) {
      effectiveOptions.put(o.getKey(), o.getValue());
    }

    // determine scrambling method
    long blockSize = Double.valueOf(effectiveOptions.get("scrambleTableBlockSize")).longValue();
    ScramblingMethod scramblingMethod;
    if (methodName.equalsIgnoreCase("uniform")) {
      scramblingMethod = new UniformScramblingMethod(blockSize);
    } else if (methodName.equalsIgnoreCase("FastConverge") && primaryColumn == null) {
      scramblingMethod = new FastConvergeScramblingMethod(blockSize, scratchpadSchema.get());
    } else if (methodName.equalsIgnoreCase("FastConverge") && primaryColumn != null) {
      scramblingMethod = new FastConvergeScramblingMethod(blockSize, scratchpadSchema.get(), primaryColumn);
    } else {
      throw new VerdictDBValueException("Invalid scrambling method: " + methodName);
    }

    // perform scrambling
    ScramblingPlan plan = ScramblingPlan.create(newSchema, newTable, originalSchema, originalTable, scramblingMethod, effectiveOptions);
    ExecutablePlanRunner.runTillEnd(conn, plan);

    // compose scramble meta
    String blockColumn = effectiveOptions.get("blockColumnName");
    int blockCount = scramblingMethod.getBlockCount();
    String tierColumn = effectiveOptions.get("tierColumnName");
    int tierCount = scramblingMethod.getTierCount();
    
    Map<Integer, List<Double>> cumulativeDistribution = new HashMap<>();
    for (int i = 0; i < tierCount; i++) {
      List<Double> dist = scramblingMethod.getStoredCumulativeProbabilityDistributionForTier(i);
      cumulativeDistribution.put(i, dist);
    }
    
    ScrambleMeta meta = 
        new ScrambleMeta(
            newSchema, newTable, originalSchema, originalTable, 
            blockColumn, blockCount, tierColumn, tierCount, cumulativeDistribution);

    return meta;
  }

}