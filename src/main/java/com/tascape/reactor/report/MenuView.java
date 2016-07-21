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
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
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
@ApplicationScoped
public class MenuView implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(MenuView.class);

    private static final long serialVersionUID = 749369214803L;

    @Inject
    private MySqlBaseBean db;

    private static final Map<String, String> projects = new HashMap<>();

    private List<Map<String, Object>> results;

    private HorizontalBarChartModel barModel;

    private int chartHeight = 88;

    private int total;

    private int fail;

    static {
        projects.put("advertising", "advertising");
        projects.put("recommendation", "recommendation");
    }

    public void showProject(String project) throws IOException {
        FacesContext.getCurrentInstance().getExternalContext().redirect("?project=" + project);
    }

    public List<Map<String, Object>> getResults() {
        return results;
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

    public int getChartHeight() {
        return chartHeight;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
        this.results.forEach(row -> {
            row.put("sort", "a");
        });
    }

    public void setBarModel(HorizontalBarChartModel barModel) {
        this.barModel = barModel;
    }

    public Map<String, String> getProjects() {
        return projects;
    }

    protected HorizontalBarChartModel initBarModel() {
        barModel = new HorizontalBarChartModel();
        barModel.setLegendPosition("n");
        barModel.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
        barModel.setLegendCols(2);
        barModel.setSeriesColors("00ff00, ff0000");
        barModel.setStacked(true);
        barModel.setBarMargin(0);
        barModel.setBarPadding(0);
        barModel.setAnimate(false);

        ChartSeries failSeries = new ChartSeries();
        failSeries.setLabel("FAIL");
        ChartSeries passSeries = new ChartSeries();
        passSeries.setLabel("PASS");
        int f = 0;
        int t = 0;
        for (Map<String, Object> result : this.results) {
            f += Integer.parseInt(result.get(SuiteResult.NUMBER_OF_FAILURE) + "");
            t += Integer.parseInt(result.get(SuiteResult.NUMBER_OF_CASES) + "");
        }
        LOG.debug("fail {}, total {}", f, t);
        failSeries.set(" ", f);
        passSeries.set(" ", t - f);
        barModel.addSeries(passSeries);
        barModel.addSeries(failSeries);

        this.total = t;
        this.fail = f;

        Axis xAxis = new LinearAxis("Total Number of Tests");
        xAxis.setTickAngle(-90);
        xAxis.setMin(0);
        if (t <= 28) {
            xAxis.setTickInterval("1");
        }
        xAxis.setMax(t);
        xAxis.setTickFormat("%" + (t + "").length() + "d");
        barModel.getAxes().put(AxisType.X, xAxis);

        this.chartHeight = 86 + (t + "").length() * 7;
        return barModel;
    }
}
