package org.onehippo.forge.ipfilter;

import java.util.Map;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.cms7.services.WhiteboardService;


//@SingletonService
@WhiteboardService
public interface IpFilterService {

    /**
     * Update ip filter data when changed or initialized
     *
     * @param data filter data or null in case no data is configured
     */
    void setData(Map<String, AuthObject> data);

    /**
     * Get ip filter data
     *
     * @return filter data or null if data not initialized yet (or missing)
     */
    Map<String, AuthObject> getData();
}
