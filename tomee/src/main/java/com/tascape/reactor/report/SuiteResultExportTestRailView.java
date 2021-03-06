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

import com.codepine.api.testrail.TestRail;
import com.codepine.api.testrail.model.CaseField;
import com.codepine.api.testrail.model.Project;
import com.codepine.api.testrail.model.Result;
import com.codepine.api.testrail.model.ResultField;
import com.codepine.api.testrail.model.Run;
import com.codepine.api.testrail.model.Section;
import com.codepine.api.testrail.model.Suite;
import com.google.common.collect.Lists;
import com.tascape.reactor.ExecutionResult;
import com.tascape.reactor.Reactor;
import com.tascape.reactor.db.CaseResult;
import com.tascape.reactor.db.SuiteResult;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@Named
@RequestScoped
public class SuiteResultExportTestRailView implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(SuiteResultExportTestRailView.class);

    private static final long serialVersionUID = 1L;

    private String srid = "";

    private String logBaseUrl = "";

    private String url = "";

    private String user = "";

    private String pass = "";

    private String projectId = "";

    private String suiteId = "";

    private String sectionIds = "";

    private boolean complete = true;

    private String runLink = "";

    @Inject
    private MySqlBaseBean db;

    @PostConstruct
    public void init() {
        try {
            this.getParameters();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void share(ActionEvent actionEvent) {
        try {
            this.share0();
        } catch (SQLException | NamingException ex) {
            LOG.error("error", ex);
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "error during sharing", ex.getMessage());
            addMessage(msg);
        }
        addMessage(new FacesMessage(FacesMessage.SEVERITY_INFO, "shared", ""));
    }

    private void share0() throws NamingException, SQLException {
        LOG.debug("srid {}", srid);
        LOG.debug("logBaseUrl {}", logBaseUrl);
        LOG.debug("url {}", url);
        LOG.debug("user {}", user);
        LOG.debug("pass {}", pass);
        LOG.debug("project {}", projectId);
        LOG.debug("suite {}", suiteId);
        LOG.debug("sections {}", sectionIds);
        LOG.debug("complete {}", complete);

        if (StringUtils.isBlank(srid)) {
            return;
        }
        TestRail testRail = TestRail.builder(url, user, pass).applicationName(Reactor.class.getSimpleName()).build();
        int pid = Integer.parseInt(projectId);
        int sid = Integer.parseInt(suiteId);
        List<Integer> sids = Stream.of(sectionIds.split(","))
                .map(id -> Integer.parseInt(id.trim()))
                .collect(Collectors.toList());
        Project project = testRail.projects().get(pid).execute();
        LOG.debug("testrail project {}", project.getName());
        Suite suite = testRail.suites().get(sid).execute();
        LOG.debug("testrail suite {}", suite.getName());
        final List<String> names = Lists.newArrayList(suite.getName());
        sids.forEach(sectionId -> {
            Section section = testRail.sections().get(sectionId).execute();
            names.add(section.getName());
            LOG.debug("testrail section {} - {}", sectionId, section.getName());
        });

        Map<String, Object> sr = this.db.getSuiteResult(srid);
        List<Map<String, Object>> trs = this.db.getCasesResult(srid);
        srid = "";

        List<CaseField> cfs = testRail.caseFields().list().execute();
        List<Integer> cids = testRail.cases().list(pid, sid, cfs).execute().stream()
                .filter(c -> sids.isEmpty() || sids.contains(c.getSectionId()))
                .map(c -> {
                    int id = c.getId();
                    LOG.debug("{} - {}", c.getSectionId(), id);
                    return id;
                })
                .collect(Collectors.toList());

        Run run = testRail.runs().add(pid, new Run()
                .setSuiteId(sid)
                .setIncludeAll(false)
                .setCaseIds(cids)
                .setName(StringUtils.join(names, " - ") + ": "
                        + DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT
                                .format(sr.get(SuiteResult.START_TIME))))
                .execute();
        this.runLink = run.getUrl();

        try {
            List<ResultField> customResultFields = testRail.resultFields().list().execute();
            trs.parallelStream().forEach(cr -> {
                String exid = cr.get(CaseResult.EXTERNAL_ID) + "";
                LOG.debug("case result external id {}", exid);
                try {
                    int crid = Integer.parseInt(exid);
                    int status = ExecutionResult.isPass(((String) cr.get(CaseResult.EXECUTION_RESULT))) ? 1 : 5;
                    testRail.results().addForCase(
                            run.getId(),
                            crid,
                            new Result().setStatusId(status)
                                    .addCustomField("custom_execmode", 0)
                                    .setComment(this.logBaseUrl + cr.get(CaseResult.LOG_DIR) + "/log.html"),
                            customResultFields
                    ).execute();
                } catch (Exception ex) {
                    LOG.error("cannot add a new test case result", ex);
                }
            });
        } finally {
            if (complete) {
                testRail.runs().close(run.getId()).execute();
            }
        }
    }

    public String getSrid() {
        return srid;
    }

    public void setSrid(String srid) {
        this.srid = srid;
    }

    public String getLogBaseUrl() {
        return logBaseUrl;
    }

    public void setLogBaseUrl(String logBaseUrl) {
        this.logBaseUrl = logBaseUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(String suiteId) {
        this.suiteId = suiteId;
    }

    public String getSectionIds() {
        return sectionIds;
    }

    public void setSectionIds(String sectionIds) {
        this.sectionIds = sectionIds;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public String getRunLink() {
        return runLink;
    }

    public void setRunLink(String runLink) {
        this.runLink = runLink;
    }

    private void getParameters() throws URISyntaxException {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String v = map.get("srid");
        if (v != null) {
            this.srid = v;
            LOG.debug("srid={}", this.srid);
        }
        String ref = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("referer");
        URI u = new URI(ref);
        this.logBaseUrl = u.getScheme() + "://" + u.getHost() + ":" + u.getPort() + "/logs/" + srid + "/";
        LOG.debug("logBaseUrl={}", logBaseUrl);
    }

    public void addMessage(FacesMessage message) {
        FacesContext.getCurrentInstance().addMessage(null, message);
    }
}
