/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
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

package org.voltdb.planner;

import java.util.List;

import org.voltdb.expressions.AbstractExpression;
import org.voltdb.expressions.TupleValueExpression;
import org.voltdb.plannodes.AbstractPlanNode;
import org.voltdb.plannodes.IndexScanPlanNode;
import org.voltdb.plannodes.OrderByPlanNode;
import org.voltdb.plannodes.ReceivePlanNode;
import org.voltdb.types.PlanNodeType;

public class TestPlansOrderBy extends PlannerTestCase {
    @Override
    protected void setUp() throws Exception {
        setupSchema(TestPlansGroupBy.class.getResource("testplans-orderby-ddl.sql"),
                    "testplansorderby", false);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private void validatePlan(String sql, boolean expectIndexScan,
            boolean expectSeqScan, boolean expectOrderBy)
    {
        validatePlan(sql, expectIndexScan, expectSeqScan, expectOrderBy, false, false);
    }

    private void validatePlan(String sql, boolean expectIndexScan,
            boolean expectSeqScan, boolean expectOrderBy, boolean expectHashAggregate,
            boolean expectedAggregate)
    {
        AbstractPlanNode pn = compile(sql);
        //* to debug */ System.out.println(pn.getChild(0).toJSONString());
        //* to debug */ System.out.println(pn.getChild(0).toExplainPlanString());
        assertEquals(expectIndexScan, pn.hasAnyNodeOfType(PlanNodeType.INDEXSCAN));
        assertEquals(expectSeqScan, pn.hasAnyNodeOfType(PlanNodeType.SEQSCAN));
        assertEquals(expectOrderBy, pn.hasAnyNodeOfType(PlanNodeType.ORDERBY));
        assertEquals(expectHashAggregate, pn.hasAnyNodeOfType(PlanNodeType.HASHAGGREGATE));
        assertEquals(expectedAggregate, pn.hasAnyNodeOfType(PlanNodeType.AGGREGATE));
    }

    /// Validate that a plan uses the full bag of tricks
    /// -- that it uses an index scan but no seq scan, order by, or hash aggregate.
    private void validateOptimalPlan(String sql)
    {
        validatePlan(sql, true, false, false);
    }

    /// Validate that a plan does not unintentionally use
    /// a completely inapplicable index scan for order.
    private void validateBruteForcePlan(String sql)
    {
        validatePlan(sql, false, true, true);
    }

    /// Validate that a plan does not unintentionally use
    /// a completely inapplicable index scan for order,
    /// when it still uses an index scan for non-determinism.
    /// This arguably should not happen for plans where the final
    /// sort already confers determinism (yet it currently does)?.
    private void validateIndexedBruteForcePlan(String sql)
    {
        validatePlan(sql, true, false, true);
    }

    public void testOrderByOneOfTwoIndexKeys()
    {
        validateOptimalPlan("SELECT * from T ORDER BY T_D0");
        validateOptimalPlan("SELECT * from T WHERE T_D0 = 1 ORDER BY T_D1");
    }

    public void testOrderByIndexedColumns() {
        validateOptimalPlan("SELECT * from T ORDER BY T_D0, T_D1");
        validateOptimalPlan("SELECT * from Tmanykeys ORDER BY T_D0, T_D1, T_D2");
    }

    public void testOrderByTwoDesc() {
        validateOptimalPlan("SELECT * from T ORDER BY T_D0 DESC, T_D1 DESC");
    }

    public void testOrderByTwoAscDesc() {
        validateBruteForcePlan("SELECT * from T ORDER BY T_D0, T_D1 DESC");
        validateBruteForcePlan("SELECT * from Tnokey ORDER BY T_D0, T_D1 DESC");
    }

    public void testOrderByTwoOfThreeIndexKeys()
    {
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D0 = ? ORDER BY T_D1, T_D2");
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D1 = ? ORDER BY T_D0, T_D2");
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D2 = ? ORDER BY T_D0, T_D1");
        validateOptimalPlan("SELECT * from Tmanykeys                ORDER BY T_D0, T_D1");
        validateIndexedBruteForcePlan("SELECT * FROM Tmanykeys WHERE T_D0 <= ? ORDER BY T_D1, T_D2");
        validateIndexedBruteForcePlan("SELECT * FROM Tmanykeys WHERE T_D0 <= ? ORDER BY T_D1 DESC, T_D2 DESC");
    }

    public void testOrderByOneOfThreeIndexKeys()
    {
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D0 = ? AND T_D1 = ? ORDER BY T_D2");
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D1 = ? AND T_D2 = ? ORDER BY T_D0");
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D2 = ? AND T_D0 = ? ORDER BY T_D1");
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D0 = ?              ORDER BY T_D1");
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D1 = ?              ORDER BY T_D0");
        validateOptimalPlan("SELECT * from Tmanykeys WHERE T_D2 = ?              ORDER BY T_D0");
        validateOptimalPlan("SELECT * from Tmanykeys                             ORDER BY T_D0");
        validateIndexedBruteForcePlan("SELECT * FROM Tmanykeys WHERE T_D0 = ? AND T_D1 < ? ORDER BY T_D2");
        validateIndexedBruteForcePlan("SELECT * FROM Tmanykeys WHERE T_D0 = ? AND T_D1 < ? ORDER BY T_D2 DESC");
        validateIndexedBruteForcePlan("SELECT * FROM Tmanykeys WHERE T_D0 = ? ORDER BY T_D2");
        validateIndexedBruteForcePlan("SELECT * FROM Tmanykeys WHERE T_D0 = ? ORDER BY T_D2 DESC");
    }

    public void testOrderByWrongPermutation()
    {
        // Order determinism, so do not make it worse by using index scan.
        validateBruteForcePlan("SELECT * from Tmanykeys ORDER BY T_D2, T_D1, T_D0");
        validateBruteForcePlan("SELECT * from Tmanykeys ORDER BY T_D2, T_D0, T_D1");
        validateBruteForcePlan("SELECT * from Tmanykeys ORDER BY T_D1, T_D0, T_D2");
        validateBruteForcePlan("SELECT * from Tmanykeys ORDER BY T_D1, T_D2, T_D0");
        validateBruteForcePlan("SELECT * from Tmanykeys ORDER BY T_D0, T_D2, T_D1");
        // This is order deterministic.  The primary key is T_D0, T_D1, T_D2. We
        // are ordering by two of these, and the third is constrained to be a constant.
        // So, there can only be one row with three different values for all three
        // columns, and it must be order deterministic.
        validateBruteForcePlan("SELECT * from Tmanykeys WHERE T_D1 = ? ORDER BY T_D2, T_D0");

        // Use index for filter.
        validateIndexedBruteForcePlan("SELECT * from Tmanykeys WHERE T_D0 = ? ORDER BY T_D2, T_D1");
    }

    public void testOrderByTooManyToIndex()
    {
        validateBruteForcePlan("SELECT * from T ORDER BY T_D0, T_D1, T_D2");
        validateBruteForcePlan("SELECT * from Tnokey ORDER BY T_D0, T_D1, T_D2");
    }

    public void testOrderByTooMany()
    {
        validateBruteForcePlan("SELECT * from Tnokey ORDER BY T_D0, T_D1, T_D2");
        validateBruteForcePlan("SELECT * from T ORDER BY T_D0, T_D1, T_D2");
    }

    public void testNoIndexToOrderBy() {
        validateIndexedBruteForcePlan("SELECT * FROM T ORDER BY T_D2");
        validateBruteForcePlan("SELECT * FROM Tnokey ORDER BY T_D2");
        validateIndexedBruteForcePlan("SELECT * FROM Tmanykeys ORDER BY T_D2");
    }

    public void testOrderByNLIJ()
    {
        validatePlan("SELECT Tnokey.T_D1, T.T_D0, T.T_D1 from Tnokey, T " +
                     "where Tnokey.T_D2 = 2 AND T.T_D0 = Tnokey.T_D0 " +
                     "ORDER BY T.T_D0, T.T_D1", true, true, true);
    }

    public void testTableAgg() {
        validatePlan("SELECT SUM(T_D0) from T", false, true, false, false, true);
        validatePlan("SELECT SUM(T_D0), COUNT(*), AVG(T_D1) from T", false, true, false, false, true);

        validatePlan("SELECT SUM(T_D0) from T ORDER BY T_D0, T_D1",
                false, true, false, false, true);
        validatePlan("SELECT SUM(T_D0), COUNT(*), AVG(T_D1) from T ORDER BY T_D0, T_D1",
                false, true, false, false, true);
    }

    public void testOrderByCountStar() {
        validatePlan("SELECT T_D0, COUNT(*) AS FOO FROM T GROUP BY T_D0 ORDER BY FOO", true, false, true, false, true);
        validatePlan("SELECT T_D0, COUNT(*) AS FOO FROM Tnokey GROUP BY T_D0 ORDER BY FOO", false, true, true, true, false);
    }

    public void testOrderByAggWithoutAlias() {
        validatePlan("SELECT T_D0, SUM(T_D1) FROM T GROUP BY T_D0 ORDER BY SUM(T_D1)",
                true, false, true, false, true);
        validatePlan("SELECT T_D0, COUNT(*) FROM T GROUP BY T_D0 ORDER BY COUNT(*)",
                true, false, true, false, true);
        validatePlan("SELECT T_D0, AVG(T_D1) FROM T GROUP BY T_D0 ORDER BY AVG(T_D1)",
                true, false, true, false, true);
        validatePlan("SELECT T_D0, COUNT(T_D1) FROM T GROUP BY T_D0 ORDER BY COUNT(T_D1)",
                true, false, true, false, true);
        validatePlan("SELECT T_D0, MIN(T_D1) FROM T GROUP BY T_D0 ORDER BY MIN(T_D1)",
                true, false, true, false, true);
        validatePlan("SELECT T_D0, MAX(T_D1) FROM T GROUP BY T_D0 ORDER BY MAX(T_D1)",
                true, false, true, false, true);

        // Complex aggregation
        validatePlan("SELECT T_D0, COUNT(*)+1 FROM T GROUP BY T_D0 ORDER BY COUNT(*)+1",
                true, false, true, false, true);
        validatePlan("SELECT T_D0, abs(MAX(T_D1)) FROM T GROUP BY T_D0 ORDER BY abs(MAX(T_D1))",
                true, false, true, false, true);
    }

    public void testEng450()
    {
        // This used to not compile. It does now. That's all we care about.
        compile("select T.T_D0, " +
                     "sum(T.T_D1) " +
                     "from T " +
                     "group by T.T_D0 " +
                     "order by T.T_D0;");
    }

    public void testOrderDescWithEquality() {
        validateOptimalPlan("SELECT * FROM T WHERE T_D0 = 2 ORDER BY T_D1");
        // See ENG-5084 to optimize this query to use inverse scan in future.
        validateIndexedBruteForcePlan("SELECT * FROM T WHERE T_D0 = 2 ORDER BY T_D1 DESC");
        //validateOptimalPlan("SELECT * FROM T WHERE T_D0 = 2 ORDER BY T_D1 DESC");
    }

    // Indexes on T (T_D0, T_D1), T2 (T_D0, T_D1), and Tmanykeys (T_D0, T_D1, T_D2)
    // no index on Tnokey and Tnokey2
    public void testENG4676() {
        // single column ORDER BY
        // ORDER BY on indexed key ascending, JOIN on indexed key from one table and
        // unindexed key from the other table -> no ORDER BY node
        validateOptimalPlan("SELECT * FROM T, Tmanykeys WHERE Tmanykeys.T_D0 = T.T_D2 " +
                "ORDER BY T.T_D0 LIMIT ?");
        // ORDER BY on indexed key descending, JOIN on indexed key from one table
        // and unindexed key from the other table -> no ORDER BY node
        validateOptimalPlan("SELECT * FROM T, Tmanykeys WHERE Tmanykeys.T_D0 = T.T_D2 " +
                "ORDER BY T.T_D0 DESC LIMIT ?");

        // multiple columns ORDER BY
        // ORDER BY on indexed key ascending, JOIN on indexed key from one table and
        // unindexed key from the other table -> no ORDER BY node
        validateOptimalPlan("SELECT * FROM T, Tmanykeys WHERE Tmanykeys.T_D0 = T.T_D2 " +
                "ORDER BY T.T_D0, T.T_D1 LIMIT ?");
        // ORDER BY on indexed key descending, JOIN on indexed key from one table and
        // unindexed key from the other table -> no ORDER BY node
        validateOptimalPlan("SELECT * FROM T, Tmanykeys WHERE Tmanykeys.T_D0 = T.T_D2 " +
                "ORDER BY T.T_D0 DESC, T.T_D1 DESC LIMIT ?");

        // filter on indexed column on one table, prefix join constraint,
        // ORDER BY looking for 1 recovered spoiler -> no ORDER BY node
        validateOptimalPlan("SELECT * FROM T, Tmanykeys WHERE Tmanykeys.T_D0 = T.T_D2 AND Tmanykeys.T_D0 = ?  " +
                "ORDER BY Tmanykeys.T_D1 LIMIT ?");
        // This query requires additional recognition of transitive equality to eliminate the ORDER BY.
        // See ENG-4728.
        //*See ENG-4728.*/ validateOptimalPlan("SELECT * FROM T, Tmanykeys WHERE Tmanykeys.T_D0 = T.T_D2 AND T.T_D2 = ?  " +
        //*See ENG-4728.*/        "ORDER BY Tmanykeys.T_D1 LIMIT ?");
        // ORDER BY is not recovered, but index is chosen for sorting purpose --> no ORDER BY node
        validateOptimalPlan("SELECT * FROM T, Tmanykeys WHERE Tmanykeys.T_D1 = T.T_D2 AND T.T_D0 = ?  " +
                "ORDER BY Tmanykeys.T_D0 LIMIT ?");

        // test NLJ --> need ORDER BY node
        validateBruteForcePlan("SELECT * FROM Tnokey, Tnokey2 WHERE Tnokey.T_D0 = Tnokey2.T_D0 " +
                "ORDER BY Tnokey.T_D1 LIMIT ?");

        // test nested NLIJ
        validateIndexedBruteForcePlan("SELECT * FROM T, T2, Tmanykeys " +
                "WHERE T.T_D0 = T2.T_D0 AND T2.T_D1 = Tmanykeys.T_D0 ORDER BY Tmanykeys.T_D1 LIMIT ?");
        validateIndexedBruteForcePlan("SELECT * FROM T, T2, Tmanykeys " +
                "WHERE T.T_D0 = T2.T_D0 AND T2.T_D1 = Tmanykeys.T_D0 ORDER BY T.T_D1 LIMIT ?");
        validateIndexedBruteForcePlan("SELECT * FROM T, T2, Tmanykeys " +
                "WHERE T.T_D0 = T2.T_D0 AND T2.T_D1 = Tmanykeys.T_D0 ORDER BY T2.T_D1 LIMIT ?");
        validateOptimalPlan("SELECT * FROM T, T2, Tmanykeys " +
                "WHERE T.T_D0 = T2.T_D0 AND T2.T_D1 = Tmanykeys.T_D0 ORDER BY T.T_D0, T.T_D1 LIMIT ?");
    }

    /**
     * Order by clause can only operate on the display columns list when having DISTINCT or GROUP BY clause.
     * However, it can operate on other columns or expressions on the table or joined table.
     */
    public void testOrderbyColumnsNotInDisplayList() {
        compile("select T.T_D0 from T order by T.T_D1;");
        compile("select T.T_D0 from T, Tmanykeys where Tmanykeys.T_D0 = T.T_D2 order by T.T_D1;");

        // DISTINCT
        failToCompile("select DISTINCT T.T_D0 from T order by T.T_D1;", "invalid ORDER BY expression");
        // GROUP BY
        failToCompile("select T.T_D0, count(*) from T group by T.T_D0 order by T.T_D1;", "invalid ORDER BY expression");

        // Very edge case:
        // Order by GROUP BY columns or expressions which are not in display list
        compile("select count(*) from T group by T.T_D0 order by T.T_D0;");
        compile("select count(*) from T group by T.T_D0 order by ABS(T.T_D0);");

        compile("select count(*) from T group by ABS(T.T_D0) order by ABS(T.T_D0);");
    }

    public void testOrderbyWithPartialIndex() {
        // Partial index T3_PARTIAL_IDX on T3 (T_D1) WHERE T_D2 > 3 is not picked up
        validatePlan("select T3.T_D0 from T3 order by T3.T_D1;", false, true, true);
        // Partial index T3_PARTIAL_IDX on T3 (T_D1) WHERE T_D2 > 3 is used now
        validatePlan("select T3.T_D0 from T3 where T_D2 > 3 order by T3.T_D1;", true, false, false);
        // Partial index T3_PARTIAL_IDX on T3 (T_D1) WHERE T_D2 > 3 is still used but now ORDER BY is required
        validatePlan("select T3.T_D0 from T3 where T_D2 > 3 order by T3.T_D2;", true, false, true);
    }

    public void testOrderByMP() {
        {
            // P_D1_IDX index provides the right order for the coordinator. Merge Receive
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, P_D1 + 1 from P order by P_D1");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateMergeReceive(pn, false, new int[] {0});
        }
        {
            // P_D1_IDX index sort direction is INVALID. Partition output is not ordered. ORDER BY
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, P_D1 + 1 from P where P_D1 > 3 order by P_D1, P_D2");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // P_D2 column is not covered by P_D1_IDX index. ORDER BY
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P where P_D1 > 1 order by P_D1, P_D2");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // P_D32_IDX index provides the right order for the coordinator. Merge Receive
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P order by P_D3 DESC, P_D2 DESC");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateMergeReceive(pn, false, new int[] {2, 1});
        }
        {
            // Column sort order mismatch
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P order by P_D3 ASC, P_D2 DESC");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // Column order mismatch
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P order by P_D2 ASC, P_D3 ASC");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // Sort order does not match
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P order by P_D3 DESC, P_D2");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // P_D1 column is not covered by the P_D32_IDX
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P where P_D3 > 1 order by P_D3 DESC, P_D1");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // P_D1_IDX index provides the right order for the coordinator. Merge Receive
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P order by P_D1");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateMergeReceive(pn, false, new int[] {0});
        }
        {
            // P_D1_IDX index provides the right order for the coordinator. Merge Receive with LIMIT
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P order by P_D1 limit 3");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateMergeReceive(pn, true, new int[] {0});
        }
        //  Partitions results are unordered. MergeSort is not supported
        {
            // Partitions results are unordered. Merge Receive and partitions ORDER BY
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, P_D2 from P order by P_D2");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // Partitions results are ordered by the pushed down ORDER BY.
            // Merge Receive with Order By and with LIMIT
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, P_D2  from P order by P_D2 limit 3");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateMergeReceive(pn, true, new int[] {1});
        }
        {
            // NLIJ with seq outer table scan.(T) ORDER BY
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P, T where P.P_D1 = T.T_D2 order by P.P_D1");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // NLIJ with index outer table scan (PK). ORDER BY column P.P_D1 is from a different table
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P, T where P.P_D1 = T.T_D2 and T.T_D0 = 2 order by P.P_D1");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // NLIJ with index outer table scan (PK). ORDER BY column T.T_D1 is not the first index column
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P, T where P.P_D1 = T.T_D2 and T.T_D0 = 2 order by T.T_D1");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // NLIJ with index outer table scan (PK). ORDER BY column T.T_D0 is the first the first index column
            // Merge RECEIVE
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1 from P, T where P.P_D1 = T.T_D2 and T.T_D0 = 2 order by T.T_D0 limit 3");
            assertEquals(2, frags.size());
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateMergeReceive(pn, true, new int[] {0});
        }
    }

    public void testOrderByMPAggregate() {
        {
            // Select from the aggregate view
            List<AbstractPlanNode> frags =  compileToFragments("Select * from V_P1 ORDER BY V_G1, V_G2");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateAggregatedMergeReceive(pn, true, false, false, false);
        }
        {
            // Two aggregates at the coordinator. ORDER BY
            List<AbstractPlanNode> frags =  compileToFragments("Select V_G1, sum(V_CNT), max(v_sum_age) from V_P1_ABS GROUP BY V_G1 ORDER BY V_G1");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // Merge Receive with Serial aggregation
            //            select indexed_non_partition_key, max(col)
            //            from partitioned
            //            group by indexed_non_partition_key
            //            order by indexed_non_partition_key;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, max(P_D2) from P where P_D1 > 0 group by P_D1 order by P_D1");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateAggregatedMergeReceive(pn, true, false, false, false);
            // Make sure partition index scan is a range scan
            pn = frags.get(1).getChild(0);
            IndexScanPlanNode ipn = (IndexScanPlanNode)pn;
            assertTrue(ipn.getPredicate() == null);;
        }
        {
            // Merge Receive with Partial aggregation
            //            select indexed_non_partition_key, col, max(col)
            //            from partitioned
            //            group by indexed_non_partition_key, col
            //            order by indexed_non_partition_key;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, P_D2, max(P_D2) from P group by P_D1, P_D2 order by P_D1");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateAggregatedMergeReceive(pn, false, true, false, false);
        }
        {
            // Coordinator HASH aggregate not convertible to Serial or Partial aggregation
            //          select indexed_non_partition_key, max(col)
            //          from partitioned
            //          group by indexed_non_partition_key
            //          order by aggregate;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, max(P_D2) from P group by P_D1 order by max(P_D2)");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // Merge Receive with Serial aggregation, projection, and limit
            // Not supported yets
            //            select indexed_non_partition_key, sum(col), sum(col)+1
            //            from partitioned
            //            group by indexed_non_partition_key
            //            order by indexed_non_partition_key
            //            limit 2;
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, sum(P_D1), sum(P_D1)+1 from P group by P_D1 order by P_D1 limit 2");
            AbstractPlanNode pn = frags.get(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // Serial aggregation instead of ORDERING
            //          select max(indexed_partition_key)
            //          from partitioned
            //          order by max(indexed_partition_key);"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select max(P_D1) from P order by max(P_D1)");
            AbstractPlanNode pn = frags.get(0).getChild(0);
            assertEquals(PlanNodeType.AGGREGATE, pn.getPlanNodeType());
        }
        {
            // No aggregation at coordinator
            //          select indexed_partition_key, max(col)
            //          from partitioned
            //          group by indexed_partition_key
            //          order by indexed_partition_key;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select max(P_D2), P_D0 from P group by P_D0  order by P_D0");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateAggregatedMergeReceive(pn, false, false, false, false);
        }
        {
            // Partition results are not sorted
            //          select non_indexed_partition, max(col)
            //          from partitioned
            //          group by non_indexed_partition
            //          order by non_indexed_partition;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P1_D0, max(P1_D2) from P1 group by P1_D0 order by P1_D0");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // No aggregation at coordinator
            //          select indexed_non_partition_key, max(col)
            //          from partitioned
            //          group by indexed_non_partition_key, indexed_partition_key
            //          order by indexed_partition_key;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select max(P_D2), P_D1 from P group by P_D1, P_D0 order by P_D0");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateAggregatedMergeReceive(pn, false, false, false, false);
        }
        {
            // The Index Scan (P_D32_IDX) does not cover P_D1 column from the ORDER BY
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D1, P_D3, max (P_D0) from p where P_D3 = 0 group by P_D1, P_D3 order by P_D3, P_D1;");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // ORDER BY column P_D1 is not covered by the P_D32_IDX index
            //            select indexed_non_partition_key1, non_indexed_non_partition_col2, max(col)
            //            from partitioned
            //            group by indexed_non_partition_key1, non_indexed_non_partition_col2
            //            order by indexed_non_partition_key1, non_indexed_non_partition_col2;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D3, P_D1, max (P_D0) from p where P_D3 > 0 group by P_D3, P_D1 order by P_D3, P_D1");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            assertEquals(PlanNodeType.ORDERBY, pn.getPlanNodeType());
        }
        {
            // Merge Receive with Serial aggregation
            //            select indexed_non_partition_key1, indexed_non_partition_key2, max(col)
            //            from partitioned
            //            group by indexed_non_partition_key1, indexed_non_partition_key2
            //            order by indexed_non_partition_key1, indexed_non_partition_key2;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D3, P_D2, max (P_D0) from p where P_D3 > 0 group by P_D3, P_D2 order by P_D3, P_D2");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateAggregatedMergeReceive(pn, true, false, false, false);
        }
        {
            // Merge Receive without aggregation at coordinator
            //            select indexed_non_partition_key1, indexed_non_partition_key2, col, max(col)
            //            from partitioned
            //            group by indexed_non_partition_key1, indexed_non_partition_key2, col
            //            order by indexed_non_partition_key1, indexed_non_partition_key2;"
            List<AbstractPlanNode> frags =  compileToFragments(
                    "select P_D3, P_D2, P_D0, max (P_D0) from p where P_D3 > 0 group by P_D3, P_D2, P_D0 order by P_D3, P_D2");
            AbstractPlanNode pn = frags.get(0).getChild(0).getChild(0);
            validateAggregatedMergeReceive(pn, false, false, false, false);
        }
    }

    private void validateMergeReceive(AbstractPlanNode pn, boolean hasLimit, int[] sortColumnIdx) {
        assertEquals(PlanNodeType.RECEIVE, pn.getPlanNodeType());
        ReceivePlanNode rpn = (ReceivePlanNode) pn;
        assertEquals(true, rpn.isMergeReceive());
        assertNotNull(rpn.getInlinePlanNode(PlanNodeType.ORDERBY));
        assertEquals(hasLimit, rpn.getInlinePlanNode(PlanNodeType.LIMIT) != null);
        OrderByPlanNode opn = (OrderByPlanNode) rpn.getInlinePlanNode(PlanNodeType.ORDERBY);
        List<AbstractExpression> ses = opn.getSortExpressions();
        assertEquals(sortColumnIdx.length, ses.size());
        int idx = 0;
        for (AbstractExpression se : ses) {
            assertEquals(sortColumnIdx[idx++], ((TupleValueExpression) se).getColumnIndex());
        }
    }

    private void validateAggregatedMergeReceive(AbstractPlanNode pn,
            boolean hasSerialAggr, boolean hasPartialAggr, boolean hasProj, boolean hasLimit) {
        assertEquals(PlanNodeType.RECEIVE, pn.getPlanNodeType());
        ReceivePlanNode rpn = (ReceivePlanNode) pn;
        assertEquals(true, rpn.isMergeReceive());
        assertNotNull(rpn.getInlinePlanNode(PlanNodeType.ORDERBY));
        assertEquals(hasSerialAggr, rpn.getInlinePlanNode(PlanNodeType.AGGREGATE) != null);
        assertEquals(hasPartialAggr, rpn.getInlinePlanNode(PlanNodeType.PARTIALAGGREGATE) != null);
        assertEquals(hasProj, rpn.getInlinePlanNode(PlanNodeType.PROJECTION) != null);
        if (hasSerialAggr || hasPartialAggr) {
            AbstractPlanNode aggrNode = (hasSerialAggr) ?
                    rpn.getInlinePlanNode(PlanNodeType.AGGREGATE) :
                        rpn.getInlinePlanNode(PlanNodeType.PARTIALAGGREGATE);
            assertEquals(hasLimit, aggrNode.getInlinePlanNode(PlanNodeType.LIMIT) != null);
        } else {
            assertEquals(hasLimit, rpn.getInlinePlanNode(PlanNodeType.LIMIT) != null);
        }
    }

}
