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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.tascape.qa.th.db.SuiteResult;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.CategoryAxis;
import org.primefaces.model.chart.LegendPlacement;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author linsong wang
 */
@Named
@RequestScoped
public class HistoryViewBySuite extends AbstractReportView implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(HistoryViewBySuite.class);

    private static final long serialVersionUID = 1L;

    @Inject
    private MySqlBaseBean db;

    private final Table<String, LocalDate, Map<String, Object>> history = HashBasedTable.create();

    private final Set<String> suites = new HashSet<>();

    private final Set<LocalDate> dates = new TreeSet<>((LocalDate o1, LocalDate o2) -> o2.compareTo(o1));

    private final Map<LocalDate, Integer> totals = new HashMap<>();

    private final Map<LocalDate, Integer> fails = new HashMap<>();

    private LineChartModel chartModel;

    private String project = "";

    private int interval = 1;

    private int entries = 30;

    @PostConstruct
    public void init() {
        this.getParameters();
        LOG.debug("interval {} day(s)", this.interval);
        LOG.debug("number of entries {}", this.entries);

        this.loadData();
        this.chartModel = this.createChartModel();
    }

    public LineChartModel getChartModel() {
        return chartModel;
    }

    public int getInterval() {
        return interval;
    }

    public int getEntries() {
        return entries;
    }

    public Set<LocalDate> getDates() {
        return this.dates;
    }

    public List<String> getSuites() {
        List<String> l = new ArrayList<>(this.suites);
        Collections.sort(l);
        return l;
    }

    public Table<String, LocalDate, Map<String, Object>> getHistory() {
        return this.history;
    }

    public Map<LocalDate, Integer> getTotals() {
        return totals;
    }

    public Map<LocalDate, Integer> getFails() {
        return fails;
    }

    public String getProject() {
        return project;
    }

    private void loadData() {
        LocalDate now = LocalDate.now();

        ExecutorService es = Executors.newFixedThreadPool(1);
        CompletionService<Map.Entry<LocalDate, List<Map<String, Object>>>> cs = new ExecutorCompletionService<>(es);

        List<Future<Map.Entry<LocalDate, List<Map<String, Object>>>>> futures = new ArrayList<>();
        IntStream.range(0, entries).forEach(i -> {
            LocalDate date = now.minusDays(i * interval);
            futures.add(cs.submit(new Snapshot(date)));
        });
        Set<LocalDate> ds = new HashSet<>();
        futures.forEach(f -> {
            try {
                Map.Entry<LocalDate, List<Map<String, Object>>> entry = f.get();
                LocalDate date = entry.getKey();
                ds.add(date);
                entry.getValue().forEach(row -> {
                    String suite = row.get(SuiteResult.SUITE_NAME).toString();
                    suites.add(suite);
                    history.put(suite, date, row);
                });
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("", ex);
            }
        });
        es.shutdown();
        ds.forEach((LocalDate date) -> {
            int total = history.column(date).values().stream()
                .mapToInt(row -> (Integer) row.get(SuiteResult.NUMBER_OF_TESTS))
                .sum();
            int fail = history.column(date).values().stream()
                .mapToInt(row -> (Integer) row.get(SuiteResult.NUMBER_OF_FAILURE))
                .sum();
            if (total > 0) {
                dates.add(date);
                totals.put(date, total);
                fails.put(date, fail);
            }
        });
    }

    private LineChartModel createChartModel() {
        LineChartModel model = new LineChartModel();
        model.setLegendPosition("n");
        model.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
        model.setAnimate(true);
        model.setShowPointLabels(true);

        LineChartSeries ts = new LineChartSeries();
        ts.setLabel("Total Number of Tests");
        model.addSeries(ts);
        this.getDates().forEach(date -> {
            ts.set(date.toString(), totals.get(date));
        });

        Axis xAxis = new CategoryAxis();
        xAxis.setTickAngle(-45);
        model.getAxes().put(AxisType.X, xAxis);
        return model;
    }

    private class Snapshot implements Callable<Map.Entry<LocalDate, List<Map<String, Object>>>> {
        private final LocalDate date;

        public Snapshot(LocalDate date) {
            this.date = date;
        }

        @Override
        public Map.Entry<LocalDate, List<Map<String, Object>>> call() throws Exception {
            ZoneId zi = ZoneId.ofOffset("", ZoneOffset.ofHoursMinutes(HistoryViewBySuite.this.clientTimezone, 0));
            long epoch = date.atStartOfDay(zi).plusDays(1).toEpochSecond() * 1000;
            return new AbstractMap.SimpleEntry<>(date, db.getLatestSuitesResult(epoch, project));
        }
    }

    private void getParameters() {
        Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String v = map.get("interval");
        if (v != null) {
            this.interval = Integer.parseInt(v);
        }
        v = map.get("entries");
        if (v != null) {
            this.entries = Integer.parseInt(v);
            if (this.entries > 30) {
                this.entries = 30;
            }
        }
        v = map.get("project");
        if (v != null) {
            this.project = v;
            LOG.debug("project={}", this.project);
        }
        LOG.debug("client timezone {}", this.clientTimezone);
    }
}
