<!--
  Copyright 2017-2022 Bloomreach

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->

## Version Compatibility

| Bloomreach Experience Manager | IP Filter |
|-------------------------------|-----------| 
| 15.x                          | 4.x       |
| 14.x                          | 3.x       |
| 13.x                          | 3.x       |
| 12.x                          | 2.x       |
| 11.x                          | 1.x       |

## Release Notes

### 4.0.0
<p class="smallinfo">Release date: 2 May 2022</p>

+ [HIPFORGE-421](https://issues.onehippo.com/browse/HIPFORGE-421)<br/>
  - Upgrade to Bloomreach Experience Manager 15, i.e. build the plugin with Java 11, redo the demo project.

### 3.1.0
<p class="smallinfo">Release date: 19 August 2020</p>

+ [HIPFORGE-363](https://issues.onehippo.com/browse/HIPFORGE-363)<br/>
  - added 'forwarded-host-header' setting on module level, see [usage page](usage.html).
  - added 'cache-enabled' setting per configuration set, see [usage page](usage.html).
  - more DEBUG logging in various locations, also `org.onehippo.forge.ipfilter.common.BaseIpFilter` will print out all
    request headers and cookies on TRACE level.


### 3.0.1
<p class="smallinfo">Release date: 30 January 2020</p>

+ [HIPFORGE-334](https://issues.onehippo.com/browse/HIPFORGE-334)<br/> 
  the cache key should include password too.

### 3.0.0
<p class="smallinfo">Release date: 7 February 2019</p>

+ [HIPFORGE-242](https://issues.onehippo.com/browse/HIPFORGE-242)<br/> 
  Make plugin 13.x version compatible.
+ Change the artifacts' groupIds to <code>org.bloomreach.forge.ipfilter</code> and have the artifactIds starting with 
<code>bloomreach-ipfilter-</code> (so renaming hippo to bloomreach).<br/>
This requires to revisit the [installation page](install.html) when upgrading. 

### 2.3.0  

<p class="smallinfo">Release date: 25 July 2018</p>

+ [HIPFORGE-187](https://issues.onehippo.com/browse/HIPFORGE-187)<br/> 
  When using `allow-cms-users` setting, enabling site visitor authentication by login, see [usage page](usage.html),
  always login to the embedded repository first, then try a configured location like RMI as fallback.

### 2.2.0  

<p class="smallinfo">Release date: 8 June 2018</p>

+ [HIPFORGE-160](https://issues.onehippo.com/browse/HIPFORGE-160)<br/> 
  Introduce additional configuration by a external properties file, see section "External properties file for default 
  set up" at the [Usage page](usage.html).
+ [HIPFORGE-158](https://issues.onehippo.com/browse/HIPFORGE-158)<br/> 
  Set Spring dependency scopes to provided as CMS includes Spring on Hippo 12.

### 2.1.0  

<p class="smallinfo">Release date: 11 April 2018</p>

+ [HIPFORGE-134](https://issues.onehippo.com/browse/HIPFORGE-134)<br/> 
  Make AuthObject immutable for robustness, better thread safety.

### 2.0.0  

<p class="smallinfo">Release date: 28 November 2017</p>

+ Upgrade to Hippo 12: rewrite the bootstrapping as yaml instead of XML. Also some minor, internal, non-functional refactoring.

### 1.2.0  

<p class="smallinfo">Release date: 19 June 2018</p>

+ [HIPFORGE-182](https://issues.onehippo.com/browse/HIPFORGE-182)<br/> 
  Introduce additional configuration by a external properties file, see section "External properties file for default 
  set up" at the [Usage page](usage.html).

### 1.1.1  

<p class="smallinfo">Release date: 29 January 2018</p>

+ Make AuthObject collections thread safe, avoiding unexpected NPE under load at `BaseIpFilter.isIgnored(BaseIpFilter.java:232)`

### 1.1.0  

<p class="smallinfo">Release date: 27 June 2017</p>

+ Apply Bloomreach Forge best practices and publish it on the Forge, under different Maven coordinates of the artifacts.
+ Fix reloading issues in the site's IpFilter.   
+ Add a filter for the CMS webapp: CmsIpFilter.
+ Available on Hippo 11.

### 1.0.0 - 1.0.4 
+ Older releases from sandbox, non-open source.
