package org.onehippo.forge.ipfilter;

import java.util.Map;

public class IpFilterServiceImpl implements IpFilterService {

    private Map<String, AuthObject> data;

    @Override
    public void setData(final Map<String, AuthObject> data) {
       this.data = data;
    }

    @Override
    public Map<String, AuthObject> getData() {
        return data;
    }
}
