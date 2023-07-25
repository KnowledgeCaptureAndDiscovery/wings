/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007, Your Corporation. All Rights Reserved.
////////////////////////////////////////////////////////////////////////////////

package edu.isi.wings.common.logging;

import java.util.HashMap;
import java.util.Set;

public class LoggerHelper {

  /**
   * returns a System agnostic string for the properties file
   *
   * @param propertiesFileName
   *            the properties file name
   * @return a String
   */
  public static String getPathToProperties(String propertiesFileName) {
    String baseDirectory = System.getProperty("user.dir");
    String fileSeparator = System.getProperty("file.separator");
    return baseDirectory + fileSeparator + propertiesFileName;
  }

  /**
   * returns a string representation from an argument map argName => Object
   *
   * @param queryIndicator
   *            a string representing the query
   * @param argumentMap
   *            argument map argName => Object @return a String
   * @return a string
   */
  public static String getArgumentString(
    String queryIndicator,
    HashMap<String, Object> argumentMap
  ) {
    StringBuilder result = new StringBuilder();
    result.append(queryIndicator);
    result.append(" called with:");
    // result.append(newlineCharacter);
    int size = argumentMap.size();
    int counter = 0;
    Set<String> keys = argumentMap.keySet();
    for (String key : keys) {
      result.append(key);
      result.append(" = ");
      result.append(argumentMap.get(key));
      if (++counter != size) {
        // result.append(newlineCharacter);
        result.append(", ");
      }
    }
    return result.toString();
  }

  /**
   * returns a String representation of the return value
   *
   * @param queryIndicator
   *            a string representing the query
   * @param returnValue
   *            an object @return a String
   * @return a string
   */
  public static String getReturnString(
    String queryIndicator,
    Object returnValue
  ) {
    StringBuilder result = new StringBuilder();
    result.append(queryIndicator);
    result.append(" returned:");
    result.append(returnValue.toString());
    return result.toString();
  }
}
