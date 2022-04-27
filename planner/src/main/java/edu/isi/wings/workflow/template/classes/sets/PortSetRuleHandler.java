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

import edu.isi.wings.catalog.data.classes.metrics.Metrics;
import edu.isi.wings.common.SerializableObjectCloner;
import edu.isi.wings.workflow.template.api.Template;
import edu.isi.wings.workflow.template.classes.Link;
import edu.isi.wings.workflow.template.classes.Node;
import edu.isi.wings.workflow.template.classes.Port;
import edu.isi.wings.workflow.template.classes.sets.SetExpression.SetOperator;

public class PortSetRuleHandler {
	/*
	 * Algorithm to convert Variable argument mappings to component mappings
	 * normalize: M => {m}
	 */
	public static PortBindingList normalizePortBindings(Node n, Template t) {
		PortBindingList possibleBindings = new PortBindingList();
		PortSetCreationRule prule = n.getPortSetRule();

		PortBinding portBindings = new PortBinding();
		ArrayList<Port> paramPorts = new ArrayList<Port>();
		Link[] ilinks = t.getInputLinks(n);
		SetExpression default_expr = new SetExpression(SetOperator.XPRODUCT);
		for (Link l : ilinks) {
			if (l.getDestinationPort() != null && l.getVariable() != null) {
				if (l.getVariable().isParameterVariable())
					paramPorts.add(l.getDestinationPort());
				portBindings.put(l.getDestinationPort(),
						(Binding) SerializableObjectCloner.clone(l.getVariable().getBinding()));
				default_expr.add(new SetExpression(SetOperator.XPRODUCT, l.getDestinationPort()));
			}
		}

		// FIXME: Currently doing a cross product for all parameters by default
		// !
		SetExpression expr = prule.getSetExpression(); //crossProductPorts(prule.getSetExpression(), paramPorts);

		// System.out.println("Expression: "+prule.getSetExpression());
		// System.out.println("Original Bindings:\n"+portBindings);
		possibleBindings = handlePortSetRule(expr, default_expr, portBindings, possibleBindings);
		// System.out.println("Final Bindings:\n"+possibleBindings);
		return possibleBindings;
	}

	@SuppressWarnings("unused")
  private static SetExpression crossProductPorts(SetExpression expr, ArrayList<Port> ports) {
		if (ports.size() == 0)
			return expr;

		SetExpression filtered_expr = removePortsFromRule(expr, ports);
		SetExpression nexpr = new SetExpression(SetOperator.XPRODUCT);
		for (Port port : ports) {
			nexpr.add(new SetExpression(SetOperator.XPRODUCT, port));
		}
		nexpr.add(filtered_expr);
		return nexpr;
	}

	private static SetExpression removePortsFromRule(SetExpression expr, ArrayList<Port> badports) {
		SetExpression nexpr = expr;
		if (expr.isSet()) {
			nexpr = new SetExpression(expr.getOperator());
			for (SetExpression cexpr : expr) {
				SetExpression ncexpr = removePortsFromRule(cexpr, badports);
				if (ncexpr != null)
					nexpr.add(ncexpr);
			}
		} else {
			Port p = expr.getPort();
			if (badports.contains(p))
				nexpr = null;
		}
		return nexpr;
	}

	/*
	 * Algorithm in an incremental algorithm that starts with one binding (with
	 * possible higher dimensionalities) and keeps finding new possibilities
	 * until the dimensionality requirements of the ports are satisfied
	 */
	public static PortBindingList handlePortSetRule(SetExpression expr, SetExpression default_expr,
			PortBinding initPortBinding, PortBindingList finalPortBindings) {
		PortBindingList tmpBindings = getPossiblePortBindings(expr,
				modPortBindings(expr, initPortBinding, new ArrayList<SetOperator>()), 
				finalPortBindings);
		int numext = 1;
		while (numext > 0) {
			numext = 0;
			PortBindingList tmpBindings1 = new PortBindingList();
			int len = tmpBindings.size();

			// System.out.println(tmpBindings);
			for (int i = 0; i < len; i++) {
				PortBindingList b = tmpBindings.get(i);
				if (!b.isList()) {
					boolean f = false;
					for (Port pp : b.getPortBinding().keySet()) {
						Binding bb = b.getPortBinding().get(pp);
						// System.out.println(bb+":"+bb.getMaxDimension());
						if (pp.getRole().getDimensionality() < bb.getMaxDimension()) {
							f = true;
						}
					}
					if (f) {
						numext++;
						// FIXME: Replaced expr with default_expr (X Product) on
						// second iteration ! (Testing phase)
						PortBindingList tmp = handlePortSetRule(expr, default_expr,
								b.getPortBinding(), new PortBindingList());
						tmpBindings1.add(tmp);
					} else {
						tmpBindings1.add(b);
					}
				}
			}
			if (numext > 0)
				tmpBindings = tmpBindings1;
		}
		return tmpBindings;
	}

	/*
	 * Increase or decrease the dimensionality of a port binding. (or shift collection) 
	 * NOTE: 
	 *  - These should only be used on a single port ? 
	 */
	private static PortBinding modPortBindings(SetExpression expr, PortBinding portBindings, ArrayList<SetOperator> operators) {
		// System.out.println("-"+expr);
		// System.out.println("--"+portBindings);
		if (expr.isSet()) {		  
			// System.out.println("--- is Set");
			for (SetExpression cexpr : expr) {
        SetOperator op = cexpr.getOperator();
        ArrayList<SetOperator> newops = new ArrayList<SetOperator>(operators);
        newops.add(op);
        portBindings = modPortBindings(cexpr, portBindings, newops);
        
			  if (op == SetOperator.INCREASEDIM ||
			      op == SetOperator.REDUCEDIM || 
			      op == SetOperator.SHIFT ) {
			    cexpr.setOperator(null);
			  }
			}
		}
		else {
		  for(SetOperator op : operators) {
		    if (op == SetOperator.INCREASEDIM) {
		      portBindings.get(expr.getPort()).increaseDimensionBy(1);
		    }
		    else if (op == SetOperator.REDUCEDIM) {
          portBindings.get(expr.getPort()).reduceDimensionBy(1);
        }
		    else if (op == SetOperator.SHIFT) {
          portBindings.get(expr.getPort()).shift();
        }
		  }
		}
		return portBindings;
	}

	private static PortBindingList getPossiblePortBindings(SetExpression expr,
			PortBinding portBindings, PortBindingList finalPortBindings) {
		if (expr.isSet()) {
			for (SetExpression cexpr : expr) {
				PortBindingList cbindings = getPossiblePortBindings(cexpr, portBindings,
						new PortBindingList());
				if (finalPortBindings.isEmpty()) {
					finalPortBindings.addAll(cbindings);
					continue;
				}
				if (expr.getOperator() == SetOperator.NWISE) {
					int num = finalPortBindings.size();
					int num2 = cbindings.size();
					// Trim current bindings if we have more than possible in an
					// NWISE operation
					if (num > num2)
						for (int i = num - 1; i >= num2; i--)
							finalPortBindings.remove(i);
					for (int i = 0; i < finalPortBindings.size(); i++)
						finalPortBindings.get(i).getPortBinding()
								.putAll(cbindings.get(i).getPortBinding());
				} else if (expr.getOperator() == SetOperator.XPRODUCT) {
					// TODO: Add support for conditional XProducts
					int i = 0;
					int num = finalPortBindings.size();
					int index = 0;
					for (PortBindingList cbinding : cbindings) {
						if (i > 0) {
							for (int j = 0; j < num; j++) {
								finalPortBindings.add(new PortBindingList(new PortBinding(
										finalPortBindings.get(j).getPortBinding())));
							}
							index += num;
						}
						i++;
						for (int j = index; j < finalPortBindings.size(); j++) {
							finalPortBindings.get(j).getPortBinding()
									.putAll(cbinding.getPortBinding());
						}
					}
				}
			}
		} else {
			Port p = expr.getPort();
			Binding b = portBindings.get(p);
			if (b == null)
				return finalPortBindings;

			b.increaseMinDimensionTo(p.getRole().getDimensionality() + 1);

			for (WingsSet s : b) {
				Binding cb = (Binding) s;
				PortBinding newBinding = new PortBinding();
				// System.out.println(p+"="+cb);
				newBinding.put(p, cb);
				finalPortBindings.add(new PortBindingList(newBinding));
			}
		}
		// System.out.println(finalPortBindings);
		return finalPortBindings;
	}

	private static int getListDimension(PortBindingList l) {
		if (!l.isList())
			return 0;
		int dimension = 0;
		for (PortBindingList i : l) {
			int idim = getListDimension(i) + 1;
			if (dimension < idim)
				dimension = idim;
		}
		return dimension;
	}

	public static PortBindingList flattenPortBindingList(PortBindingList l, int level) {
		PortBindingList r = new PortBindingList();
		ArrayList<PortBindingList> q = new ArrayList<PortBindingList>();
		q.add(l);
		while (!q.isEmpty()) {
			PortBindingList ql = q.remove(0);
			if (getListDimension(ql) == level)
				r.add(ql);
			else
				q.addAll(ql);
		}
		return r;
	}

	/*
	 * Algorithm to create output maps for variables (denormalization: {m} => M)
	 */
	public static PortBinding deNormalizePortBindings(PortBindingList pblist) {
		if (pblist.isList()) {
			PortBinding pb = new PortBinding();
			for (PortBindingList pbl : pblist) {
				PortBinding cpb = deNormalizePortBindings(pbl);
				for (Port p : cpb.keySet()) {
					Binding cb = cpb.get(p);
					Binding b = pb.get(p);
					if (b == null) {
						b = new Binding();
						b.setMetrics(new Metrics(cb.getMetrics()));
					} else {
						// ****************** TODO: Remove Non-Matching metrics
						// from the new Binding **********************
					}
					/*
					 * if (cb.isSet() && cb.size() == 1) { b.addAll(cb); } else
					 * { b.add(cb); }
					 */
					// Temporarily replaced the above block with the one line
					// below (Testing phase)
					b.add(cb);
					pb.put(p, b);
				}
			}
			/*
			 * for (Port p : pb.keySet()) { Binding b = pb.get(p); if (b.isSet()
			 * && b.size() == 1) { for (WingsSet s : b) pb.put(p, (Binding) s);
			 * } }
			 */
			return pb;
		} else {
			return pblist.getPortBinding();
		}
	}

}
