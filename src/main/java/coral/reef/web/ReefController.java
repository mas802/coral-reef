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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.HandlerMapping;

import coral.reef.domain.CoralBean;
import coral.reef.domain.CoralBeanRepository;
import coral.reef.service.ReefService;

@Controller
public class ReefController {

    @Autowired
    private ReefService reefService;

    @Autowired
    private CoralBeanRepository coralBeanRepository;

    @Autowired
    ApplicationContext appContext;

    @Autowired
    ServletContext servletContext;

    // @Autowired
    // private ServletContext servletContext;

    @RequestMapping("/admin/corals")
    public String list(Map<String, Object> model) {

        model.put("coralInfos", reefService.getCoralInfos());

        return "corals";
    }

    @RequestMapping("/admin/start")
    public String start(@RequestParam(value = "name") String name) {

        reefService.startCoralService(name);
        return "redirect:/admin/corals";
    }

    @RequestMapping("/admin/stop")
    public String stop(@RequestParam(value = "name") String name) {

        reefService.stopCoralService(name);
        return "redirect:/admin/corals";
    }

    @RequestMapping("/admin/edit")
    public String edit(
            @RequestParam(value = "name") String name,
            @RequestParam(value = "properties", required = false) String properties,
            @RequestParam(value = "basepath", required = false) String basepath,
            Map<String, Object> model) {

        CoralBean cb = coralBeanRepository.findOne(name);

        if (cb == null) {
            cb = new CoralBean();
            String content;
            try {
                Scanner scan = new Scanner(new ClassPathResource(
                        "reef.default.properties").getInputStream());
                scan.useDelimiter("\\Z");
                content = scan.next();
                cb.setProperties(content);
            } catch (IOException e) {
                content = e.getMessage();
            }
            cb.setName(name);
        }

        if (properties != null) {
            cb.setProperties(properties);
        }
        
        
        if (  basepath != null ) {
            String prop = cb.getProperties();
            prop = prop.replaceFirst("[#]?(\\s*?)exp.basepath = (.*?)\n", "# exp.basepath = "+basepath+"\n");
            cb.setProperties( prop );
        }
        
        coralBeanRepository.save(cb);

        model.put("coral", cb);

        return "edit";
    }

    @RequestMapping("/admin/copy")
    public String copy(@RequestParam(value = "name") String name,
            @RequestParam(value = "newname") String newname,
            Map<String, Object> model) {

        CoralBean old = coralBeanRepository.findOne(name);

        CoralBean cb = new CoralBean();
        cb.setName(newname);

        if (old != null) {
            cb.setProperties(old.getProperties());
        }
        coralBeanRepository.save(cb);

        model.put("coral", cb);

        return "edit";
    }

    @RequestMapping(value = "/images/bg")
    public void bg(HttpServletResponse httpResponse) throws IOException {
        Resource res = appContext.getResource("classpath:CORAL_bg.png");

        File f = res.getFile();
        FileSystemResource fr = new FileSystemResource(f);
        String type = servletContext.getMimeType(f.getAbsolutePath());
        httpResponse.setContentType(type);

        IOUtils.copy(fr.getInputStream(), httpResponse.getOutputStream());
    }

    @RequestMapping(value = "/assets/**")
    public void icon(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException {

        String path = ((String) httpRequest
                .getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE));

        Resource res = appContext.getResource("classpath:" + path);

        File f = res.getFile();
        FileSystemResource fr = new FileSystemResource(f);
        String type = servletContext.getMimeType(f.getAbsolutePath());
        httpResponse.setContentType(type);

        IOUtils.copy(fr.getInputStream(), httpResponse.getOutputStream());
    }

}
