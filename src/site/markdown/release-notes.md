<!--
  Copyright 2018 Hippo B.V. (http://www.onehippo.com)

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

## Version Compatibility

| Hippo CMS | IP Filter |
| --------- |-----------| 
| 12.x      | 2.x       |
| 11.x      | 1.x       |

## Release Notes

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

+ Apply Hippo Forge best practices and publish it on the Forge, under different Maven coordinates of the artifacts.
+ Fix reloading issues in the site's IpFilter.   
+ Add a filter for the CMS webapp: CmsIpFilter.
+ Available on Hippo 11.

### 1.0.0 - 1.0.4 
+ Older releases from sandbox, non-open source.
