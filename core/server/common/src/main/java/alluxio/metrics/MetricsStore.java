/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.metrics;

import alluxio.collections.IndexDefinition;
import alluxio.collections.IndexedSet;

import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A store of metrics collecting metrics from workers and clients.
 */
@ThreadSafe
public class MetricsStore {
  private static final IndexDefinition<Metric> FULL_NAME_INDEX = new IndexDefinition<Metric>(true) {
    @Override
    public Object getFieldValue(Metric o) {
      return o.getFullMetricName();
    }
  };

  private static final IndexDefinition<Metric> NAME_INDEX = new IndexDefinition<Metric>(false) {
    @Override
    public Object getFieldValue(Metric o) {
      return o.getName();
    }
  };

  private static final IndexDefinition<Metric> HOSTNAME_INDEX = new IndexDefinition<Metric>(false) {
    @Override
    public Object getFieldValue(Metric o) {
      return o.getHostname();
    }
  };

  private final IndexedSet<Metric> mWorkerMetrics =
      new IndexedSet<>(FULL_NAME_INDEX, NAME_INDEX, HOSTNAME_INDEX);

  /**
   * Gets all the metrics by instance type. The supported instance types are worker and client.
   *
   * @param instanceType the instance type
   * @return the metrics stored in {@link IndexedSet};
   */
  private IndexedSet<Metric> getMetricsByInstanceType(String instanceType) {
    if (instanceType.equals(MetricsSystem.WORKER_INSTANCE)) {
      return mWorkerMetrics;
    } else {
      throw new IllegalArgumentException("Unsupported instance type " + instanceType);
    }
  }

  /**
   * Put the metrics from an instance with a hostname. If all the old metrics associated with this
   * instance will be removed and then replaced by the latest.
   *
   * @param instance the instance type
   * @param hostname the hostname of the instance
   * @param metrics the new worker metrics
   */
  public synchronized void putWorkerMetrics(String instance, String hostname,
      List<Metric> metrics) {
    IndexedSet<Metric> set = getMetricsByInstanceType(instance);
    set.removeByField(HOSTNAME_INDEX, hostname);
    for (Metric metric : metrics) {
      set.add(metric);
    }
  }

  /**
   * Gets all the metrics by instance type and the metric name. The supported instance types are
   * worker and client.
   *
   * @param instanceType the instance type
   * @param name the metric name
   * @return the set of matched metrics
   */
  public synchronized Set<Metric> getMetricsByInstanceTypeAndName(String instanceType,
      String name) {
    return getMetricsByInstanceType(instanceType).getByField(NAME_INDEX, name);
  }

  /**
   * Clears all the metrics.
   */
  public synchronized void clear() {
    mWorkerMetrics.clear();
  }
}
