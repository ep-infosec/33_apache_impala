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

package org.apache.impala;

import java.text.ParseException;
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.Text;

/**
 * Helper for a regression test for IMPALA-8016: this
 * uses another class (from the same jar) at evaluate() time, needing
 * the class loader.
 */
public class ImportsNearbyClassesUdf extends UDF {
  public ImportsNearbyClassesUdf() {
    // Ensure that new classes can be loaded in constructor.
    UtilForUdfConstructor.getHello();
  }

  public Text evaluate(Text para) throws ParseException {
    return new Text(UtilForUdf.getHello());
  }
}
