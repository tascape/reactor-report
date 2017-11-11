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

import com.google.common.collect.Lists;
import com.tascape.reactor.db.SuiteResult;
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

    public static final String ALL_PROJECTS = "ALL_PROJECTS";

    private static final long serialVersionUID = 1L;

    @Inject
    private MySqlBaseBean db;

    private final List<String> projects = new ArrayList<>();

    private final List<Integer> weekList = Lists.newArrayList(1, 2, 4, 8, 26, 52, 104);

    private String project = ALL_PROJECTS;

    private int weeks = 2;

    private final List<Map<String, Object>> results = new ArrayList<>();

    private HorizontalBarChartModel barModel;

    private int chartHeight = 88;

    private int total;

    private int fail;

    private MenuModel menuProjects;

    private MenuModel menuWeeks;

    private MenuModel menuModel;

    private String weekString = "";

    @PostConstruct
    public void init() {
        this.getParameters();
        try {
            projects.addAll(this.db.loadProjects());
            Collections.sort(projects);
        } catch (SQLException | NamingException ex) {
            LOG.warn(ex.getMessage());
        }

        menuProjects = new DefaultMenuModel();
        for (String p : projects) {
            DefaultMenuItem item = new DefaultMenuItem(p);
            item.setUrl("dashboard.xhtml?project=" + p + "&weeks=" + weeks);
            item.setStyle("white-space: nowrap;");
            menuProjects.addElement(item);
        }

        menuWeeks = new DefaultMenuModel();
        for (int w : weekList) {
            DefaultMenuItem item = new DefaultMenuItem(this.getWeekString(w));
            item.setUrl("dashboard.xhtml?project=" + project + "&weeks=" + w);
            item.setStyle("white-space: nowrap;");
            getMenuWeeks().addElement(item);
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

    public MenuModel getMenuProjects() {
        return menuProjects;
    }

    public MenuModel getMenuWeeks() {
        return menuWeeks;
    }

    public int getWeeks() {
        return weeks;
    }

    public void setWeeks(int weeks) {
        this.weeks = weeks;
    }

    /**
     * @return the getWeekString
     */
    public String getWeekString() {
        return weekString;
    }

    /**
     * @return the menuModel
     */
    public MenuModel getMenuModel() {
        return menuModel;
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

        Axis xAxis = new LinearAxis("Total Number of Cases");
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

    private String getWeekString(int weeks) {
        return "last " + (weeks > 1 ? weeks + " weeks" : "week");
    }

    private void getParameters() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String v = map.get("project");
        if (StringUtils.isBlank(v)) {
            this.project = ALL_PROJECTS;
        } else {
            this.project = v;
        }
        v = map.get("weeks");
        if (v != null) {
            try {
                this.weeks = Integer.parseInt(v);
            } catch (Exception ex) {
                this.weeks = 2;
            }
            if (this.weeks < 1 || this.weeks > 52) {
                this.weeks = 2;
            }
        }
        this.weekString = this.getWeekString(weeks);
    }
}
