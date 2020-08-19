<!--
  Copyright 2017-2020 Bloomreach

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

These instructions assume a Bloomreach Experience project based on the website archetype, i.e. a Maven multi-module project 
with parent pom `org.onehippo.cms7:hippo-cms7-release` and consisting of at least three sub-modules: cms, site and repository-data.

### Forge Repository
In the main pom.xml of the project, in the repositories section, add this repository if it is not configured there yet. 

```
<repository>
  <id>hippo-forge</id>
  <name>Hippo Forge Maven 2 repository.</name>
  <url>https://maven.onehippo.com/maven2-forge/</url>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
  <releases>
    <updatePolicy>never</updatePolicy>
  </releases>
  <layout>default</layout>
</repository>
```

### Dependency management 
Add this property to the properties section of the root pom.xml:

    <bloomreach.forge.ipfilter.version>version.number</bloomreach.forge.ipfilter.version>

Select the correct version for your project. See the [release notes](release-notes.html) for more information on which 
version is applicable.

Add these dependencies to the `<dependencyManagement>` section of the root pom.xml:

```
  <dependency>
    <groupId>org.bloomreach.forge.ipfilter</groupId>
    <artifactId>bloomreach-ipfilter-hst</artifactId>
    <version>${bloomreach.forge.ipfilter.version}</version>
  </dependency>
  <dependency>
    <groupId>org.bloomreach.forge.ipfilter</groupId>
    <artifactId>bloomreach-ipfilter-cms</artifactId>
    <version>${bloomreach.forge.ipfilter.version}</version>
  </dependency>
```

<div class="alert alert-info">
    Note: before version 3.0, the artifacts' groupId was <code>org.onehippo.forge.ipfilter</code> and
    the artifactIds started with <code>hippo-ipfilter</code>
</div>

### Installation in site application

Add this dependency to the `<dependencies>` section of the site/pom.xml. It contains the site IP filter.

```
  <dependency>
    <groupId>org.bloomreach.forge.ipfilter</groupId>
    <artifactId>bloomreach-ipfilter-hst</artifactId>
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
    <groupId>org.bloomreach.forge.ipfilter</groupId>
    <artifactId>bloomreach-ipfilter-cms</artifactId>
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

### Bootstrapping
When running a version 12 or later, make sure your project bootstraps after `bloomreach-forge` group. 
Typically, add it in file `repository-data/application/src/main/resources/hcm-module.yaml`:
```  
  group:
    name: <your-group-name>
    after: [hippo-cms, bloomreach-forge]
```  

Rebuild your project and distribute. In case you start with an existing repository don't forget to add *-Drepo.bootstrap=true*
to your startup options.

