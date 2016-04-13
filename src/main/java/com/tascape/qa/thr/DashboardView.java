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
package com.tascape.qa.thr;

import com.tascape.qa.th.db.SuiteResult;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.HorizontalBarChartModel;
import org.primefaces.model.chart.LegendPlacement;
import org.primefaces.model.chart.LinearAxis;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@Named
@RequestScoped
public class DashboardView implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardView.class);

    private static final long serialVersionUID = 1L;

    @Inject
    private MySqlBaseBean db;

    private final List<String> projects = new ArrayList<>();

    private String project = "";

    private final List<Map<String, Object>> results = new ArrayList<>();

    private HorizontalBarChartModel barModel;

    private int chartHeight = 88;

    private int total;

    private int fail;

    private MenuModel model;

    @PostConstruct
    public void init() {
        this.getParameters();
        try {
            projects.addAll(this.db.loadProjects());
            Collections.sort(projects);
        } catch (SQLException | NamingException ex) {
            LOG.warn(ex.getMessage());
        }

        model = new DefaultMenuModel();
        for (String p : projects) {
            DefaultMenuItem item = new DefaultMenuItem(StringUtils.isBlank(p) ? "all projects" : p);
            item.setUrl("dashboard.xhtml?project=" + p);
            item.setIcon("ui-icon-arrowreturnthick-1-n");
            model.addElement(item);
        }
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
        this.results.addAll(results);
        this.results.forEach(row -> {
            row.put("sort", "a");
        });
    }

    public void setBarModel(HorizontalBarChartModel barModel) {
        this.barModel = barModel;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public List<String> getProjects() {
        return projects;
    }

    public MenuModel getModel() {
        return model;
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
            t += Integer.parseInt(result.get(SuiteResult.NUMBER_OF_TESTS) + "");
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

    private void getParameters() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String v = map.get("project");
        if (v != null) {
            this.project = v;
        }
    }
}
