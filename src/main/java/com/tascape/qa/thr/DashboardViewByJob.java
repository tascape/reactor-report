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
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
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
public class DashboardViewByJob implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardViewByJob.class);

    private static final long serialVersionUID = 1L;

    @Inject
    private MySqlBaseBean db;

    private List<Map<String, Object>> results;

    private List<Map<String, Object>> resultsSelected;

    private HorizontalBarChartModel barModel;

    private int total;

    private int fail;

    @PostConstruct
    public void init() {
        try {
            this.results = this.db.getLatestJobsResult();
            this.results.forEach(row -> {
                row.put("sort", "a");
            });
        } catch (NamingException | SQLException ex) {
            throw new RuntimeException(ex);
        }

        this.barModel = this.initBarModel();
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public List<Map<String, Object>> getResultsSelected() {
        return resultsSelected;
    }

    public void setResultsSelected(List<Map<String, Object>> resultsSelected) {
        this.resultsSelected = resultsSelected;
    }

    public HorizontalBarChartModel getBarModel() {
        return barModel;
    }

    public int getTotal() {
        return total;
    }

    public int getFail() {
        return fail;
    }

    private HorizontalBarChartModel initBarModel() {
        HorizontalBarChartModel model = new HorizontalBarChartModel();
        model.setLegendPosition("n");
        model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
        model.setLegendCols(2);
        model.setSeriesColors("00ff00, ff0000");
        model.setStacked(true);
        model.setBarMargin(0);
        model.setBarPadding(0);
        model.setAnimate(true);

        ChartSeries failSeries = new ChartSeries();
        failSeries.setLabel("FAIL");
        ChartSeries passSeries = new ChartSeries();
        passSeries.setLabel("PASS");
        int f = 0;
        int t = 0;
        for (Map<String, Object> result : this.results) {
            f += Integer.parseInt(result.get(SuiteResult.NUMBER_OF_FAILURE) + "");
            t += Integer.parseInt(result.get(SuiteResult.NUMBER_OF_TESTS) + "");
        }
        LOG.debug("fail {}, total {}", f, t);
        failSeries.set(" ", f);
        passSeries.set(" ", t - f);
        model.addSeries(passSeries);
        model.addSeries(failSeries);

        this.total = t;
        this.fail = f;

        Axis xAxis = new LinearAxis("Total Number of Tests");
        xAxis.setTickAngle(-90);
        xAxis.setMin(0);
        xAxis.setTickInterval((t / 100 + 1) + "");
        xAxis.setMax(t);
        xAxis.setTickFormat("%03d");
        model.getAxes().put(AxisType.X, xAxis);
        return model;
    }
}
