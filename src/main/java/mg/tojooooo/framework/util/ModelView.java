package mg.tojooooo.framework.util;

import java.util.Map;
import java.util.HashMap;

public class ModelView {

    private String view;
    private Map<String, Object> dataMap;

    public ModelView() {}
    public ModelView(String view) { setView(view); setDataMap(new HashMap<String, Object>()); }
    public ModelView(Map<String, Object> dataMap) { setDataMap(dataMap); setDataMap(new HashMap<String, Object>()); }
    public ModelView(String view, Map<String, Object> dataMap) { setView(view); setDataMap(dataMap); }

    public String getView() { return view; }
    public void setView(String view) { this.view = view; }
    public Map<String, Object> getDataMap() { return dataMap;}
    public void setDataMap(Map<String, Object> dataMap) { this.dataMap = dataMap;}

    public void addData(String key, Object value) {
        dataMap.put(key, value);
    }

}
