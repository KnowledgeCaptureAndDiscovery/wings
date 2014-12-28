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

package edu.isi.wings.portal.classes.domains;

public class Permission {
  String userid;
  boolean canRead;
  boolean canWrite;
  boolean canExecute;
  
  public Permission(String userid, boolean canRead, boolean canWrite,
      boolean canExecute) {
    super();
    this.userid = userid;
    this.canRead = canRead;
    this.canWrite = canWrite;
    this.canExecute = canExecute;
  }

  public Permission(String userid) {
    super();
    this.userid = userid;
  }

  public String getUserid() {
    return userid;
  }

  public void setUserid(String userid) {
    this.userid = userid;
  }

  public boolean canRead() {
    return canRead;
  }

  public void setCanRead(boolean canRead) {
    this.canRead = canRead;
  }

  public boolean canWrite() {
    return canWrite;
  }

  public void setCanWrite(boolean canWrite) {
    this.canWrite = canWrite;
  }

  public boolean canExecute() {
    return canExecute;
  }

  public void setCanExecute(boolean canExecute) {
    this.canExecute = canExecute;
  }
}
