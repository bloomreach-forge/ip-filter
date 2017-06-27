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
## Installation of the IP Filter plugin

These instructions assume a Hippo CMS project based on the Hippo website archetype, i.e. a Maven multi-module project 
with parent pom `org.onehippo.cms7:hippo-cms7-release` and consisting of at least three sub-modules: cms, site and bootstrap.

### Dependency management 
Add this property to the properties section of the root pom.xml:

    <hippo.forge.ipfilter.version>1.1.0</hippo.forge.ipfilter.version>

Select the correct version for your project. See the [release notes](release-notes.html) for more information on which 
version is applicable.

Add these dependencies to the `<dependencyManagement>` section of the root pom.xml:

```
  <dependency>
    <groupId>org.onehippo.forge.ipfilter</groupId>
    <artifactId>hippo-ipfilter-hst</artifactId>
    <version>${hippo.forge.ipfilter.version}</version>
  </dependency>
  <dependency>
    <groupId>org.onehippo.forge.ipfilter</groupId>
    <artifactId>hippo-ipfilter-cms</artifactId>
    <version>${hippo.forge.ipfilter.version}</version>
  </dependency>
```

### Installation in site application

Add this dependency to the `<dependencies>` section of the site/pom.xml. It contains the site IP filter.

```
  <dependency>
    <groupId>org.onehippo.forge.ipfilter</groupId>
    <artifactId>hippo-ipfilter-hst</artifactId>
  </dependency>
```

Add the following filter to the site's web.xml. It should be defined as **second** filter mapping in chain so just after 
CharacterEncodingFilter (in a standard Hippo project).

```  
  <filter>
    <filter-name>HippoIpFilter</filter-name>
    <filter-class>org.onehippo.forge.ipfilter.hst.IpFilter</filter-class>
  </filter>

  <!-- second mapping! -->
  <filter-mapping>
    <filter-name>HippoIpFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
```

### Installation in CMS application

Always add the following dependency to the `<dependencies>` section of the cms/pom.xml. It contains default 
configuration and the CMS filter.

```
  <dependency>
    <groupId>org.onehippo.forge.ipfilter</groupId>
    <artifactId>hippo-ipfilter-cms</artifactId>
  </dependency>
```

**Optionally, install the CMS filter.**
The CMS is password protected already but if the IP filtering functionality is also required for the CMS, add the 
following filter to the CMS's web.xml. It should be defined as **first** filter in chain, so just before 
ConcurrentLoginFilter (in a standard Hippo project).

```  
  <filter>
    <filter-name>HippoCmsIpFilter</filter-name>
    <filter-class>org.onehippo.forge.ipfilter.cms.CmsIpFilter</filter-class>
  </filter>

  <!-- first mapping! -->
  <filter-mapping>
    <filter-name>HippoCmsIpFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
```

Rebuild your project and distribute. In case you start with an existing repository don't forget to add *-Drepo.bootstrap=true*
to your startup options.

