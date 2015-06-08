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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import coral.data.DataService;
import coral.model.ExpData;
import coral.service.ExpHandler;
import coral.service.ExpServiceImpl;
import coral.service.IExpService;
import coral.utils.CoralUtils;

public class ReefHandler implements ExpHandler {

    // transient holder for messages
    private Map<Integer, String> messages = new HashMap<Integer, String>();

    private DataService dataService;

    private IExpService service;

    private String basepath;

    private Map<String, File> resMap = new HashMap<String, File>();

    private String startMarker;
    private String refreshMarker;
    private String serverMarker;
    private String processMarker;

    private static Integer clientcount = 0;

    public ReefHandler(String name, Properties properties) {
        this.basepath = properties.getProperty("exp.basepath", "./");

        String dbname = properties.getProperty("coral.db.name", "~/" + name
                + "_db");
        String dbmode = properties.getProperty("coral.db.mode", "file");
        boolean resetdb = (properties.getProperty("coral.db.reset", "false")
                .equals("true"));

        this.dataService = new ReefCoralDAO(dbname, resetdb, dbmode);

        String stagesfile = "stages.csv";
        if (properties.containsKey("exp.stagesfile")) {
            stagesfile = properties.getProperty("exp.stagesfile");
        }
        ExpServiceImpl serv = new ExpServiceImpl(this, properties, dataService);
        serv.init(basepath, stagesfile,
                properties.getProperty("coral.exp.variant"));

        // this.viewname = properties.getProperty("exp.viewname", "_exp.html");

        // this.server = new ExpServer(0,
        // properties.getProperty("exp.servertype",
        // "none"), this, dataService);

        serv.debug = (properties.getProperty("coral.debug", "false")
                .equals("true"));
        // logger.debug("debug status is "
        // + properties.getProperty("exp.debug", "false") + "  --  "
        // + serv.debug);

        // useScreenwriter = (properties.getProperty("coral.head.screenwriter",
        // "false").equals("true"));

        this.startMarker = properties.getProperty("coral.cmd.start",
                CoralUtils.START_KEY);
        properties.setProperty("coral.cmd.start", startMarker);

        this.refreshMarker = properties.getProperty("coral.cmd.refresh",
                CoralUtils.REFRESH_KEY);
        properties.setProperty("coral.cmd.refresh", refreshMarker);

        this.serverMarker = properties.getProperty("coral.cmd.server",
                CoralUtils.SERVER_KEY);
        properties.setProperty("coral.cmd.server", serverMarker);

        this.processMarker = properties.getProperty("coral.cmd.process", "exp"); // FIXME

        this.service = serv;
    }

    public Integer addClient(Integer clientId) {
        if (clientId == null) {
            clientcount = dataService.getNewId(clientcount + 1);
            clientId = clientcount;
        }
        service.addClient(clientId);

        return clientId;
    }

    /**
     * key method to process request and deliver results
     */
    public String process(Integer id, String path, String query) {

        messages.put(id, null);

        service.process(id, query);

        String result = null;
        while ((result = messages.get(id)) == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * Server evaluation method
     * 
     * @param exp
     * @param counter
     * @param cmd
     * @param map
     * @return
     */
    public String server(String cmd, Map<String, String[]> map) {

        System.out.println(cmd);

        ExpServiceImpl service = ((ExpServiceImpl) this.service);
        Map<Integer, ExpData> data = service.getAllData();

        Map<String, Object> adds = new HashMap<String, Object>();
        adds.put("_agentdata", data);
        adds.put("_stages", service.getStages());
        adds.put("_query", map);

        // FIXME check what should be done here
        @SuppressWarnings("unchecked")
        String content = service.getUtil().evalVM(cmd, data, null, service,
                adds);

        return content;
    }

    /*
     * EXPHANDLER METHODS
     * 
     * (non-Javadoc)
     * 
     * @see coral.service.ExpHandler#broadcast(java.lang.Integer,
     * java.lang.String)
     */

    @Override
    public void broadcast(Integer id, String msg) {

        // todo properly handle replacement here
        messages.put(id, msg);
    }

    @Override
    public void wait(Integer id, String mode, int stage, int loop) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IExpService getService() {
        return service;
    }

    @Override
    public Set<Integer> getServiceIds() {
        throw new UnsupportedOperationException();
        // return null;
    }

    @Override
    public ArrayList<Map<String, Object>> getClientInfoMapList() {
        throw new UnsupportedOperationException();
        // return null;
    }

    @Override
    public void sendRes(Integer id, String... ress) {
        // throw new UnsupportedOperationException();
        for (String res : ress) {
            File f = new File(this.basepath, res);
            resMap.put(f.getName(), f);
        }
    }

    public Map<String, File> getResMap() {
        return resMap;
    }

    public String startMarker() {
        return startMarker;
    }

    public String refreshMarker() {
        return refreshMarker;
    }

    public String serverMarker() {
        return serverMarker;
    }

    public String processMarker() {
        return processMarker;
    }

}
