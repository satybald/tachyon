/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.master.lineage.recompute;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import tachyon.Constants;
import tachyon.exception.FileDoesNotExistException;
import tachyon.heartbeat.HeartbeatExecutor;
import tachyon.master.file.FileSystemMaster;
import tachyon.master.lineage.meta.Lineage;

/**
 * A periodical executor that detects lost files and launches recompute jobs.
 */
public final class RecomputeExecutor implements HeartbeatExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_TYPE);

  private static final int DEFAULT_RECOMPUTE_LAUNCHER_POOL_SIZE = 10;
  private final RecomputePlanner mPlanner;
  private final FileSystemMaster mFileSystemMaster;
  /** The thread pool to launch recompute jobs */
  private final ExecutorService mFixedExecutionService =
      Executors.newFixedThreadPool(DEFAULT_RECOMPUTE_LAUNCHER_POOL_SIZE);

  /**
   * @param planner recompute planner
   */
  public RecomputeExecutor(RecomputePlanner planner, FileSystemMaster fileSystemMaster) {
    mPlanner = Preconditions.checkNotNull(planner);
    mFileSystemMaster = Preconditions.checkNotNull(fileSystemMaster);
  }

  @Override
  public void heartbeat() {
    RecomputePlan plan = mPlanner.plan();
    if (plan != null && !plan.isEmpty()) {
      mFixedExecutionService.submit(new RecomputeLauncher(plan));
    }
  }

  /**
   * Thread to launch the recompute jobs in a given plan.
   */
  final class RecomputeLauncher implements Runnable {
    private RecomputePlan mPlan;

    RecomputeLauncher(RecomputePlan plan) {
      mPlan = Preconditions.checkNotNull(plan);
    }

    @Override
    public void run() {
      for (Lineage lineage : mPlan.getLineageToRecompute()) {
        // empty all the lost files
        for (Long fileId : lineage.getLostFiles()) {
          try {
            mFileSystemMaster.resetFile(fileId);
          } catch (FileDoesNotExistException e) {
            LOG.error("the lost file {} is invalid", fileId, e);
          }
        }

        boolean success = lineage.getJob().run();
        if (!success) {
          LOG.error("Failed to recompute job {}", lineage.getJob());
        }
      }
    }
  }
}
