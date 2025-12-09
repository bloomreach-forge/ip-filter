# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Bloomreach Forge IP Filter Plugin**, a security plugin for Bloomreach Experience Manager (brXM) that provides IP address-based access control for both CMS authoring and site delivery tiers.

**Project Identity:**
- Group ID: `org.bloomreach.forge.ipfilter`
- Version: 5.0.1-SNAPSHOT
- Parent: `hippo-cms7-project` 16.0.0
- License: Apache License 2.0
- Repository: https://github.com/bloomreach-forge/ip-filter
- Issue Tracking: https://issues.onehippo.com/browse/HIPFORGE

**Purpose:**
Control access to CMS and site based on the IP address of incoming requests, providing security layer for restricting access to authorized networks or addresses.

## Architecture Overview

The IP Filter plugin consists of three main modules that integrate with different tiers of the brXM platform:

### 1. Common Module (`/common/`)
The core filtering logic and utilities shared across CMS and HST implementations.

- **Purpose**: IP filtering logic, configuration loading, file monitoring
- **Key Technologies**: Spring Security, Guava, Java NIO file watching
- **Key Components**:
  - `BaseIpFilter` - Base class for IP filtering logic
  - `IpFilterUtils` - IP address parsing and validation utilities
  - `IpMatcher` - IP address matching with CIDR notation support
  - `IpFilterConfigLoader` - Configuration loading from JCR repository
  - `FileWatchService` - File system monitoring for configuration changes
  - `AuthObject` - Authorization object with IP/host/user information
- **Artifact**: `bloomreach-ipfilter-common`

### 2. HST Module (`/hst/`)
Integration with the Hippo Site Toolkit (HST) to filter requests to public-facing websites.

- **Purpose**: Website delivery tier access control
- **Key Technologies**: HST API, HST Core, Servlet filters
- **Key Components**:
  - HST-specific IP filter implementations
  - Integration with HST request pipeline
  - Configuration from HST content repository nodes
- **Dependencies**: common module, hst-api, hst-core
- **Artifact**: `bloomreach-ipfilter-hst`

### 3. CMS Module (`/cms/`)
Integration with the CMS tier to filter access to the authoring environment.

- **Purpose**: CMS authoring interface access control
- **Key Components**:
  - `IpFilterModule` - Repository daemon module for CMS integration
  - `CmsIpFilter` - CMS-specific filter implementation
  - `CmsConfigLoader` - Loads configuration from CMS repository
  - `IpFilterService` - Service for managing IP filter lifecycle
- **Dependencies**: common module, hippo-cms-api, hippo-repository-api
- **Artifact**: `bloomreach-ipfilter-cms`

### 4. Demo Module (`/demo/`)
Example brXM application demonstrating IP filter configuration and usage.

- **Purpose**: Reference implementation and testing
- **Components**: Complete brXM project with CMS, Site, and Repository data

## Build Commands

### Build All Modules
```bash
# Build entire plugin (all modules)
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Package for deployment
mvn package
```

### Build Specific Modules
```bash
# Common module only
cd common && mvn clean install

# HST module only
cd hst && mvn clean install

# CMS module only
cd cms && mvn clean install

# Demo application
cd demo && mvn clean install
```

### Testing
```bash
# Run all unit tests
mvn test

# Test specific module
cd common && mvn test
cd hst && mvn test
cd cms && mvn test

# Run specific test class
mvn test -Dtest=IpFilterUtilsTest

# Run specific test method
mvn test -Dtest=IpFilterUtilsTest#testMethodName
```

### Documentation Generation
```bash
# Generate site documentation (for GitHub Pages on master branch)
mvn clean site:site -Pgithub.pages

# Generate site documentation locally (output to /target)
mvn clean site:site
```

## Directory Structure

```
ip-filter/
├── common/                  # Common IP filtering logic
│   ├── src/main/java/
│   │   └── org/onehippo/forge/ipfilter/common/
│   │       ├── BaseIpFilter.java           # Base filter implementation
│   │       ├── IpFilterUtils.java          # IP utilities
│   │       ├── IpMatcher.java              # IP matching logic
│   │       ├── IpFilterConfigLoader.java   # Configuration loading
│   │       ├── IpFilterConstants.java      # Constants
│   │       ├── AuthObject.java             # Auth data holder
│   │       ├── IpHostPair.java            # IP/host pair
│   │       ├── Status.java                # Filter status enum
│   │       └── file/                      # File watching
│   │           ├── FileWatchService.java
│   │           ├── FileChangeObserver.java
│   │           └── WatchEventType.java
│   └── src/test/java/                     # Unit tests
│
├── hst/                     # HST (delivery tier) integration
│   └── src/main/java/
│       └── org/onehippo/forge/ipfilter/hst/
│
├── cms/                     # CMS tier integration
│   └── src/main/java/
│       └── org/onehippo/forge/ipfilter/
│           ├── repository/
│           │   └── IpFilterModule.java    # Repository daemon module
│           └── cms/
│               ├── CmsIpFilter.java       # CMS filter
│               ├── CmsConfigLoader.java   # Config loader
│               └── IpFilterService.java   # Filter service
│
└── demo/                    # Demo application
    ├── cms/                # Demo CMS webapp
    ├── site/               # Demo site webapp
    └── repository-data/    # Demo content and configuration
```

## Key Architecture Patterns

### IP Filtering
- **CIDR Notation Support**: Matches IP addresses against CIDR ranges (e.g., 192.168.1.0/24)
- **Whitelist/Blacklist**: Support for both allow and deny lists
- **Host-based Filtering**: Can filter by hostname in addition to IP address
- **X-Forwarded-For Support**: Handles proxied requests correctly

### Configuration Management
- **JCR Repository Storage**: Configuration stored in JCR repository nodes
- **File-based Configuration**: Optional file-based configuration with hot-reload
- **File Watching**: Automatic configuration reload on file changes using Java NIO WatchService
- **Observer Pattern**: FileChangeObserver for reacting to configuration changes

### Integration Patterns
- **Servlet Filter**: Implements standard Servlet Filter for request interception
- **Spring Security Integration**: Compatible with Spring Security filter chains
- **Repository Daemon Module**: CMS module runs as repository daemon for initialization
- **HST Valve**: Can be integrated as HST valve in request pipeline

## Technology Stack

### Java Backend
- **Java Version**: Java 8+
- **Build Tool**: Apache Maven 3.x
- **Security**: Spring Security Web
- **Utilities**: Apache Commons Lang, Google Guava
- **JCR**: Hippo Repository API, Hippo Services
- **HST Integration**: HST API, HST Core, HST Commons
- **CMS Integration**: Hippo CMS API

### Testing Frameworks
- **JUnit**: 4.13.1 (unit testing)
- **EasyMock**: 3.6 (mocking)
- **Spring Mock**: 2.0.8 (Spring integration testing)
- **Logging**: SLF4J with Log4j2 (for tests)

### Dependencies from brXM Platform
- hippo-repository-api: JCR repository access
- hippo-services: Platform services
- hippo-cms-api: CMS integration APIs
- hst-api: HST public APIs
- hst-core: HST core components
- hst-commons: HST common utilities

## Common Development Tasks

### Adding IP Filter to a Project

**1. Add Dependencies:**
```xml
<!-- For CMS filtering -->
<dependency>
  <groupId>org.bloomreach.forge.ipfilter</groupId>
  <artifactId>bloomreach-ipfilter-cms</artifactId>
  <version>5.0.1-SNAPSHOT</version>
</dependency>

<!-- For Site/HST filtering -->
<dependency>
  <groupId>org.bloomreach.forge.ipfilter</groupId>
  <artifactId>bloomreach-ipfilter-hst</artifactId>
  <version>5.0.1-SNAPSHOT</version>
</dependency>
```

**2. Configure in Repository:**
- Define IP filter configuration nodes in JCR repository
- Configure allowed/denied IP addresses or CIDR ranges
- Set up filter rules for different paths

**3. Register Filter:**
- For CMS: Register as repository daemon module
- For HST: Configure in HST container as valve or filter

### Extending Filter Functionality

**1. Custom IP Matching Logic:**
- Extend `BaseIpFilter` class
- Override matching methods in `IpMatcher`
- Add custom validation in `IpFilterUtils`

**2. Custom Configuration Loading:**
- Extend `IpFilterConfigLoader`
- Implement custom configuration sources
- Add file watching for external configuration files

**3. Custom Authorization:**
- Create custom `AuthObject` implementations
- Add additional authorization criteria beyond IP
- Integrate with external authorization systems

### Working with File Watching

**1. Monitor Configuration Files:**
```java
FileWatchService watchService = new FileWatchService(configDirectory);
watchService.addObserver(new FileChangeObserver() {
    @Override
    public void onFileChange(WatchEventType eventType, Path file) {
        // Reload configuration
    }
});
watchService.start();
```

**2. Handle Configuration Changes:**
- Implement `FileChangeObserver` interface
- React to CREATE, MODIFY, DELETE events
- Reload IP filter rules dynamically

## Running Tests

```bash
# Run all tests for all modules
mvn test

# Run tests for specific module
cd common && mvn test
cd hst && mvn test
cd cms && mvn test

# Run specific test class
mvn test -Dtest=IpFilterUtilsTest

# Run specific test method
mvn test -Dtest=IpFilterUtilsTest#testIpMatching

# Run with debug output
mvn test -Dtest=IpFilterUtilsTest -X

# Run tests with coverage
mvn clean test jacoco:report
```

## Test Utilities

### Common Module Tests
- **IpFilterUtilsTest**: Tests for IP parsing and validation
- **FileWatchServiceTest**: Tests for file monitoring
- **Mock JCR**: Uses Hippo repository mock objects for testing

## Development Workflow

### Git Branch Strategy
- **develop**: Main development branch (current: version 5.0.1-SNAPSHOT)
- **release branches**: release/X.X.X for release preparation
- **Feature branches**: feature/HIPFORGE-XXX for new features
- **PR base branch**: Use `develop` for pull requests

### Maven Conventions
- **Parent POM**: Inherits from `hippo-cms7-project`
- **Module Naming**:
  - Common: `bloomreach-ipfilter-common`
  - HST: `bloomreach-ipfilter-hst`
  - CMS: `bloomreach-ipfilter-cms`
- **Packaging**: JAR for library modules, WAR for demo webapps

### Release Process
- Version managed through Maven release plugin
- Documentation updated via `mvn site:site -Pgithub.pages`
- Deployed to Bloomreach Maven 2 Forge Repository
- GitHub releases created for each version

## Documentation & Resources

### Online Documentation
- **Plugin Documentation**: https://bloomreach-forge.github.io/ip-filter/
- **Javadoc**: https://javadoc.onehippo.org/
- **brXM Documentation**: https://documentation.bloomreach.com/

### Generate Documentation Locally
```bash
# Generate site documentation for GitHub Pages (master branch only)
mvn clean site:site -Pgithub.pages
# Output: /docs directory (served by GitHub Pages)

# Generate site documentation locally (any branch)
mvn clean site:site
# Output: /target/site directory
```

### Related Documentation
- Spring Security Filter Chain documentation
- Servlet Filter API documentation
- Java NIO WatchService API
- CIDR notation and IP subnet documentation

## Important Development Notes

### Best Practices
- **API First**: Common module provides shared APIs for both CMS and HST
- **Fail-Safe**: Filter should fail open or closed based on configuration
- **Performance**: Cache IP matching results when possible
- **Logging**: Log all access denials for security auditing
- **Testing**: Test with various IP formats (IPv4, IPv6, CIDR)
- **Configuration**: Validate configuration at startup

### Common Pitfalls
- Don't forget X-Forwarded-For headers when behind proxies
- Test with both IPv4 and IPv6 addresses
- Handle edge cases: localhost, private networks, invalid IPs
- Consider cluster environments with multiple nodes
- Watch for configuration reload race conditions
- Always read files before editing (use Read tool)

### Security Considerations
- **Fail-Safe Defaults**: Default to deny if configuration is missing
- **Proxy Awareness**: Handle X-Forwarded-For correctly to prevent spoofing
- **Configuration Security**: Protect filter configuration from unauthorized changes
- **Logging**: Log security events for audit trails
- **Testing**: Test filter bypass scenarios
- **IPv6**: Ensure IPv6 addresses are properly handled

### IP Address Formats Supported
- **IPv4**: 192.168.1.1
- **IPv4 CIDR**: 192.168.1.0/24
- **IPv6**: 2001:0db8:85a3:0000:0000:8a2e:0370:7334
- **IPv6 CIDR**: 2001:0db8::/32
- **Hostname**: example.com (resolved to IP)

## Integration with brXM Platform

### CMS Integration
1. **Module Registration**: Registered as repository daemon module
2. **Filter Chain**: Integrated into CMS servlet filter chain
3. **Configuration**: Stored in CMS repository nodes
4. **Startup**: Initialized when repository starts

### HST Integration
1. **Valve Integration**: Can be added to HST request processing pipeline
2. **Filter Integration**: Can be configured as servlet filter for site webapp
3. **Configuration**: Stored in HST configuration nodes
4. **Multi-Site**: Supports different rules per HST site

### Repository Configuration
Configuration nodes in JCR repository typically follow this structure:
```
/hippo:configuration/hippo:modules/ipfilter/
  - enabled: boolean
  - allowedIPs: String[] (CIDR notation supported)
  - deniedIPs: String[] (CIDR notation supported)
  - defaultAllow: boolean
```

## Quick Start for Common Scenarios

### Scenario 1: Add New IP Matching Logic
```bash
cd common
# Read BaseIpFilter and IpMatcher classes
# Implement new matching algorithm
# Add tests in IpFilterUtilsTest
mvn test -Dtest=IpFilterUtilsTest
mvn clean install
```

### Scenario 2: Update CMS Filter
```bash
cd cms
# Modify CmsIpFilter or CmsConfigLoader
# Test with demo CMS application
mvn clean install
cd ../demo && mvn clean install
# Deploy and test
```

### Scenario 3: Add HST Filter Feature
```bash
cd hst
# Implement new HST-specific feature
# Add configuration support
# Test with demo site
mvn test
mvn clean install
```

### Scenario 4: Update Configuration Loading
```bash
cd common
# Modify IpFilterConfigLoader
# Update file watching in FileWatchService
# Test configuration reload
mvn test -Dtest=FileWatchServiceTest
mvn clean install
```

## Performance Optimization

### IP Matching Performance
- **Caching**: Cache resolved IP addresses and match results
- **Lazy Evaluation**: Only resolve hostnames when necessary
- **Efficient Data Structures**: Use range trees for CIDR matching
- **Early Exit**: Check most specific rules first

### Configuration Loading
- **Lazy Loading**: Load configuration on-demand
- **Caching**: Cache repository configuration in memory
- **File Watching**: Use NIO WatchService for efficient file monitoring
- **Batch Updates**: Group configuration changes together

## Deployment Considerations

### Clustered Environments
- Configuration changes should propagate across cluster nodes
- Use repository events for cluster-wide updates
- Consider eventual consistency for configuration changes
- Test failover scenarios

### Proxy Configurations
- Configure X-Forwarded-For header handling
- Determine trusted proxy IP addresses
- Handle multiple proxy hops correctly
- Validate proxy headers to prevent spoofing

### High-Traffic Sites
- Minimize configuration reload frequency
- Use efficient IP matching algorithms
- Consider dedicated filter instances per site
- Monitor filter performance impact

## Version Information

- **Current Version**: 5.0.1-SNAPSHOT
- **brXM Compatibility**: 16.0.0
- **Java Version**: Java 8+
- **Build Tool**: Apache Maven 3.x

## License

Apache License 2.0 - Open Source

## Getting Help

- **Issues**: Report issues at https://issues.onehippo.com/browse/HIPFORGE
- **Documentation**: https://bloomreach-forge.github.io/ip-filter/
- **Source Code**: https://github.com/bloomreach-forge/ip-filter
- **Bloomreach Community**: https://community.bloomreach.com/
