Configure module here:

http://localhost:8080/cms/console/?1&path=/hippo:configuration/hippo:modules/ipfilter-module/hippo:moduleconfig
```
<?xml version="1.0" encoding="UTF-8"?>
<sv:node sv:name="hippo:moduleconfig" xmlns:sv="http://www.jcp.org/jcr/sv/1.0">
  <sv:property sv:name="jcr:primaryType" sv:type="Name">
    <sv:value>nt:unstructured</sv:value>
  </sv:property>
  <sv:property sv:multiple="true" sv:name="allowed-ip-ranges" sv:type="String">
    <sv:value>127.0.0.1</sv:value>
    <sv:value>81.21.138.121</sv:value>
    <sv:value>2001:4cb8:29d:1::/64</sv:value>
    <sv:value>80.100.160.251</sv:value>
  </sv:property>
</sv:node>

```
Static config: (overridden by dynamic one above)
```
  <filter>
    <filter-name>IpFilter</filter-name>
    <filter-class>org.onehippo.forge.ipfilter.IpFilter</filter-class>
    <init-param>
      <param-name>allowed-ip-ranges</param-name>
      <!-- allow localhosts and Hippo Office IPs -->
      <param-value>127.0.0.1,0:0:0:0:0:0:0:1,81.21.138.121,2001:4cb8:29d:1::/64,80.100.160.250</param-value>
    </init-param>
  </filter>


```
Make filter mapping
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
