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
package coral.reef.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import coral.reef.domain.CoralBean;
import coral.reef.domain.CoralBeanRepository;
import coral.reef.domain.CoralRun;
import coral.reef.domain.CoralRunRepository;
import coral.utils.CoralUtils;

@Service
public class ReefService {

    protected final Log logger = LogFactory.getLog(this.getClass());

    // service map
    // Map<String,IExpService> services = new HashMap<String, IExpService>();

    // service map
    Map<String, ReefHandler> handlers = new HashMap<String, ReefHandler>();

    // service map
    Map<String, Properties> serviceProperties = new HashMap<String, Properties>();

    // client to service map
    // Map<Integer,String> clientService = new HashMap<Integer,String>();

    @Autowired
    private Environment environment;

    @Autowired
    private CoralBeanRepository coralBeanRepository;

    @Autowired
    private CoralRunRepository runRepository;

    public ReefService() {

    }

    @PostConstruct
    public void startAllActive() {

        logger.info("restarting all active Corals");

        for (CoralBean cb : coralBeanRepository.findAll()) {
            if (cb.getStart() != null && cb.getStart().equals(true)) {
                logger.info("restarting " + cb.getName());
                startCoralService(cb.getName());
            }
        }
    }

    public boolean startCoralService(String name) {

        CoralBean coralBean = coralBeanRepository.findOne(name);

        Properties properties = fillDefaultProperties();
        InputStream is = new ByteArrayInputStream(coralBean.getProperties()
                .getBytes());
        try {
            properties.load(is);
            is.close();
        } catch (IOException e) {
            // never happens
            e.printStackTrace();
        }

        CoralUtils.hoststr = properties.getProperty("coral.head.coralhost",
                "exp://host/");

        String path = properties.getProperty("coral.web.path",
                coralBean.getName());
        String host = properties.getProperty("coral.web.hostname", "localhost");
        int port = Integer.parseInt(properties.getProperty("coral.web.port",
                "8080"));

        if (!properties.containsKey("coral.web.exphost")) {
            properties.setProperty("coral.web.exphost", "http://" + host + ":"
                    + port + "/" + path + "/");
        }

        ReefHandler handler = new ReefHandler(name, properties);

        this.handlers.put(coralBean.getName(), handler);
        // this.services.put( expname, serv);
        this.serviceProperties.put(coralBean.getName(), properties);

        coralBean.setStart(true);
        coralBeanRepository.save(coralBean);

        CoralRun run = new CoralRun();
        run.setProperties(coralBean.getProperties());
        run.setTimestamp(new Date());
        run.setCoralBean(coralBean);
        runRepository.save(run);

        return true;
    }

    public boolean stopCoralService(String name) {

        CoralBean coralBean = coralBeanRepository.findOne(name);

        // ReefHandler handler = handlers.get( coralBean.getName() );
        handlers.remove(coralBean.getName());
        serviceProperties.remove(coralBean.getName());

        coralBean.setStart(false);
        coralBeanRepository.save(coralBean);

        return true;
    }

    public ReefHandler handler(String exp) {

        return handlers.get(exp);
    }

    public String replaceHost(String exp, String msg) {
        String hoststr = serviceProperties.get(exp).getProperty(
                "coral.web.exphost");
        return msg.replaceAll(CoralUtils.hoststr, hoststr);
    }

    public Iterable<CoralBeanInfo> getCoralInfos() {

        ArrayList<CoralBeanInfo> result = new ArrayList<CoralBeanInfo>();

        for (CoralBean cb : coralBeanRepository.findAll(new Sort("name"))) {
            CoralBeanInfo info = new CoralBeanInfo();

            info.setCoral(cb);

            Properties properties = serviceProperties.get(cb.getName());

            if (cb.getStart() != null && cb.getStart() && properties != null) {

                /*
                 * String logfilepath = properties.getProperty("coral.log.path",
                 * properties.getProperty("exp.basepath", "./")); String
                 * logfilename = properties.getProperty("coral.log.name",
                 * "coral.log");
                 * 
                 * String content; try { Scanner scan = new Scanner(new
                 * File(logfilepath, logfilename)); scan.useDelimiter("\\Z");
                 * content = scan.next(); } catch (FileNotFoundException e) {
                 * content = e.getMessage(); } info.getInfo().put("log",
                 * content);
                 */

                /*
                 * provide links
                 */
                info.getInfo().put("hoststr",
                        properties.getProperty("coral.web.exphost"));
                info.getInfo().put("start",
                        properties.getProperty("coral.cmd.start"));
                info.getInfo().put("refresh",
                        properties.getProperty("coral.cmd.refresh"));
                info.getInfo().put("server",
                        properties.getProperty("coral.cmd.server"));

            }

            result.add(info);
        }

        return result;
    }

    public String getLog(String exp) {

        Properties p = serviceProperties.get(exp);

        p.getProperty("exp.basepath");

        return "";
    }

    private Properties fillDefaultProperties() {

        Properties defaults = new Properties();

        try {
            InputStream stream = new ClassPathResource(
                    "reef.default.properties").getInputStream();
            defaults.load(stream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return defaults;

    }

}
