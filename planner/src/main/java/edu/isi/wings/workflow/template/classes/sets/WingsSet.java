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

package edu.isi.wings.workflow.template.classes.sets;

import java.util.ArrayList;

public class WingsSet extends ArrayList<WingsSet> {
	private static final long serialVersionUID = 1L;
	protected Object obj;

	public WingsSet() {
		super();
	}

	public WingsSet(WingsSet s) {
		this.add(s);
	}

	public WingsSet(Object obj) {
		this.obj = obj;
	}

	public WingsSet(Object[] objs) {
		for (Object t : objs) {
			this.add(new WingsSet(t));
		}
	}

	public int getMaxDimension() {
		if (!isSet())
			return 0;
		int dimension = 0;
		for (WingsSet i : this) {
			int idim = i.getMaxDimension() + 1;
			if (dimension < idim)
				dimension = idim;
		}
		return dimension;
	}

	public int getMinDimension() {
		if (!isSet())
			return 0;
		int dimension = 9999;
		for (WingsSet i : this) {
			int idim = i.getMinDimension() + 1;
			if (dimension > idim)
				dimension = idim;
		}
		return dimension;
	}

	public void increaseMinDimensionTo(int toDimension) {
		int increase = toDimension - getMinDimension();
		increaseDimensionBy(increase);
	}

	public void increaseMaxDimensionTo(int toDimension) {
		int increase = toDimension - getMaxDimension();
		increaseDimensionBy(increase);
	}

	public void increaseDimensionBy(int increase) {
		for (int i = 0; i < increase; i++) {
			WingsSet child = (WingsSet) this.clone();
			this.clear();
			this.add(child);
		}
	}

	public void shift() {
		if (!isSet())
			return;
		this.remove(0);
	}

	public int getSize() {
		return this.size();
	}

	public boolean isSet() {
		if (!isEmpty())
			return true;
		return false;
	}

	public Object getValue() {
		if (!isSet())
			return obj;
		return null;
	}

	/*
	 * Overrides
	 */
	public boolean add(WingsSet s) {
		if (!this.contains(s))
			return super.add(s);
		return false;
	}

	public int hashCode() {
		if (isSet())
			return super.hashCode();
		else if (obj != null)
			return obj.hashCode();
		return 0;
	}

	public boolean equals(Object s) {
		if (hashCode() == s.hashCode())
			return true;
		return false;
	}

	public String toString() {
		if (isSet())
			return super.toString();
		else
			return getValue().toString();
	}
}
