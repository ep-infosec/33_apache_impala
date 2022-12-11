// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.impala.hive.executor;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF.DeferredJavaObject;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF.DeferredObject;
import org.apache.hadoop.hive.serde2.io.ByteWritable;
import org.apache.hadoop.hive.serde2.io.DateWritable;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.hive.serde2.io.ShortWritable;
import org.apache.hadoop.hive.serde2.io.TimestampWritable;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector.PrimitiveCategory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.WritableIntObjectInspector;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Simple Generic UDFs for testing.
 *
 * Udf that takes a variable number of arguments of the same type and applies
 * the "+" operator to them. The "+" is a concatenation for string and binary types.
 * For boolean types, it applies the OR operation. If only one argument is provided,
 * it returns that argument. If any argument is NULL, it returns NULL.
 *
 * For all argument types the return type is Writable class (e.g. IntWritable).
 * Generic UDfs can also return Java primitive classes (e.g. Integer). A separate
 * UDF class (TestGenericUdfWithJavaReturnTypes) is created with similar behavior
 * as this but different return types.
 *
 * This class is duplicated in fe and java/test-hive-udfs. We need this class in a
 * separate project so we can test loading UDF jars that are not already on the
 * classpath, and we can't delete the FE's class because UdfExecutorTest depends
 * on it.
 *
 */
public class TestGenericUdf extends GenericUDF {

  private List<PrimitiveCategory> inputTypes_;
  private PrimitiveObjectInspector retTypeOI_;
  private PrimitiveCategory argAndRetType_;

  private static final Set SUPPORTED_ARG_TYPES =
      new ImmutableSet.Builder<PrimitiveCategory>()
          .add(PrimitiveCategory.BOOLEAN)
          .add(PrimitiveCategory.BYTE)
          .add(PrimitiveCategory.SHORT)
          .add(PrimitiveCategory.INT)
          .add(PrimitiveCategory.LONG)
          .add(PrimitiveCategory.FLOAT)
          .add(PrimitiveCategory.DOUBLE)
          .add(PrimitiveCategory.STRING)
          .add(PrimitiveCategory.BINARY)
          .build();

  public TestGenericUdf() {
  }

  @Override
  public ObjectInspector initialize(ObjectInspector[] arguments)
      throws UDFArgumentException {

    if (arguments.length == 0) {
      throw new UDFArgumentException("No arguments provided.");
    }

    // Resetting here as initialize can be called more than once by Hive.
    inputTypes_  = new ArrayList<>();
    for (ObjectInspector oi : arguments) {
      if (!(oi instanceof PrimitiveObjectInspector)) {
        throw new UDFArgumentException("Found an input that is not a primitive.");
      }
      PrimitiveObjectInspector poi = (PrimitiveObjectInspector) oi;
      inputTypes_.add(poi.getPrimitiveCategory());
    }

    // return type is always same as first argument
    retTypeOI_ = getReturnObjectInspector((PrimitiveObjectInspector) arguments[0]);

    argAndRetType_ = retTypeOI_.getPrimitiveCategory();

    verifyArgs(argAndRetType_, inputTypes_);
    return retTypeOI_;
  }

  protected PrimitiveObjectInspector getReturnObjectInspector(
        PrimitiveObjectInspector oi) {
    // Simply returns the same object inspector. Subclasses can override this to return
    // different types of object inspectors.
    return oi;
  }

  @Override
  public Object evaluate(DeferredObject[] arguments)
      throws HiveException {
    if (arguments.length != inputTypes_.size()) {
      throw new HiveException("Number of arguments passed in did not match number of " +
          "arguments expected. Expected: "
              + inputTypes_.size() + " actual: " +  arguments.length);
    }
    switch (argAndRetType_) {
      case BOOLEAN:
        return evaluateBooleanWrapped(arguments);
      case BYTE:
        return evaluateByteWrapped(arguments);
      case SHORT:
        return evaluateShortWrapped(arguments);
      case INT:
        return evaluateIntWrapped(arguments);
      case LONG:
        return evaluateLongWrapped(arguments);
      case FLOAT:
        return evaluateFloatWrapped(arguments);
      case DOUBLE:
        return evaluateDoubleWrapped(arguments);
      case STRING:
        return evaluateStringWrapped(arguments);
      case BINARY:
        return evaluateBinaryWrapped(arguments);
      case DATE:
      case TIMESTAMP:
      default:
        throw new HiveException("Unsupported argument type " + argAndRetType_);
    }
  }

  @Override
  public String getDisplayString(String[] children) {
    return "TestGenericUdf";
  }

  private void verifyArgs(PrimitiveCategory argAndRetType,
      List<PrimitiveCategory> inputTypes) throws UDFArgumentException {

    if (!SUPPORTED_ARG_TYPES.contains(argAndRetType)) {
      throw new UDFArgumentException("Unsupported argument type " + argAndRetType_);
    }

    for (PrimitiveCategory inputType : inputTypes) {
      if (inputType != argAndRetType) {
        throw new UDFArgumentException("Invalid function for " +
            getSignatureString(argAndRetType, inputTypes));
      }
    }
  }

  protected Boolean evaluateBoolean(DeferredObject[] inputs) throws HiveException {
    boolean finalBoolean = false;
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof BooleanWritable)) {
        throw new HiveException("Expected BooleanWritable but got " + input.getClass());
      }
      boolean currentBool = ((BooleanWritable) input.get()).get();
      finalBoolean |= currentBool;
    }
    return finalBoolean;
  }

  protected Byte evaluateByte(DeferredObject[] inputs) throws HiveException {
    byte finalByte = 0;
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof ByteWritable)) {
        throw new HiveException("Expected ByteWritable but got " + input.getClass());
      }
      byte currentByte = ((ByteWritable) input.get()).get();
      finalByte += currentByte;
    }
    return finalByte;
  }

  protected Short evaluateShort(DeferredObject[] inputs) throws HiveException {
    short finalShort = 0;
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof ShortWritable)) {
        throw new HiveException("Expected ShortWritable but got " + input.getClass());
      }
      short currentShort = ((ShortWritable) input.get()).get();
      finalShort += currentShort;
    }
    return finalShort;
  }

  protected Integer evaluateInt(DeferredObject[] inputs) throws HiveException {
    int finalInt = 0;
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof IntWritable)) {
        throw new HiveException("Expected IntWritable but got " + input.getClass());
      }
      int currentInt = ((IntWritable) input.get()).get();
      finalInt += currentInt;
    }
    return finalInt;
  }

  protected Long evaluateLong(DeferredObject[] inputs) throws HiveException {
    long finalLong = 0;
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof LongWritable)) {
        throw new HiveException("Expected LongWritable but got " + input.getClass());
      }
      long currentLong = ((LongWritable) input.get()).get();
      finalLong += currentLong;
    }
    return finalLong;
  }

  protected Float evaluateFloat(DeferredObject[] inputs) throws HiveException {
    float finalFloat = 0.0F;
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof FloatWritable)) {
        throw new HiveException("Expected FloatWritable but got " + input.getClass());
      }
      float currentFloat = ((FloatWritable) input.get()).get();
      finalFloat += currentFloat;
    }
    return finalFloat;
  }

  protected Double evaluateDouble(DeferredObject[] inputs) throws HiveException {
    double finalDouble = 0.0;
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof DoubleWritable)) {
        throw new HiveException("Expected DoubleWritable but got " + input.getClass());
      }
      double currentDouble = ((DoubleWritable) input.get()).get();
      finalDouble  += currentDouble;
    }
    return finalDouble;
  }

  protected String evaluateString(DeferredObject[] inputs) throws HiveException {
    String finalString = "";
    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof Text)) {
        throw new HiveException("Expected Text but got " + input.get().getClass());
      }
      String currentString = ((Text) input.get()).toString();
      finalString += currentString;
    }
    return finalString;
  }

  protected byte[] evaluateBinary(DeferredObject[] inputs) throws HiveException {
    int resultLength = 0;

    for (DeferredObject input : inputs) {
      if (input == null) {
        return null;
      }
      if (!(input.get() instanceof BytesWritable)) {
        throw new HiveException(
            "Expected BytesWritable but got " + input.get().getClass());
      }
      byte[] currentArray = ((BytesWritable) input.get()).getBytes();
      resultLength += currentArray.length;
    }
    int pos = 0;
    byte[] result = new byte[resultLength];
    for (DeferredObject input : inputs) {
      byte[] currentArray = ((BytesWritable) input.get()).getBytes();
      System.arraycopy(
          currentArray, 0, result, pos, currentArray.length);
      pos += currentArray.length;
    }
    return result;
  }

  // The evaluate*Wrapped functions below get the result from evaluate*
  // and wrap in a Writable* class.

  protected Object evaluateBooleanWrapped(DeferredObject[] inputs)
      throws HiveException {
    BooleanWritable resultBool = new BooleanWritable();
    resultBool.set(evaluateBoolean(inputs));
    return resultBool;
  }

  protected Object evaluateByteWrapped(DeferredObject[] inputs)
      throws HiveException {
    ByteWritable resultByte = new ByteWritable();
    resultByte.set(evaluateByte(inputs));
    return resultByte;
  }

  protected Object evaluateShortWrapped(DeferredObject[] inputs)
     throws HiveException {
    ShortWritable resultShort = new ShortWritable();
    resultShort.set(evaluateShort(inputs));
    return resultShort;
  }

  protected Object evaluateIntWrapped(DeferredObject[] inputs)
      throws HiveException {
    IntWritable resultInt = new IntWritable();
    resultInt.set(evaluateInt(inputs));
    return resultInt;
  }

  protected Object evaluateLongWrapped(DeferredObject[] inputs)
      throws HiveException {
    LongWritable resultLong = new LongWritable();
    resultLong.set(evaluateLong(inputs));
    return resultLong;
  }

  protected Object evaluateFloatWrapped(DeferredObject[] inputs)
      throws HiveException {
    FloatWritable resultFloat = new FloatWritable();
    resultFloat.set(evaluateFloat(inputs));
    return resultFloat;
  }

  protected Object evaluateDoubleWrapped(DeferredObject[] inputs)
      throws HiveException {
    DoubleWritable resultDouble = new DoubleWritable();
    resultDouble.set(evaluateDouble(inputs));
    return resultDouble;
  }

  protected Object evaluateStringWrapped(DeferredObject[] inputs) throws HiveException {
    Text resultString = new Text();
    resultString.set(evaluateString(inputs));
    return resultString;
  }

  protected Object evaluateBinaryWrapped(DeferredObject[] inputs)
      throws HiveException {
    byte[] result = evaluateBinary(inputs);
    if (result == null) return null;
    BytesWritable resultBinary = new BytesWritable();
    resultBinary.set(result, 0, result.length);
    return resultBinary;
  }

  protected String getSignatureString(PrimitiveCategory argAndRetType_,
      List<PrimitiveCategory> inputTypes_) {
    return argAndRetType_ + "TestGenericUdf(" + Joiner.on(",").join(inputTypes_) + ")";
  }
}