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

package edu.isi.wings.planner;

//import java.util.ArrayList;

import edu.isi.wings.workflow.template.classes.Port;
import edu.isi.wings.workflow.template.classes.Role;
import edu.isi.wings.workflow.template.classes.sets.Binding;
import edu.isi.wings.workflow.template.classes.sets.PortBinding;
import edu.isi.wings.workflow.template.classes.sets.PortBindingList;
import edu.isi.wings.workflow.template.classes.sets.PortSetRuleHandler;
import edu.isi.wings.workflow.template.classes.sets.SetExpression;
import edu.isi.wings.workflow.template.classes.sets.ValueBinding;
import edu.isi.wings.workflow.template.classes.sets.SetExpression.SetOperator;

public class Test {

	// TODO: Need a flatten function
	// - To flatten multi-dimensionality lists in case of a WTYPE Rule

	// TODO:
	// 1. Transfer this functionality to SWG and finish forward sweeping
	// 2. Implement the light forward, backward sweeps

	// 3. Start working on the GUI !!
	// - Important: Node Ports + PortSetExpressions

	// 4. Make parameter PortSetExpressions work too (new Rule for them ?)
	// - Change PC to return multiple parameter bindings

	public static void main(String[] args) {

		// int [] dimSizes = new int[] {2,3,3,4};
		//
		//
		// int dim = dimSizes.length;
		// int [] dimCounters = new int[dim]; // not more than 10 dimensions
		// dimCounters[0] = 1;
		// for(int k=1; k<dim; k++) {
		// int perms = 1;
		// for(int l=k-1; l>=0; l--) perms *= dimSizes[l];
		// dimCounters[k] = dimCounters[k-1] + perms;
		// }
		//
		// for(int x: dimCounters) {
		// System.out.println(x);
		// }
		//
		// Binding x = new ValueBinding("main");
		// ArrayList<Binding> vbs = new ArrayList<Binding>();
		// vbs.add(x);
		// int counter=0;
		// int num=0;
		// while(!vbs.isEmpty()) {
		// Binding vb = vbs.remove(0);
		// int vdim = 0;
		// for(vdim=0; vdim < dim; vdim++) {
		// if(counter < dimCounters[vdim])
		// break;
		// }
		//
		// if(vdim < dim) {
		// System.out.println("Count:"+counter+", Size:"+dimSizes[vdim]);
		// for(int i=0; i<dimSizes[vdim]; i++) {
		// num++;
		// Binding cvb = new ValueBinding(num);
		// vb.add(cvb);
		// vbs.add(cvb);
		// }
		// }
		// counter++;
		// }
		// System.out.println(x);

		String ns = "http://www.isi.edu#";

		// Node n = new Node(ns+"n1");
		// System.out.println(n.getID());

		// n.getInputMap(t.getInputMap(n), t.getOutputMap(n),
		// t.getConstraints(n));
		// Binding var = new Binding(new Binding(new
		// String[]{ns+"var1",ns+"var2"}));
		// var.add(new ValueBinding(new ValueBinding(new String[] { ns+"x",
		// ns+"y" })));
		// var.add(new Binding(ns+"tmp"));
		// var.add(new Binding(new String[] { ns+"a1", ns+"a4", ns+"a5" }));
		// System.out.println(var);
		// System.out.println(var.getMaxDimension() + "," +
		// var.getMinDimension());
		// System.out.println(var.getSize());
		//
		// var.increaseMinDimensionTo(3);
		// System.out.println(var);
		// System.out.println(var.getMaxDimension() + "," +
		// var.getMinDimension());
		// System.out.println(var.getSize());

		// Binding a = new ValueBinding(1);
		Binding a = new ValueBinding(new String[] { "a", "b", "c" });
		Binding b = new ValueBinding(new String[] { "d", "e" });
		Binding c = new ValueBinding(new String[] { "f", "g" });
		// Binding c = new ValueBinding(new Integer[]{ 3, 4 });
		// Binding d = new ValueBinding(new ValueBinding(new String[]{ "y", "z"
		// }));
		// Binding d = new ValueBinding(new ValueBinding(new ValueBinding(new
		// ValueBinding(new String[]{ "y", "z" }))));
		Binding d = new ValueBinding(new ValueBinding(new String[] { "y1", "z1" }));
		d.add(new ValueBinding(new String[] { "y2", "z2" }));
		// d.add(new ValueBinding("M"));

		Port p1 = new Port(ns + "p1");
		Port p2 = new Port(ns + "p2");
		Port p3 = new Port(ns + "p3");
		Port p4 = new Port(ns + "p4");

		p1.setRole(new Role(ns + "arole"));
		// p1.getRole().setDimensionality(1);
		p2.setRole(new Role(ns + "brole"));
		// p2.getRole().setDimensionality(1);
		p3.setRole(new Role(ns + "crole"));
		p4.setRole(new Role(ns + "drole"));

		// p4.getRole().setDimensionality(1);

		PortBinding portBindings = new PortBinding();
		portBindings.put(p1, a);
		portBindings.put(p2, b);
		portBindings.put(p3, c);
		portBindings.put(p4, d);

//		SetExpression s = new SetExpression(SetOperator.XPRODUCT, new Port[] { p1, p2 });
//		SetExpression s1 = new SetExpression(SetOperator.INCREASEDIM, new Port[] { p2 });
//		s.add(s1);
		SetExpression s = new SetExpression(SetOperator.XPRODUCT, new Port[] { p1, p2 });
//		SetExpression s2 = new SetExpression(SetOperator.NWISE, new Port[] { p3, p4 });
		s.add(new SetExpression(SetOperator.SHIFT, new Port[] { p3 }));
		s.add(new SetExpression(SetOperator.REDUCEDIM, new Port[] { p4 }));
		//s.add(s2);

		System.out.println("Expression: " + s);
		System.out.println("Original Bindings: " + portBindings);
		PortBindingList possibleBindings = new PortBindingList();
		possibleBindings = PortSetRuleHandler.handlePortSetRule(s, s, portBindings,
				possibleBindings);
		System.out.println("Final possible Bindings: \n" + possibleBindings);
		/*
		 * possibleBindings =
		 * PortSetRuleHandler.flattenPortBindingList(possibleBindings, 1);
		 * System.out.println("Flattened: \n"+possibleBindings);
		 */

		PortBinding pb = PortSetRuleHandler.deNormalizePortBindings(possibleBindings);
		System.out.println("Denormalized: \n" + pb);
	}
}
