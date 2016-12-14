/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * This file contains original code and/or modifications of original code.
 * Any modifications made by VoltDB Inc. are licensed under the following
 * terms and conditions:
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *//* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.sqlparser.assertions.symtab;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.Fail;
import org.voltdb.sqlparser.semantics.symtab.Column;

/**
 * {@link Column} specific assertions - Generated by CustomAssertionGenerator.
 */
public class ColumnAssert extends AbstractAssert<ColumnAssert, Column> {

    /**
     * Creates a new </code>{@link ColumnAssert}</code> to make assertions on actual Column.
     * @param actual the Column we want to make assertions on.
     */
    public ColumnAssert(Column actual) {
        super(actual, ColumnAssert.class);
    }

    /**
     * An entry point for ColumnAssert to follow AssertJ standard <code>assertThat()</code> statements.<br>
     * With a static import, one's can write directly : <code>assertThat(myColumn)</code> and get specific assertion with code completion.
     * @param actual the Column we want to make assertions on.
     * @return a new </code>{@link ColumnAssert}</code>
     */
    public static ColumnAssert assertThat(Column actual) {
        return new ColumnAssert(actual);
    }
    /**
     * Verifies that the actual Column's type is equal to the given one.
     * @param type the given type to compare the actual Column's type to.
     * @return this assertion object.
     * @throws AssertionError - if the actual Column's type is not equal to the given one.
     */
    public static Condition<Column> withColumnTypeNamed(final String aTypeName) {
        return new Condition<Column>() {
            @Override
            public boolean matches(Column aValue) {
                String actualTypeName = aValue.getType().getName();
                if (!actualTypeName.equalsIgnoreCase(aTypeName)) {
                    String message = String.format(
                            "\nExpected type name to be:\n  <%s>\n but was:\n  <%s>",
                            aTypeName, actualTypeName);
                    Fail.fail(message);
                    return false;
                }
                return true;
            }
        };
    }
}
