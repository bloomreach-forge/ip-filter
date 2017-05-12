package org.onehippo.forge.ipfilter;

import java.util.Map;

import javax.jcr.Session;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.cms7.services.WhiteboardService;


@SingletonService
public interface IpFilterService {

    Session getSession();

    boolean configurationChanged();

    void setConfigurationChanged(boolean configurationChanged);
}
