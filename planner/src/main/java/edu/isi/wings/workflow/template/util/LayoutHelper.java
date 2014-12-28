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

package edu.isi.wings.workflow.template.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LayoutHelper {
	static public double[] parseCommentString(String s) {
		Pattern pat = Pattern.compile("x=([\\d\\.]+),y=([\\d\\.]+)");
		Matcher m = pat.matcher(s);
		if (m.find()) {
			double[] pos = new double[2];
			try {
				pos[0] = Double.parseDouble(m.group(1));
				pos[1] = Double.parseDouble(m.group(2));
				return pos;
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	static public String createCommentString(double x, double y) {
		return "x=" + x + ",y=" + y;
	}
}
