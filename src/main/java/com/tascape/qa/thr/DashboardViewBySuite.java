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

import java.io.Serializable;
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
public class DashboardViewBySuite extends DashboardView implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardViewBySuite.class);

    private static final long serialVersionUID = 5749324382L;

    @Inject
    private MySqlBaseBean db;


    @PostConstruct
    public void init() {
        try {
            this.setResults(this.db.getLatestSuitesResult());
        } catch (NamingException | SQLException ex) {
            throw new RuntimeException(ex);
        }

        this.initBarModel();
    }
}
