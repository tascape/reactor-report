/*
 * Copyright (c) 2015 - present Nebula Bay.
 * All rights reserved.
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

import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
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
public class DashboardViewByJob extends DashboardView {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardViewByJob.class);

    private static final long serialVersionUID = 3482194726573629L;

    @Inject
    private MySqlBaseBean db;

    @PostConstruct
    @Override
    public void init() {
        super.init();
        try {
            String project = this.getProject();
            if (project.equals(ALL_PROJECTS)) {
                project = "";
            }
            int weeks = this.getWeeks();
            this.setResults(this.db.getLatestJobsResult(project, weeks));
        } catch (NamingException | SQLException ex) {
            throw new RuntimeException(ex);
        }

        this.initBarModel();
    }
}
