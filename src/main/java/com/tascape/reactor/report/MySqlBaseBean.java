/*
 * Copyright 2015 - 2016 Nebula Bay.
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
package com.tascape.reactor.report;

import com.tascape.reactor.db.SuiteProperty;
import com.tascape.reactor.db.SuiteResult;
import com.tascape.reactor.db.TaskCase;
import com.tascape.reactor.db.CaseResult;
import com.tascape.reactor.db.CaseResultMetric;
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
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final long serialVersionUID = 1L;

    private static final Map<String, List<Map<String, Object>>> LOADED_LACASE_SUITES_RESULT = new ConcurrentHashMap<>();

    private static final Map<String, List<Map<String, Object>>> LOADED_LACASE_JOBS_RESULT = new ConcurrentHashMap<>();

    @Resource(name = "jdbc/thr")
    private DataSource ds;

    Set<String> loadProjects() throws SQLException, NamingException {
        Set<String> projects = new HashSet<>();
        String sql = "SELECT DISTINCT " + SuiteResult.PROJECT_NAME + " FROM " + SuiteResult.TABLE_NAME
            + " WHERE (NOT " + SuiteResult.INVISIBLE_ENTRY + ")"
            + " ORDER BY " + SuiteResult.PROJECT_NAME + ";";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                if (name != null) {
                    projects.add(name);
                    while (true) {
                        int i = name.lastIndexOf("-");
                        if (i != -1) {
                            name = name.substring(0, i);
                            projects.add(name);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        projects.add("");
        return projects;
    }

    public List<Map<String, Object>> getLatestSuitesResult() throws NamingException, SQLException {
        return this.getLatestSuitesResult(System.currentTimeMillis(), "");
    }

    public List<Map<String, Object>> getLatestSuitesResult(String project) throws NamingException, SQLException {
        return this.getLatestSuitesResult(System.currentTimeMillis(), project);
    }

    List<Map<String, Object>> getLatestSuitesResult(long date, String project) throws NamingException, SQLException {
        List<Map<String, Object>> list = LOADED_LACASE_SUITES_RESULT.get(date + project);
        if (list != null) {
            LOG.debug("retrieved from cache {}", date);
            return list;
        }
        String sql = new StringBuilder("SELECT * FROM (SELECT * FROM ").append(SuiteResult.TABLE_NAME)
            .append(" WHERE (NOT INVISIBLE_ENTRY) AND (")
            .append(SuiteResult.START_TIME + " < ").append(date)
            .append(") AND (" + SuiteResult.START_TIME + " > ").append(date - 604800000) // a week
            .append(StringUtils.isBlank(project) ? ")" : ") AND (" + SuiteResult.PROJECT_NAME + " LIKE '" + project
                + "%')")
            .append(" ORDER BY " + SuiteResult.START_TIME + " DESC) AS T")
            .append(" GROUP BY " + SuiteResult.SUITE_NAME)
            .append(" ORDER BY " + SuiteResult.SUITE_NAME + ";").toString();
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            list = this.dumpResultSetToList(rs);
            if (date < System.currentTimeMillis()) {
                LOG.debug("cache history data");
//                LOADED_LACASE_SUITES_RESULT.put(date + project, list);
            }
            return list;
        }
    }

    public List<Map<String, Object>> getLatestJobsResult() throws NamingException, SQLException {
        return this.getLatestJobsResult(System.currentTimeMillis(), "");
    }

    public List<Map<String, Object>> getLatestJobsResult(String project) throws NamingException, SQLException {
        return this.getLatestJobsResult(System.currentTimeMillis(), project);
    }

    public List<Map<String, Object>> getLatestJobsResult(long date, String project) throws NamingException, SQLException {
        List<Map<String, Object>> list = LOADED_LACASE_JOBS_RESULT.get(date + project);
        if (list != null) {
            LOG.debug("retrieved from cache {}", date);
            return list;
        }
        String sql = new StringBuilder("SELECT * FROM (SELECT * FROM ").append(SuiteResult.TABLE_NAME)
            .append(" WHERE (NOT INVISIBLE_ENTRY) AND (")
            .append(SuiteResult.START_TIME + " < ").append(date)
            .append(") AND (" + SuiteResult.START_TIME + " > ").append(date - 604800000) // a week
            .append(StringUtils.isBlank(project) ? ")" : ") AND (" + SuiteResult.PROJECT_NAME + " LIKE '" + project
                + "%')")
            .append(" ORDER BY " + SuiteResult.START_TIME + " DESC) AS T")
            .append(" GROUP BY " + SuiteResult.JOB_NAME)
            .append(" ORDER BY " + SuiteResult.JOB_NAME + ";").toString();
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            list = this.dumpResultSetToList(rs);
            if (date < System.currentTimeMillis()) {
                LOG.debug("cache history data");
//                LOADED_LACASE_JOBS_RESULT.put(date + project, list);
            }
            return list;
        }
    }

    public List<Map<String, Object>> getSuitesResult(String project, long startTime, long stopTime, int numberOfEntries,
        String suiteName, String jobName, boolean invisibleIncluded)
        throws NamingException, SQLException {
        String sql = "SELECT * FROM " + SuiteResult.TABLE_NAME
            + " WHERE (" + SuiteResult.START_TIME + " > ?)"
            + " AND (" + SuiteResult.STOP_TIME + " < ?)";
        if (StringUtils.isNotBlank(suiteName)) {
            sql += " AND (" + SuiteResult.SUITE_NAME + " = ?)";
        } else if (StringUtils.isNotBlank(jobName)) {
            sql += " AND (" + SuiteResult.JOB_NAME + " = ?)";
        }
        if (StringUtils.isNotBlank(project)) {
            sql += " AND (" + SuiteResult.PROJECT_NAME + " LIKE ?)";
        }
        if (!invisibleIncluded) {
            sql += " AND NOT " + SuiteResult.INVISIBLE_ENTRY;
        }
        sql += " ORDER BY " + SuiteResult.START_TIME + " DESC;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setLong(1, startTime);
            stmt.setLong(2, stopTime);
            if (StringUtils.isNotBlank(suiteName)) {
                stmt.setString(3, suiteName);
            } else if (StringUtils.isNotBlank(jobName)) {
                stmt.setString(3, jobName);
            }
            if (StringUtils.isNotBlank(project)) {
                if (StringUtils.isNotBlank(suiteName) || StringUtils.isNotBlank(jobName)) {
                    stmt.setString(4, project + "%");
                } else {
                    stmt.setString(3, project + "%");
                }
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

    public List<Map<String, Object>> getCasesResult(String srid) throws NamingException, SQLException {
        String sql = "SELECT * FROM " + CaseResult.TABLE_NAME + " TR "
            + "INNER JOIN " + TaskCase.TABLE_NAME + " TC "
            + "ON TR.CASE_CASE_ID = TC.CASE_CASE_ID "
            + "WHERE " + CaseResult.SUITE_RESULT + " = ? "
            + "ORDER BY " + CaseResult.START_TIME + " DESC;";
        try (Connection conn = this.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, srid);
            LOG.trace("{}", stmt);
            ResultSet rs = stmt.executeQuery();
            return this.dumpResultSetToList(rs);
        }
    }

    public List<Map<String, Object>> getCasesResult(List<String> srids) throws NamingException, SQLException {
        String sql = "SELECT * FROM " + CaseResult.TABLE_NAME + " TR "
            + "INNER JOIN " + TaskCase.TABLE_NAME + " TC "
            + "ON TR.CASE_CASE_ID = TC.CASE_CASE_ID "
            + "WHERE " + CaseResult.SUITE_RESULT + " IN (" + StringUtils.join(srids, ",") + ") "
            + "ORDER BY " + TaskCase.SUITE_CLASS + "," + TaskCase.CASE_CLASS + ","
            + TaskCase.CASE_METHOD + "," + TaskCase.CASE_DATA_INFO + " DESC;";
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

        String tr = "SELECT * FROM " + CaseResult.TABLE_NAME
            + " WHERE " + CaseResult.EXECUTION_RESULT
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
                rs.updateObject(cn, sr.opt(cn));
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

        JSONArray trs = sr.getJSONArray("CASE_results");
        int len = trs.length();

        try (Connection conn = this.getConnection()) {
            String sql = String.format("SELECT * FROM %s WHERE %s=? AND %s=? AND %s=? AND %s=? AND %s=?;",
                TaskCase.TABLE_NAME,
                TaskCase.SUITE_CLASS,
                TaskCase.CASE_CLASS,
                TaskCase.CASE_METHOD,
                TaskCase.CASE_DATA_INFO,
                TaskCase.CASE_DATA
            );
            PreparedStatement stmt
                = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setMaxRows(1);
            for (int i = 0; i < len; i++) {
                JSONObject tr = trs.getJSONObject(i);
                stmt.setString(1, tr.getString(TaskCase.SUITE_CLASS));
                stmt.setString(2, tr.getString(TaskCase.CASE_CLASS));
                stmt.setString(3, tr.getString(TaskCase.CASE_METHOD));
                stmt.setString(4, tr.getString(TaskCase.CASE_DATA_INFO));
                stmt.setString(5, tr.getString(TaskCase.CASE_DATA));
                ResultSet rs = stmt.executeQuery();
                if (!rs.first()) {
                    rs.moveToInsertRow();
                    rs.updateString(TaskCase.SUITE_CLASS, tr.getString(TaskCase.SUITE_CLASS));
                    rs.updateString(TaskCase.CASE_CLASS, tr.getString(TaskCase.CASE_CLASS));
                    rs.updateString(TaskCase.CASE_METHOD, tr.getString(TaskCase.CASE_METHOD));
                    rs.updateString(TaskCase.CASE_DATA_INFO, tr.getString(TaskCase.CASE_DATA_INFO));
                    rs.updateString(TaskCase.CASE_DATA, tr.getString(TaskCase.CASE_DATA));
                    rs.insertRow();
                    rs.last();
                    rs.updateRow();
                    rs = stmt.executeQuery();
                    rs.first();
                }
                tr.put(TaskCase.TASK_CASE_ID, rs.getLong(TaskCase.TASK_CASE_ID));
            }
            LOG.debug("tcid updated");
        }

        try (Connection conn = this.getConnection()) {
            String sql = "SELECT * FROM " + CaseResult.TABLE_NAME + " WHERE " + CaseResult.SUITE_RESULT + " = ?;";
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
                    rs.updateObject(cn, tr.opt(cn));
                }
                rs.insertRow();
                rs.last();
                rs.updateRow();
            }
            LOG.debug("trs imported");
        }

        try (Connection conn = this.getConnection()) {
            String sql = "SELECT * FROM " + CaseResultMetric.TABLE_NAME + ";";
            PreparedStatement stmt = conn.prepareStatement(sql,
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            stmt.setMaxRows(1);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 0; i < len; i++) {
                JSONArray jarr = trs.getJSONObject(i).optJSONArray("CASE_result_metrics");
                if (jarr == null) {
                    continue;
                }
                int l = jarr.length();
                for (int j = 0; j < l; j++) {
                    JSONObject trm = jarr.getJSONObject(j);
                    rs.moveToInsertRow();
                    for (int col = 1; col <= rsmd.getColumnCount(); col++) {
                        String cn = rsmd.getColumnLabel(col);
                        if (cn.equals(CaseResultMetric.CASE_RESULT_METRIC_ID)) {
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

    List<Map<String, Object>> getCaseMetrics(String srid) throws NamingException, SQLException {
        String sql = "SELECT * FROM " + CaseResultMetric.TABLE_NAME + " trm JOIN " + CaseResult.TABLE_NAME + " tr"
            + " ON trm." + CaseResultMetric.CASE_RESULT_ID + "=" + "tr." + CaseResult.CASE_RESULT_ID
            + " WHERE " + CaseResult.SUITE_RESULT + "=?"
            + " ORDER BY " + CaseResultMetric.METRIC_GROUP + "," + CaseResultMetric.METRIC_NAME + ";";
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
