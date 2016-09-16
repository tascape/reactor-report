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

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
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

    private String host = "";

    private String user = "";

    private String pass = "";

    private String projectId = "";

    private String suiteId = "";

    private String sectionIds = "";

    @Inject
    private MySqlBaseBean db;

    @PostConstruct
    public void init() {
        this.getParameters();
    }

    public void share(ActionEvent actionEvent) {
        LOG.debug("srid {}", srid);
        LOG.debug("host {}", host);
        LOG.debug("user {}", user);
        LOG.debug("pass {}", pass);
        LOG.debug("project {}", projectId);
        LOG.debug("suite {}", suiteId);
        LOG.debug("sections {}", sectionIds);

        if (StringUtils.isNotBlank(srid)) {
            try {
                Map<String, Object> sr = this.db.getSuiteResult(srid);
                List<Map<String, Object>> trs = this.db.getCasesResult(srid);

            } catch (NamingException | SQLException ex) {
                throw new RuntimeException(ex);
            }
            addMessage("exported");
        }
    }

    public String getSrid() {
        return srid;
    }

    public void setSrid(String srid) {
        this.srid = srid;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    private void getParameters() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String v = map.get("srid");
        if (v != null) {
            this.srid = v;
            LOG.debug("srid={}", this.srid);
        }
    }

    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, null);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }
}
