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

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@Named
@RequestScoped
public class SuiteResultShareView implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(SuiteResultShareView.class);

    private static final long serialVersionUID = 1L;

    public static final List<String> DESTINATIONS = Lists.newArrayList("TestRail");

    @Inject
    private MySqlBaseBean db;

    private String srid;

    @PostConstruct
    public void init() {
        this.getParameters();
    }

    public String getSrid() {
        return srid;
    }

    private void getParameters() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String v = map.get("srid");
        LOG.debug("srid = {}", v);
        if (v != null) {
            this.srid = v;
        }
    }
}
