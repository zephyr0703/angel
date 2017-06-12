/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tencent.angel.ml.matrix.psf;

import com.tencent.angel.exception.InvalidParameterException;
import com.tencent.angel.ml.math.vector.DenseDoubleVector;
import com.tencent.angel.ml.matrix.psf.aggr.*;
import com.tencent.angel.ml.matrix.psf.aggr.base.ScalarAggrResult;
import com.tencent.angel.ml.matrix.psf.get.base.GetFunc;
import com.tencent.angel.ml.matrix.psf.get.single.GetRowResult;
import com.tencent.angel.ml.matrix.psf.updater.Fill;
import com.tencent.angel.ml.matrix.psf.updater.RandomNormal;
import com.tencent.angel.ml.matrix.psf.updater.RandomUniform;
import com.tencent.angel.psagent.matrix.MatrixClient;
import com.tencent.angel.psagent.matrix.MatrixClientFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class AggrFuncTest extends SharedAngelTest {
  private static MatrixClient w2Client = null;
  private static double[] localArray0 = null;
  private static double[] localArray1 = null;
  private static double delta = 1e-6;
  private static int dim = -1;

  @BeforeClass
  public static void setup() throws Exception {
    SharedAngelTest.setup();
    w2Client = MatrixClientFactory.get("w2", 0);
    // row 0 is a random uniform
    w2Client.update(new RandomUniform(w2Client.getMatrixId(), 0, 0.0, 1.0)).get();
    // row 1 is a random normal
    w2Client.update(new RandomNormal(w2Client.getMatrixId(), 1, 0.0, 1.0)).get();
    // row 2 is filled with 1.0
    w2Client.update(new Fill(w2Client.getMatrixId(), 2, 1.0)).get();

    localArray0 = pull(w2Client, 0);
    localArray1 = pull(w2Client, 1);
    dim  = localArray1.length;
  }

  @Test
  public void testAmax() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Amax(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double max = Double.MIN_VALUE;
    for (double x : localArray1) {
      if (max < Math.abs(x)) max = Math.abs(x);
    }
    Assert.assertEquals(result, max, delta);
  }

  @Test
  public void testAmin() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Amin(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double min = Double.MAX_VALUE;
    for (double x : localArray1) {
      if (min > Math.abs(x)) min = Math.abs(x);
    }
    Assert.assertEquals(result, min, delta);
  }

  @Test
  public void testAsum() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Asum(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double sum = 0.0;
    for (double x : localArray1) {
      sum += Math.abs(x);
    }
    Assert.assertEquals(result, sum, delta);
  }

  @Test
  public void testDot() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Dot(w2Client.getMatrixId(), 0, 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double dot = 0.0;
    for (int i = 0; i < dim; i++) {
      dot += localArray0[i] * localArray1[i];
    }
    Assert.assertEquals(result, dot, delta);
  }

  @Test
  public void testMax() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Max(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double max = Double.MIN_VALUE;
    for (double x : localArray1) {
      if (max < x) max = x;
    }
    Assert.assertEquals(result, max, delta);
  }

  @Test
  public void testMin() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Min(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double min = Double.MAX_VALUE;
    for (double x : localArray1) {
      if (min > x) min = x;
    }
    Assert.assertEquals(result, min, delta);
  }

  @Test
  public void testNnz() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Nnz(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    int count = 0;
    for (double x : localArray1) {
      if (Math.abs(x - 0.0) > delta) count++;
    }
    Assert.assertEquals((int)result, count);
  }

  @Test
  public void testNrm2() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Nrm2(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double nrm2 = 0;
    for (double x : localArray1) {
      nrm2 += x * x;
    }
    nrm2 = Math.sqrt(nrm2);
    Assert.assertEquals(result, nrm2, delta);
  }

  @Test
  public void testPull() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Pull(w2Client.getMatrixId(), 1);
    double[] result = ((DenseDoubleVector)(((GetRowResult) w2Client.get(func)).getRow())).getValues();

    for (int i = 0; i < dim; i ++ ) {
      Assert.assertEquals(result[i], localArray1[i], delta);
    }
  }

  @Test
  public void testSum() throws InvalidParameterException, InterruptedException, ExecutionException {
    GetFunc func = new Sum(w2Client.getMatrixId(), 1);
    double result = ((ScalarAggrResult) w2Client.get(func)).getResult();

    double sum = 0.0;
    for (double x : localArray1) {
      sum += x;
    }
    Assert.assertEquals(result, sum, delta);
  }

  private static void printMatrix(MatrixClient client, int rowId) {
    double[] arr = pull(client, rowId);
    System.out.println(Arrays.toString(arr));
  }

  private static double[] pull(MatrixClient client, int rowId) {
    GetRowResult rowResult = (GetRowResult) client.get(new Pull(client.getMatrixId(), rowId));
    return ((DenseDoubleVector)rowResult.getRow()).getValues();
  }
}