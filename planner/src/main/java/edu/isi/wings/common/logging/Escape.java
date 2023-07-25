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

/*
 * This file or a portion of this file is licensed under the terms of
 * the Globus Toolkit Public License, found in file GTPL, or at
 * http://www.globus.org/toolkit/download/license.html. This notice must
 * appear in redistributions of this file, with or without modification.
 *
 * Redistributions of this Software, with or without modification, must
 * reproduce the GTPL in: (1) the Software, or (2) the Documentation or
 * some other similar material which is provided with the Software (if
 * any).
 *
 * Copyright 1999-2004 University of Chicago and The University of
 * Southern California. All rights reserved.
 */

package edu.isi.wings.common.logging;

/**
 * This class tries to define an interface to deal with quoting, escaping, and
 * the way back. The quoting algorithm is safe to only itself. Thus,
 *
 * <pre>
 * unescape( escape( s ) ) === s
 * </pre>
 *
 * holds true, but
 *
 * <pre>
 * escape( unescape( s ) ) =?= s
 * </pre>
 *
 * does not necessarily hold.
 *
 * @author Gaurang Mehta
 * @author Karan Vahi
 * @author Jens-S. Vöckler
 * @version $Revision: 1.1 $
 */
public class Escape {

  /**
   * Defines the character used to escape characters.
   */
  private char m_escape;

  /**
   * Defines the set of characters that require escaping.
   */
  private String m_escapable;

  /**
   * Defines the default quoting and escaping rules, escaping the apostrophe,
   * double quote and backslash. The escape character is the backslash.
   *
   */
  public Escape() {
    m_escapable = "\"'\\";
    m_escape = '\\';
  }

  /**
   * Constructs arbitrary escaping rules.
   *
   * @param escapable
   *            is the set of characters that require escaping
   * @param escape
   *            is the escape character itself.
   */
  public Escape(String escapable, char escape) {
    m_escape = escape;
    m_escapable = escapable;

    // ensure that the escape character is part of the escapable char set
    if (escapable.indexOf(escape) == -1) m_escapable += m_escape;
  }

  /**
   * Transforms a given string by escaping all characters inside the quotable
   * characters set with the escape character. The escape character itself is
   * also escaped.
   *
   * @param s
   *            is the string to escape.
   * @return the quoted string
   * @see #unescape(String )
   */
  public String escape(String s) {
    // sanity check
    if (s == null) return null;

    StringBuffer result = new StringBuffer(s.length());
    for (int i = 0; i < s.length(); ++i) {
      char ch = s.charAt(i);
      if (m_escapable.indexOf(ch) != -1) result.append(m_escape);
      result.append(ch);
    }
    return result.toString();
  }

  /**
   * Transforms a given string by unescaping all characters that are prefixed
   * with the escape character.
   *
   * @param s
   *            is the string to remove escapes from.
   * @return the quoted string
   * @see #unescape(String )
   */
  public String unescape(String s) {
    // sanity check
    if (s == null) return null;

    StringBuffer result = new StringBuffer(s.length());
    int state = 0;
    for (int i = 0; i < s.length(); ++i) {
      char ch = s.charAt(i);
      if (state == 0) {
        // default state
        if (ch == m_escape) state = 1; else result.append(ch);
      } else {
        // "found escape" state
        if (m_escapable.indexOf(ch) == -1) result.append(m_escape);
        result.append(ch);
        state = 0;
      }
    }

    return result.toString();
  }
}
