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

import com.tascape.reactor.db.SuiteResult;
import com.tascape.reactor.db.TaskCase;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@Named
@RequestScoped
public class SuiteResultDetailHistoryView implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(SuiteResultDetailHistoryView.class);

    private static final long serialVersionUID = 1L;

    private String project = "";

    private long startTime = System.currentTimeMillis() - 5184000000L; // two months

    private long stopTime = System.currentTimeMillis() + 86400000L; // one day

    private int numberOfEntries = 100;

    private boolean invisibleIncluded = false;

    private String suiteName = "";

    private String jobName = "";

    @Inject
    private MySqlBaseBean db;

    private List<Map<String, Object>> suitesResult;

    private final List<Map<String, Map<String, Object>>> suiteHistoryDetail = new ArrayList<>();

    @PostConstruct
    public void init() {
        this.getParameters();

        try {
            this.suitesResult = this.db.getSuitesResult(project, this.startTime, this.stopTime, this.numberOfEntries,
                this.suiteName, this.jobName, this.invisibleIncluded);

            for (Map<String, Object> suiteResult : this.suitesResult) {
                String srid = suiteResult.get(SuiteResult.SUITE_RESULT_ID).toString();

                List<Map<String, Object>> testsResult = this.db.getTestsResult(srid);
                for (Map<String, Object> testResult : testsResult) {

                    boolean toAddOneRow = true;
                    for (Map<String, Map<String, Object>> testHistory : this.suiteHistoryDetail) {
                        Map<String, Object> tr = testHistory.get(srid);
                        if (tr != null) {
                            continue;
                        }

                        TaskCase testCase = new TaskCase(testResult);
                        TaskCase tc = new TaskCase(testHistory.get("TEST_CASE"));
                        if (testCase.equals(tc)) {
                            testHistory.put(srid, testResult);
                            toAddOneRow = false;
                            break;
                        }
                    }

                    if (toAddOneRow) {
                        Map<String, Map<String, Object>> testHistory = new HashMap<>();
                        testHistory.put("TEST_CASE", testResult);
                        testHistory.put(srid, testResult);
                        this.suiteHistoryDetail.add(testHistory); // add one row
                    }
                }
            }
        } catch (NamingException | SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public int getNumberOfEntries() {
        return numberOfEntries;
    }

    public boolean isInvisibleIncluded() {
        return invisibleIncluded;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public void setNumberOfEntries(int numberOfEntries) {
        this.numberOfEntries = numberOfEntries;
    }

    public void setInvisibleIncluded(boolean invisibleIncluded) {
        this.invisibleIncluded = invisibleIncluded;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public List<Map<String, Object>> getSuitesResult() {
        return suitesResult;
    }

    public List<Map<String, Map<String, Object>>> getSuiteHistoryDetail() {
        return suiteHistoryDetail;
    }

    public String getProject() {
        return project;
    }

    private void getParameters() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        LOG.trace("request parameters {}", map);
        String v = map.get("start");
        if (v != null) {
            this.startTime = Long.parseLong(v);
        }
        v = map.get("stop");
        if (v != null) {
            this.stopTime = Long.parseLong(v);
        }
        v = map.get("number");
        if (v != null) {
            this.numberOfEntries = Integer.parseInt(v);
        }
        v = map.get("invisible");
        if (v != null) {
            this.invisibleIncluded = Boolean.parseBoolean(v);
        }
        v = map.get("suite");
        if (v != null) {
            this.suiteName = v;
        }
        v = map.get("job");
        if (v != null) {
            this.jobName = v;
        }
        v = map.get("project");
        if (v != null) {
            this.project = v;
        }
    }
}
