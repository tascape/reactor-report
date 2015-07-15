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

import com.tascape.qa.th.db.SuiteResult;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.HorizontalBarChartModel;
import org.primefaces.model.chart.LegendPlacement;
import org.primefaces.model.chart.LinearAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@Named
@RequestScoped
public class SuiteResultView implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(SuiteResultView.class);

    private static final long serialVersionUID = 1L;

    @Inject
    private MySqlBaseBean db;

    private String srid;

    private boolean toggleInvisible = false;

    private Map<String, Object> suiteResult;

    private List<Map<String, Object>> testsResult;

    private List<Map<String, Object>> suiteProperties;

    private HorizontalBarChartModel barModel;

    @PostConstruct
    public void init() {
        this.getParameters();

        try {
            this.suiteResult = this.db.getSuiteResult(this.srid);
            boolean invisible = this.suiteResult.get(SuiteResult.INVISIBLE_ENTRY).equals(1);
            if (this.toggleInvisible) {
                this.setInvisible(!invisible);
                return;
            }
            this.testsResult = this.db.getTestsResult(this.srid);
            this.testsResult.stream().filter(row -> {
                return !(row.get("LOG_DIR") + "").isEmpty();
            });
            this.suiteProperties = this.db.getSuiteProperties(this.srid);
        } catch (NamingException | SQLException | IOException ex) {
            throw new RuntimeException(ex);
        }

        this.barModel = this.initBarModel();
    }

    public Map<String, Object> getSuiteResult() {
        return suiteResult;
    }

    public List<Map<String, Object>> getTestsResult() {
        return testsResult;
    }

    public List<Map<String, Object>> getSuiteProperties() {
        return suiteProperties;
    }

    public String getSrid() {
        return srid;
    }

    public HorizontalBarChartModel getBarModel() {
        return barModel;
    }

    private HorizontalBarChartModel initBarModel() {
        HorizontalBarChartModel model = new HorizontalBarChartModel();
        model.setLegendPosition("n");
        model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
        model.setLegendCols(2);
        model.setSeriesColors("ff0000, 00ff00");
        model.setStacked(true);
        model.setBarMargin(0);
        model.setBarPadding(0);
        model.setAnimate(true);

        ChartSeries fail = new ChartSeries();
        fail.setLabel("FAIL");
        ChartSeries pass = new ChartSeries();
        pass.setLabel("PASS");
        int f = (int) suiteResult.get(SuiteResult.NUMBER_OF_FAILURE);
        int t = (int) suiteResult.get(SuiteResult.NUMBER_OF_TESTS);
        fail.set(" ", f);
        pass.set(" ", t - f);
        model.addSeries(fail);
        model.addSeries(pass);

        Axis xAxis = new LinearAxis("Number of Tests");
        xAxis.setTickAngle(-90);
        xAxis.setMax(t);
        xAxis.setTickCount(t + 1);
        xAxis.setTickFormat("%03d");
        model.getAxes().put(AxisType.X, xAxis);
        return model;
    }

    private void setInvisible(boolean invisible) throws NamingException, SQLException, IOException {
        this.db.setSuiteResultInvisible(this.srid, invisible);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + context.getRequestServletPath() + "?srid=" + srid);
    }

    private void getParameters() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String v = map.get("srid");
        LOG.debug("srid = {}", v);
        if (v != null) {
            this.srid = v;
        }
        v = map.get("ti");
        LOG.debug("toggle invisible = {}", v);
        if (v != null) {
            this.toggleInvisible = Boolean.parseBoolean(v);
        }
    }
}
