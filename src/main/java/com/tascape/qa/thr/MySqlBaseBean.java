/*
 * Copyright 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tascape.qa.thr;

import com.tascape.qa.th.db.SuiteProperty;
import com.tascape.qa.th.db.SuiteResult;
import com.tascape.qa.th.db.TestCase;
import com.tascape.qa.th.db.TestResult;
import com.tascape.qa.th.db.TestResultMetric;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@Named
@ApplicationScoped
public class MySqlBaseBean implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(MySqlBaseBean.class);

    @Resource(name = "jdbc/thr")
    private DataSource ds;

    public List<Map<String, Object>> getLatestSuitesResult() throws NamingException, SQLException {
        String sql = "SELECT * FROM (SELECT * FROM "
            + SuiteResult.TABLE_NAME + " WHERE NOT INVISIBLE_ENTRY ORDER BY " + SuiteResult.START_TIME + " DESC) AS T"
            + " GROUP BY " + SuiteResult.SUITE_NAME
            + " ORDER BY " + SuiteResult.SUITE_NAME + ";";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            return this.dumpResultSetToList(rs);
        }
    }

    public List<Map<String, Object>> getSuitesResult(long startTime, long stopTime, int numberOfEntries,
        String suiteName, String jobName, boolean invisibleIncluded)
        throws NamingException, SQLException {
        String sql = "SELECT * FROM " + SuiteResult.TABLE_NAME
            + " WHERE " + SuiteResult.START_TIME + " > ?"
            + " AND " + SuiteResult.STOP_TIME + " < ?";
        if (notEmpty(suiteName)) {
            sql += " AND " + SuiteResult.SUITE_NAME + " = ?";
        } else if (notEmpty(jobName)) {
            sql += " AND " + SuiteResult.JOB_NAME + " = ?";
        }
        if (!invisibleIncluded) {
            sql += " AND NOT " + SuiteResult.INVISIBLE_ENTRY;
        }
        sql += " ORDER BY " + SuiteResult.START_TIME + " DESC;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, startTime);
            stmt.setLong(2, stopTime);
            if (notEmpty(suiteName)) {
                stmt.setString(3, suiteName);
            } else if (notEmpty(jobName)) {
                stmt.setString(3, jobName);
            }
            LOG.trace("{}", stmt);
            stmt.setMaxRows(numberOfEntries);
            ResultSet rs = stmt.executeQuery();
            return this.dumpResultSetToList(rs);
        }
    }

    public Map<String, Object> getSuiteResult(String srid) throws NamingException, SQLException {
        String sql = "SELECT * FROM " + SuiteResult.TABLE_NAME
            + " WHERE " + SuiteResult.SUITE_RESULT_ID + " = ?;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, srid);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            List<Map<String, Object>> data = this.dumpResultSetToList(rs);
            if (data.isEmpty()) {
                throw new SQLException("No data for suite result id " + srid);
            }
            return data.get(0);
        }
    }

    public List<Map<String, Object>> getTestsResult(String srid) throws NamingException, SQLException {
        String sql = "SELECT * FROM " + TestResult.TABLE_NAME + " TR "
            + "INNER JOIN " + TestCase.TABLE_NAME + " TC "
            + "ON TR.TEST_CASE_ID = TC.TEST_CASE_ID "
            + "WHERE " + TestResult.SUITE_RESULT + " = ? "
            + "ORDER BY " + TestResult.START_TIME + " DESC;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, srid);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            return this.dumpResultSetToList(rs);
        }
    }

    public List<Map<String, Object>> getTestsResult(List<String> srids) throws NamingException, SQLException {
        String sql = "SELECT * FROM " + TestResult.TABLE_NAME + " TR "
            + "INNER JOIN " + TestCase.TABLE_NAME + " TC "
            + "ON TR.TEST_CASE_ID = TC.TEST_CASE_ID "
            + "WHERE " + TestResult.SUITE_RESULT + " IN (" + StringUtils.join(srids, ",") + ") "
            + "ORDER BY " + TestCase.SUITE_CLASS + "," + TestCase.TEST_CLASS + ","
            + TestCase.TEST_METHOD + "," + TestCase.TEST_DATA_INFO + " DESC;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            return this.dumpResultSetToList(rs);
        }
    }

    public void setSuiteResultInvisible(String srid, boolean invisible) throws NamingException, SQLException {
        String sql = "UPDATE " + SuiteResult.TABLE_NAME
            + " SET " + SuiteResult.INVISIBLE_ENTRY + " = ?"
            + " WHERE " + SuiteResult.SUITE_RESULT_ID + " = ?;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);
            stmt.setBoolean(1, invisible);
            stmt.setString(2, srid);
            LOG.trace("{}", stmt);
            stmt.executeUpdate();
        }
    }

    public List<Map<String, Object>> getSuiteResultDetailHistory(long startTime, long stopTime, int numberOfEntries,
        String suiteName, boolean invisibleIncluded) throws NamingException, SQLException {
        String sr = "SELECT " + SuiteResult.SUITE_RESULT_ID + " FROM " + SuiteResult.TABLE_NAME
            + " WHERE " + SuiteResult.START_TIME + " > ?"
            + " AND " + SuiteResult.STOP_TIME + " < ?"
            + " AND " + SuiteResult.SUITE_NAME + " = ?";
        if (!invisibleIncluded) {
            sr += " AND NOT " + SuiteResult.INVISIBLE_ENTRY;
        }
        sr += " ORDER BY " + SuiteResult.START_TIME + " DESC;";

        String tr = "SELECT * FROM " + TestResult.TABLE_NAME
            + " WHERE " + TestResult.EXECUTION_RESULT
            + " IN (" + sr + ")"
            + " ORDER BY " + SuiteResult.START_TIME + " DESC;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(tr);
            stmt.setLong(1, startTime);
            stmt.setLong(2, stopTime);
            if (suiteName != null && !suiteName.isEmpty()) {
                stmt.setString(3, suiteName);
            }
            LOG.trace("{}", stmt);
            stmt.setMaxRows(numberOfEntries);
            ResultSet rs = stmt.executeQuery();
            return this.dumpResultSetToList(rs);
        }
    }

    public List<Map<String, Object>> dumpResultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rsml = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();

        while (rs.next()) {
            Map<String, Object> d = new LinkedHashMap<>();
            for (int col = 1; col <= rsmd.getColumnCount(); col++) {
                d.put(rsmd.getColumnLabel(col), rs.getObject(col));
            }
            rsml.add(d);
        }
        LOG.trace("{} rows loaded", rsml.size());
        return rsml;
    }

    public void importJson(JSONObject json) throws NamingException, SQLException {
        JSONObject sr = json.getJSONObject("suite_result");
        String srid = sr.getString(SuiteResult.SUITE_RESULT_ID);
        LOG.debug("srid {}", srid);

        try (Connection conn = this.getConnection()) {
            String sql = "SELECT * FROM " + SuiteResult.TABLE_NAME + " WHERE " + SuiteResult.SUITE_RESULT_ID + " = ?;";
            PreparedStatement stmt = conn.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, srid);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            if (rs.first()) {
                LOG.debug("already imported {}", srid);
                return;
            }
            rs.moveToInsertRow();
            for (int col = 1; col <= rsmd.getColumnCount(); col++) {
                String cn = rsmd.getColumnLabel(col);
                rs.updateObject(cn, sr.get(cn));
            }
            rs.insertRow();
            rs.last();
            rs.updateRow();
            LOG.debug("sr imported");
        }

        try (Connection conn = this.getConnection()) {
            String sql = "SELECT * FROM " + SuiteProperty.TABLE_NAME
                + " WHERE " + SuiteProperty.SUITE_RESULT_ID + " = ?;";
            PreparedStatement stmt = conn.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, srid);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();

            JSONArray sps = sr.getJSONArray("suite_properties");
            int len = sps.length();
            for (int i = 0; i < len; i++) {
                rs.moveToInsertRow();
                JSONObject tr = sps.getJSONObject(i);
                for (int col = 1; col <= rsmd.getColumnCount(); col++) {
                    String cn = rsmd.getColumnLabel(col);
                    if (SuiteProperty.SUITE_PROPERTY_ID.equals(cn)) {
                        continue;
                    }
                    rs.updateObject(cn, tr.get(cn));
                }
                rs.insertRow();
                rs.last();
                rs.updateRow();
            }
            LOG.debug("sps imported");
        }

        JSONArray trs = sr.getJSONArray("test_results");
        int len = trs.length();

        try (Connection conn = this.getConnection()) {
            String sql = String.format("SELECT * FROM %s WHERE %s=? AND %s=? AND %s=? AND %s=? AND %s=?;",
                TestCase.TABLE_NAME,
                TestCase.SUITE_CLASS,
                TestCase.TEST_CLASS,
                TestCase.TEST_METHOD,
                TestCase.TEST_DATA_INFO,
                TestCase.TEST_DATA
            );
            PreparedStatement stmt
                = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setMaxRows(1);
            for (int i = 0; i < len; i++) {
                JSONObject tr = trs.getJSONObject(i);
                stmt.setString(1, tr.getString(TestCase.SUITE_CLASS));
                stmt.setString(2, tr.getString(TestCase.TEST_CLASS));
                stmt.setString(3, tr.getString(TestCase.TEST_METHOD));
                stmt.setString(4, tr.getString(TestCase.TEST_DATA_INFO));
                stmt.setString(5, tr.getString(TestCase.TEST_DATA));
                ResultSet rs = stmt.executeQuery();
                if (!rs.first()) {
                    rs.moveToInsertRow();
                    rs.updateString(TestCase.SUITE_CLASS, tr.getString(TestCase.SUITE_CLASS));
                    rs.updateString(TestCase.TEST_CLASS, tr.getString(TestCase.TEST_CLASS));
                    rs.updateString(TestCase.TEST_METHOD, tr.getString(TestCase.TEST_METHOD));
                    rs.updateString(TestCase.TEST_DATA_INFO, tr.getString(TestCase.TEST_DATA_INFO));
                    rs.updateString(TestCase.TEST_DATA, tr.getString(TestCase.TEST_DATA));
                    rs.insertRow();
                    rs.last();
                    rs.updateRow();
                    rs = stmt.executeQuery();
                    rs.first();
                }
                tr.put(TestCase.TEST_CASE_ID, rs.getLong(TestCase.TEST_CASE_ID));
            }
            LOG.debug("tcid updated");
        }

        try (Connection conn = this.getConnection()) {
            String sql = "SELECT * FROM " + TestResult.TABLE_NAME + " WHERE " + TestResult.SUITE_RESULT + " = ?;";
            PreparedStatement stmt = conn.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setString(1, srid);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 0; i < len; i++) {
                rs.moveToInsertRow();
                JSONObject tr = trs.getJSONObject(i);
                for (int col = 1; col <= rsmd.getColumnCount(); col++) {
                    String cn = rsmd.getColumnLabel(col);
                    rs.updateObject(cn, tr.get(cn));
                }
                rs.insertRow();
                rs.last();
                rs.updateRow();
            }
            LOG.debug("trs imported");
        }

        try (Connection conn = this.getConnection()) {
            String sql = "SELECT * FROM " + TestResultMetric.TABLE_NAME + ";";
            PreparedStatement stmt = conn.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setMaxRows(1);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 0; i < len; i++) {
                JSONArray jarr = trs.getJSONObject(i).optJSONArray("test_result_metrics");
                if (jarr == null) {
                    continue;
                }
                int l = jarr.length();
                for (int j = 0; j < l; j++) {
                    JSONObject trm = jarr.getJSONObject(j);
                    rs.moveToInsertRow();
                    for (int col = 1; col <= rsmd.getColumnCount(); col++) {
                        String cn = rsmd.getColumnLabel(col);
                        if (cn.equals(TestResultMetric.TEST_RESULT_METRIC_ID)) {
                            continue;
                        }
                        rs.updateObject(cn, trm.get(cn));
                    }
                    rs.insertRow();
                    rs.last();
                    rs.updateRow();
                }
            }
            LOG.debug("trms imported");
        }
    }

    public Date convertToDate(long time) {
        return new Date(time);
    }

    public static long getMillis(String time) {
        if (time == null || time.trim().isEmpty()) {
            return System.currentTimeMillis();
        } else {
            LocalDateTime ldt = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LOG.trace("ldt {}", ldt);
            ZoneId zone = ZoneId.of("America/Los_Angeles");
            ldt.atZone(zone);
            LOG.trace("ldt {}", ldt);
            return ldt.toInstant(ZoneOffset.ofHours(-8)).toEpochMilli();
        }
    }

    public static boolean notEmpty(String string) {
        if (string == null) {
            return false;
        }
        if (string.trim().isEmpty()) {
            return false;
        }
        return true;
    }

    List<Map<String, Object>> getSuiteProperties(String srid) throws NamingException, SQLException {
        String sql = "SELECT * FROM " + SuiteProperty.TABLE_NAME
            + " WHERE " + SuiteProperty.SUITE_RESULT_ID + " = ?"
            + " ORDER BY " + SuiteProperty.PROPERTY_NAME + ";";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, srid);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            return this.dumpResultSetToList(rs);
        }
    }

    private Connection getConnection() throws NamingException, SQLException {
        Connection conn = this.ds.getConnection();
        if (conn == null) {
            throw new SQLException("Can't get database connection");
        }
        return conn;
    }
}
