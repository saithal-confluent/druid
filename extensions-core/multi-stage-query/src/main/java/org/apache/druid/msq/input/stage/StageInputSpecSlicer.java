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

package org.apache.druid.msq.input.stage;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.druid.java.util.common.ISE;
import org.apache.druid.msq.exec.OutputChannelMode;
import org.apache.druid.msq.input.InputSlice;
import org.apache.druid.msq.input.InputSpec;
import org.apache.druid.msq.input.InputSpecSlicer;

import java.util.ArrayList;
import java.util.List;

/**
 * Slices {@link StageInputSpec} into {@link StageInputSlice}.
 */
public class StageInputSpecSlicer implements InputSpecSlicer
{
  // Stage number -> partitions for that stage
  private final Int2ObjectMap<ReadablePartitions> stagePartitionsMap;

  // Stage number -> output mode for that stage
  private final Int2ObjectMap<OutputChannelMode> stageOutputChannelModeMap;

  public StageInputSpecSlicer(
      final Int2ObjectMap<ReadablePartitions> stagePartitionsMap,
      final Int2ObjectMap<OutputChannelMode> stageOutputChannelModeMap
  )
  {
    this.stagePartitionsMap = stagePartitionsMap;
    this.stageOutputChannelModeMap = stageOutputChannelModeMap;
  }

  @Override
  public boolean canSliceDynamic(InputSpec inputSpec)
  {
    return false;
  }

  @Override
  public List<InputSlice> sliceStatic(InputSpec inputSpec, int maxNumSlices)
  {
    final StageInputSpec stageInputSpec = (StageInputSpec) inputSpec;

    final ReadablePartitions stagePartitions = stagePartitionsMap.get(stageInputSpec.getStageNumber());
    final OutputChannelMode outputChannelMode = stageOutputChannelModeMap.get(stageInputSpec.getStageNumber());

    if (stagePartitions == null) {
      throw new ISE("Stage[%d] output partitions not available", stageInputSpec.getStageNumber());
    }

    if (outputChannelMode == null) {
      throw new ISE("Stage[%d] output mode not available", stageInputSpec.getStageNumber());
    }

    // Decide how many workers to use, and assign inputs.
    final List<ReadablePartitions> workerPartitions = stagePartitions.split(maxNumSlices);
    final List<InputSlice> retVal = new ArrayList<>();

    for (final ReadablePartitions partitions : workerPartitions) {
      retVal.add(
          new StageInputSlice(
              stageInputSpec.getStageNumber(),
              partitions,
              outputChannelMode
          )
      );
    }

    return retVal;
  }

  @Override
  public List<InputSlice> sliceDynamic(
      InputSpec inputSpec,
      int maxNumSlices,
      int maxFilesPerSlice,
      long maxBytesPerSlice
  )
  {
    throw new UnsupportedOperationException("Cannot sliceDynamic.");
  }
}
