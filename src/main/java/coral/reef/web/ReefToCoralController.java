/*
 * Copyright 2015 the original author or authors.
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
package coral.reef.web;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

import coral.reef.service.ReefHandler;
import coral.reef.service.ReefService;

/**
 * Controller to link up to a specific experiment.
 * 
 * @author Markus Schaffner
 *
 */
@Controller
public class ReefToCoralController {

    private final static String REEF_EXP = "reefExp";
    private final static String REEF_ID = "reefId";

    @Autowired
    ServletContext servletContext;

    @Autowired
    ReefService service;

    @RequestMapping(value = "/{exp}/**")
    public void dispatchToExpHandler(@PathVariable("exp") String exp,
            HttpSession httpSession, HttpServletResponse httpResponse,
            HttpServletRequest httpRequest) throws IOException {

        httpResponse.setHeader("Cache-Control",
                "no-cache, no-store, max-age=0, must-revalidate");
        httpResponse.setHeader("Pragms", "no-cache");
        httpResponse.setHeader("Expires", "0");

        Integer id = (Integer) httpSession.getAttribute(REEF_ID);
        String sessionExp = (String) httpSession.getAttribute(REEF_EXP);

        String path = ((String) httpRequest
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))
                .substring(exp.length() + 2);

        ReefHandler handler = service.handler(exp);

        if (path.startsWith(handler.startMarker())) {

            if (sessionExp != null || id != null) {
                // TODO remove old client (?)
            }

            String query = httpRequest.getQueryString();
            query = (query == null) ? "" : query;
            id = handler.addClient(null);
            String result = handler.process(id, path, query);
            result = service.replaceHost(exp, result);
            httpSession.setAttribute(REEF_ID, id);
            httpSession.setAttribute(REEF_EXP, exp);
            httpResponse.setContentType("text/html");
            httpResponse.getWriter().println(result);

        } else if (path.startsWith(handler.refreshMarker())) {

            if (sessionExp != null || id != null) {
                // TODO remove old client (?)
            }

            String query = httpRequest.getQueryString();
            query = (query == null) ? "" : query;
            id = Integer.parseInt(query.replaceAll("[^\\d]", ""));
            id = handler.addClient(id);
            String result = handler.process(id, "", "?refreshid=" + id);
            result = service.replaceHost(exp, result);
            httpSession.setAttribute(REEF_ID, id);
            httpSession.setAttribute(REEF_EXP, exp);
            httpResponse.setContentType("text/html");
            httpResponse.getWriter().println(result);

        } else if (path.startsWith(handler.processMarker())
                && sessionExp != null && sessionExp.equals(exp)) {

            /*
             * PROCESS
             */

            String query = httpRequest.getQueryString();
            query = (query == null) ? "" : query;
            String result = handler.process(id, path, query);
            result = service.replaceHost(exp, result);
            httpResponse.setContentType("text/html");
            httpResponse.getWriter().println(result);

        } else if (path.startsWith(handler.serverMarker())) {

            /*
             * SERVER
             */

            String result = handler.server(
                    path.substring(handler.serverMarker().length() + 1),
                    httpRequest.getParameterMap());
            result = service.replaceHost(exp, result);
            httpResponse.setContentType("text/html");
            httpResponse.getWriter().println(result);

        } else if (handler.getResMap().containsKey(path)) {

            /*
             * RESOURCE
             */

            File f = handler.getResMap().get(path);
            FileSystemResource fr = new FileSystemResource(f);
            String type = servletContext.getMimeType(path);
            httpResponse.setContentType(type);

            IOUtils.copy(fr.getInputStream(), httpResponse.getOutputStream());
        } else {
            // no match = no response => should propagate to next controller
            System.out.println(path + " - " + handler.getResMap());
        }

    }

}
