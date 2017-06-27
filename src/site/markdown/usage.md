<!--
  Copyright 2017 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->
## Using the IP Filter plugin

### Configuration

During installation of IP Filter plugin, configuration for `localhost` is bootstrapped into 
`/hippo:configuration/hippo:modules/ipfilter/hippo:moduleconfig/localhost`. To make the configuration work on other 
environments, please make a copy of this node with a descriptive name like 'staging' or 'production'. Alternatively, 
if you want one configuration set, rename it to 'all-envs'.

#### Properties of a configuration set 

|Property               | Type            | Default         | Description 
|------------------------|-----------------|-----------------|------------- 
|`enabled`              | boolean         | true            | Enable this configuration or not.
|`hostnames`            | multiple string |                 | **Mandatory** list of hostnames, matching a browser request to this configuration set.   
|`allowed-ip-ranges`    | multiple string |                 | Whitelist of ip address **ranges** e.g. **2001:4cb8:29d:1::/64**. Controls access the both CMS as the site.  
|`allow-cms-users`      | boolean         | true            | To access the site, allow login with CMS credentials, with a basic authentication popup.
|`match-all`            | boolean         | false           | To access the site, both IP address must be whitelisted and login with CMS credentials must be successful. 
|`ignored-paths`        | multiple string |                 | List of paths that are ignored by the filters, e.g. **/ping/.*** 
|`forwarded-for-header` | string          | X-Forwarded-For | Name of the request header that is used for forwarding.

**NOTES** 
- Both `hostnames` and `ignored-paths` must be regular expression escaped like  **\*.onehippo\\.org** or **127\\.0\\.0\\.1**
- Either `allow-cms-users` or `allowed-ip-ranges` must be enabled for valid configuration.
 
### Multiple optional subconfigurations for special headers  

Below a configuration set node, there may be any number of subnodes of type `hipposys:moduleconfig`, named freely 
but descriptively, for example "ignore-fastly". Such node has the following properties:
 
|Property                  | Type            | Description 
|---------------------------|-----------------|------------ 
|`ignored-header`        | string          | Name of header to ignore, for example 'X-fastly'.
|`ignored-header-values` | multiple string | Values of **ignored-header** that must be matched for the request to be ignored by the filter.


### System properties to disable the filters

For recovery purposes, should administrators have locked everybody out of the CMS/console by misconfiguration, the 
possibility exists to disable the filters by setting system properties.
 
|System Property               | Description
|-------------------------------|------------
|`hippo.cms-ipfilter.disabled` | Disables the CMS's `org.onehippo.forge.ipfilter.cms.CmsIpFilter` 
|`hippo.ipfilter.disabled`     | Disables the site's `org.onehippo.forge.ipfilter.hst.IpFilter`

