(function (thr, $, undefined) {
    thr.setTimeZoneCookie = function () {
        var offset = new Date().getTimezoneOffset();
        document.cookie = "TIMEZONE_COOKIE=" + offset; //Cookie name with value
    };

    thr.querySuiteResults = function (suite, job, project) {
        var start = PF("wv_start").getDate().getTime();
        var stop = PF("wv_stop").getDate().getTime();
        var number = PF("wv_number").value;
        var invisible = PF("wv_invisible").getSelectedValue();
        var search = "?start=" + start + "&stop=" + stop + "&number=" + number + "&invisible=" + invisible;
        if (suite) {
            search += "&suite=" + suite;
        }
        if (job) {
            search += "&job=" + job;
        }
        if (project) {
            search += "&project=" + project;
        }
        location.search = search;
    };

    thr.querySuiteResultDetail = function (suite, job, project) {
        var start = PF("wv_start").getDate().getTime();
        var stop = PF("wv_stop").getDate().getTime();
        var number = PF("wv_number").value;
        var invisible = PF("wv_invisible").getSelectedValue();
        var search = "?start=" + start + "&stop=" + stop + "&number=" + number + "&invisible=" + invisible
                + "&suite=" + suite + "&job=" + job + "&project=" + project;
        location.replace("suite_result_history_detail.xhtml" + search);
    };


    thr.querySuiteMetricDetail = function (suite, job, project) {
        var start = PF("wv_start").getDate().getTime();
        var stop = PF("wv_stop").getDate().getTime();
        var number = PF("wv_number").value;
        var invisible = PF("wv_invisible").getSelectedValue();
        var search = "?start=" + start + "&stop=" + stop + "&number=" + number + "&invisible=" + invisible
                + "&suite=" + suite + "&job=" + job + "&project=" + project;
        location.replace("suite_metric_history_detail.xhtml" + search);
    };
}(window.thr = window.thr || {}, jQuery));

thr.setTimeZoneCookie();
