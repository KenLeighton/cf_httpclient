# cf_httpclient - Drop-in Replacement for CFX_HTTP5

> **⚠️ NOTICE:** This project is not under active development. Use at your own risk. No support, updates, or bug fixes will be provided. See LICENSE file for complete disclaimer.

**Version:** 1.10.0  
**Date:** 2025-11-03  
**License:** Apache License 2.0  

## What is cf_httpclient?

cf_httpclient is a professional-grade HTTP client for Adobe ColdFusion and Lucee, designed as a drop-in replacement for the legacy CFX_HTTP5 C++ extension. Built on Apache HttpClient 4.5.14, it provides:

- ✅ **100% CFX_HTTP5 compatibility** - All 56 parameters supported
- ✅ **True session management** - Automatic cookie handling
- ✅ **Connection pooling** - Enterprise-grade performance
- ✅ **Asynchronous execution** - Up to 64 simultaneous requests
- ✅ **SSL/TLS 1.2** - Modern security standards
- ✅ **No native dependencies** - Pure Java implementation

## Quick Start

### 1. Extract Files

Extract this archive to a temporary directory. You'll find:

```
cf_httpclient/
  /lib/                          - JAR files (Apache HttpClient)
  /customtags/                   - Custom tag (httpclient.cfm)
  /docs/                         - Complete documentation
  README.md                      - This file
```

### 2. Copy to Your Application

Copy the folders to your application root:

```
your-application/
  /lib/                          - Copy JARs here
  /customtags/                   - Copy httpclient.cfm here
  Application.cfc                - Configure here
```

### 3. Configure Application.cfc

Add this to your `Application.cfc`:

```cfml
component {
    this.name = "MyApplication";
    
    // Configure custom tag path
    this.customtagpaths = expandPath("./customtags");
    
    // Load HttpClient JAR files
    this.javaSettings = {
        loadPaths = [
            expandPath("./lib/httpclient-4.5.14.jar"),
            expandPath("./lib/httpcore-4.4.16.jar"),
            expandPath("./lib/commons-logging-1.2.jar"),
            expandPath("./lib/commons-codec-1.11.jar")
        ],
        reloadOnChange = false
    };
}
```

### 4. Use cf_httpclient

```cfml
<!--- Basic GET request --->
<cf_httpclient url="https://api.example.com/data" method="get" out="result">

<cfoutput>
    <p>Status: #status#</p>
    <p>Response: #result#</p>
</cfoutput>
```

## Documentation

See `docs/Programmer's Reference - cf_httpclient.md` for complete documentation including:

- Installation instructions for Application.cfc and Application.cfm
- Complete parameter reference (all 56 parameters)
- Usage examples (GET, POST, file upload/download, sessions, async)
- SSL/TLS configuration
- Proxy configuration
- Authentication (Basic, Digest, NTLM)
- Troubleshooting guide
- Migration from CFX_HTTP5

## System Requirements

**Adobe ColdFusion:**
- ColdFusion 11 or later
- Java 8 or later

**Lucee:**
- Lucee 5.0 or later
- Java 8 or later

**Operating Systems:**
- Windows Server 2012 R2+
- Linux (Ubuntu 18.04+, CentOS 7+, RHEL 7+)
- macOS 10.14+

## CommandBox Compatible

cf_httpclient works seamlessly with CommandBox:

```bash
box server start
```

No server.json configuration required - JARs and custom tags are loaded automatically via Application.cfc.

## What's Included

### JAR Files (Apache HttpClient 4.5.14)

- `httpclient-4.5.14.jar` (1.0 MB) - HTTP client implementation
- `httpcore-4.4.16.jar` (328 KB) - HTTP protocol support
- `commons-codec-1.11.jar` (335 KB) - Encoding/decoding utilities
- `commons-logging-1.2.jar` (61 KB) - Logging facade

**Total Size:** ~2 MB

### Custom Tag

- `httpclient.cfm` - The cf_httpclient custom tag implementation

### Documentation

- `Programmer's Reference - cf_httpclient.md` - Complete programmer's reference

## License and Disclaimer

**License:** Apache License 2.0 (see LICENSE file)

**DISCLAIMER:** This software is provided "AS IS" without warranty of any kind. Use at your own risk. This project is not under active development. Users assume all risks associated with the use of this software.

### Apache HttpClient

This implementation uses Apache HttpClient 4.5.14:
- **License:** Apache License 2.0
- **Website:** https://hc.apache.org/
- **Copyright:** The Apache Software Foundation

See `lib/LICENSE.txt` and `lib/NOTICE.txt` for Apache HttpClient license information.

## Support

For issues, questions, or contributions, refer to the complete documentation in `docs/Programmer's Reference - cf_httpclient.md`.

## Version History

**1.10.0** (2025-11-03)
- 100% CFX_HTTP5 parity achieved (all 56 parameters)
- Complete rewrite using Apache HttpClient 4.5.14
- True session management with HttpClient instances
- Asynchronous execution support
- SSL/TLS 1.2 support
- Windows certificate store integration
- Professional connection pooling
- Enhanced proxy support with authentication

---

**Ready to deploy.** Copy the files, configure Application.cfc, and start making HTTP requests.
