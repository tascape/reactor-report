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

import java.util.Map;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;

/**
 *
 * @author linsong wang
 */
public class AbstractReportView {

    @SuppressWarnings("ProtectedField")
    protected int clientTimezone = -7;

    public AbstractReportView() {
        Map<String, Object> cookies = FacesContext.getCurrentInstance().getExternalContext().getRequestCookieMap();
        Object cookie = cookies.get("TIMEZONE_COOKIE");
        if (cookie != null && cookie instanceof Cookie) {
            String tz = ((Cookie) cookie).getValue();
            clientTimezone = 0 - Integer.parseInt(tz) / 60;
        }
    }
}
