Configuration options:

* **enabled** : config is enabled or not ( boolean, default **true**)
* **allow-cms-users** : allow login with CMS credentials, basic authentication popup( boolean, default **false**)
* **match-all** : both: user IP address must be whitelisted and login with CMS credentials must be successful to access the site ( boolean, default **false**)
* **hostnames** : list of hostnames. NOTE: must be regular expression escaped like  **\*.onehippo\.org** (multivalue string, **mandatory**)
* **allowed-ip-ranges** : whitelist of ip address **ranges** e.g. **2001:4cb8:29d:1::/64** (multivalue string, **not** mandatory in case **allow-cms-users** is true)
* **ignored-paths** : list of (**regular expression escaped!**) paths which are ignored by filter e.g. **/ping/.*** (multivalue string, optional)
* **forwarded-for-header** : name of (**X-Forwarded-For**) header (string, optional, default: X-Forwarded-For)
                                

Either allow-cms-users or allowed-ip-ranges must be enabled for valid configuration.


Module can be configured here:

http://localhost:8080/cms/console/?1&path=/hippo:configuration/hippo:modules/ipfilter-module/hippo:moduleconfig
```
  <sv:node sv:name="hippo:moduleconfig">
    <sv:property sv:name="jcr:primaryType" sv:type="Name">
      <sv:value>nt:unstructured</sv:value>
    </sv:property>
    <sv:node sv:name="onehippo" sv:type="Name">
      <sv:property sv:name="jcr:primaryType" sv:type="Name">
        <sv:value>nt:unstructured</sv:value>
      </sv:property>
      <sv:property sv:name="enabled" sv:type="Boolean">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="match-all" sv:type="Boolean">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="allow-cms-users" sv:type="Boolean">
        <sv:value>true</sv:value>
      </sv:property>
      <sv:property sv:name="ignored-paths" sv:type="String" sv:multiple="true">
        <sv:value>/ping/.*</sv:value>
        <sv:value>.*\.css</sv:value>
        <sv:value>.*\.js</sv:value>
        <sv:value>.*\.ico</sv:value>
      </sv:property>
      <sv:property sv:name="allowed-ip-ranges" sv:type="String" sv:multiple="true">
        <sv:value>127.0.0.1</sv:value>
        <sv:value>81.21.138.121</sv:value>
        <sv:value>2001:4cb8:29d:1::/64</sv:value>
        <sv:value>80.100.160.250</sv:value>
      </sv:property>
      <sv:property sv:name="hostnames" sv:type="String" sv:multiple="true">
        <sv:value>localhost</sv:value>
        <sv:value>127\.0\.0\.1</sv:value>
        <sv:value>.*onehippo\.com</sv:value>
        <sv:value>.*onehippo\.org</sv:value>
      </sv:property>
    </sv:node>
  </sv:node>
```

Filter configuration (**web.xml**)


```
  <filter>
    <filter-name>IpFilter</filter-name>
    <filter-class>org.onehippo.forge.ipfilter.IpFilter</filter-class>
    <init-param>
      <param-name>repository-address</param-name>
      <param-value>vm://</param-value>
    </init-param>
    <init-param>
       <param-name>realm</param-name>
       <param-value>somename</param-value>
    </init-param>
  </filter>


```

Make filter mapping
NOTE:  filter mapping should be defined just after **CharacterEncodingFilter** so as a **second** filter in chain
```
  <filter-mapping>
    <filter-name>IpFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>

```
Dependencies CMS

```
     <dependency>
       <groupId>org.onehippo.forge</groupId>
       <artifactId>hippo-cms-ip-filter-common</artifactId>
       <version>1.0.0-SNAPSHOT</version>
     </dependency>
     <dependency>
       <groupId>org.onehippo.forge</groupId>
       <artifactId>hippo-cms-ip-filter-cms</artifactId>
       <version>1.0.0-SNAPSHOT</version>
     </dependency>
```

Dependencies HST

```
    <dependency>
      <groupId>org.onehippo.forge</groupId>
      <artifactId>hippo-cms-ip-filter-common</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>org.onehippo.forge</groupId>
      <artifactId>hippo-cms-ip-filter-hst</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>
```
