/*
 * Licensed to Metamarkets Group Inc. (Metamarkets) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Metamarkets licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.druid.query.aggregation.datasketches.tuple.doublearray;

import java.nio.ByteBuffer;

import com.google.common.primitives.Longs;
import com.metamx.common.IAE;
import com.yahoo.memory.Memory;
import com.yahoo.memory.MemoryRegion;
import com.yahoo.memory.NativeMemory;
import com.yahoo.sketches.tuple.ArrayOfDoublesSketch;

import io.druid.segment.data.ObjectStrategy;

/**
 * 
 * @author sunxin@rongcapital.cn
 *
 */
@SuppressWarnings("rawtypes")
public class SketchObjectStrategy implements ObjectStrategy
{

  private static final byte[] EMPTY_BYTES = new byte[]{};
  private static final ArrayOfDoublesSketch EMPTY_SKETCH = SketchOperations.EMPTY_SKETCH;

  @Override
  public int compare(Object s1, Object s2)
  {
    if (s1 instanceof ArrayOfDoublesSketch) {
      if (s2 instanceof ArrayOfDoublesSketch) {
        return SketchAggregatorFactory.COMPARATOR.compare((ArrayOfDoublesSketch) s1, (ArrayOfDoublesSketch) s2);
      } else {
        return -1;
      }
    }
    if (s1 instanceof Memory) {
      if (s2 instanceof Memory) {
        Memory s1Mem = (Memory) s1;
        Memory s2Mem = (Memory) s2;

        // We have two Ordered Compact sketches, so just compare their last entry if they have the size.
        // This is to produce a deterministic ordering, though it might not match the actual estimate
        // ordering, but that's ok because this comparator is only used by GenericIndexed
        int retVal = Longs.compare(s1Mem.getCapacity(), s2Mem.getCapacity());
        if (retVal == 0) {
          retVal = Longs.compare(s1Mem.getLong(s1Mem.getCapacity() - 8), s2Mem.getLong(s2Mem.getCapacity() - 8));
        }

        return retVal;
      } else {
        return 1;
      }
    }
    throw new IAE("Unknwon class[%s], toString[%s]", s1.getClass(), s1);

  }

  @Override
  public Class<? extends ArrayOfDoublesSketch> getClazz()
  {
    return ArrayOfDoublesSketch.class;
  }

  @Override
  public Object fromByteBuffer(ByteBuffer buffer, int numBytes)
  {
    if (numBytes == 0) {
      return EMPTY_SKETCH;
    }

    return new MemoryRegion(new NativeMemory(buffer), buffer.position(), numBytes);
  }

  @Override
  public byte[] toBytes(Object obj)
  {
    if (obj instanceof ArrayOfDoublesSketch) {
      return ((ArrayOfDoublesSketch) obj).toByteArray();
    } else if (obj instanceof Memory) {
      Memory mem = (Memory) obj;
      byte[] retVal = new byte[(int) mem.getCapacity()];
      mem.getByteArray(0, retVal, 0, (int) mem.getCapacity());
      return retVal;
    } else if (obj == null) {
      return EMPTY_BYTES;
    } else {
      throw new IAE("Unknown class[%s], toString[%s]", obj.getClass(), obj);
    }
  }
}
