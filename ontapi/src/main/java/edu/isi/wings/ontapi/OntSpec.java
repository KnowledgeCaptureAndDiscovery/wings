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

package edu.isi.wings.ontapi;

public final class OntSpec {
  private String id;

  private OntSpec(String anID) {
    this.id = anID;
  }

  public String toString() {
    return this.id;
  }

  public static final OntSpec PLAIN = new OntSpec("No Inference Reasoner");

  public static final OntSpec MICRO = new OntSpec("Micro Rules Reasoner");

  public static final OntSpec MINI = new OntSpec("Mini Rules Reasoner");

  public static final OntSpec DL = new OntSpec("DL Reasoner");

  public static final OntSpec FULL = new OntSpec("Full OWL Reasoner");

  public static final OntSpec TRANS = new OntSpec("Transitive Reasoner");

  public static final OntSpec RDFS = new OntSpec("RDFS Reasoner");

  public static final OntSpec PELLET = new OntSpec("Pellet Reasoner");
}
