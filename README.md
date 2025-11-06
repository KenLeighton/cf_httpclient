# cf_httpclient

**Drop-in replacement for CFX_HTTP5 with 100% feature parity**

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![ColdFusion](https://img.shields.io/badge/ColdFusion-11%2B-orange.svg)]()
[![Lucee](https://img.shields.io/badge/Lucee-5%2B-blue.svg)]()

Pure Java implementation providing complete CFX_HTTP5 compatibility for Adobe ColdFusion and Lucee servers.

---

## Overview

cf_httpclient is a **drop-in replacement for CFX_HTTP5** built on Apache HttpClient 4.5.14. It provides identical functionality to the legacy C++ extension with **all 56 parameters** supported, eliminating platform-specific DLL dependencies while maintaining full backward compatibility.

### Key Features

✅ **100% CFX_HTTP5 Compatible** - All 56 parameters with identical behavior  
✅ **Pure Java** - No native DLL dependencies  
✅ **Session Management** - Automatic cookie handling with persistent sessions  
✅ **Asynchronous Requests** - Background execution with FNC="GET", "WAIT", "CANCEL"  
✅ **SSL/TLS Support** - Full SSL parameter integration  
✅ **Proxy Configuration** - PROXYSERVER, PROXYPORT, PROXYUSER, PROXYPASS  
✅ **Authentication** - Basic, Digest, NTLM schemes  
✅ **File Operations** - Upload/download via FILE parameter  
✅ **DNS Lookups** - FNC="DNS" for hostname resolution  
✅ **Cross-Platform** - Windows, Linux, macOS  

---

## Quick Start

### Installation

**1. Create directory structure in your application:**

```
/my-application/
  /lib/
    httpclient-4.5.14.jar
    httpcore-4.4.16.jar
    commons-logging-1.2.jar
    commons-codec-1.11.jar
  /customtags/
    httpclient.cfm
```

**2. Configure Application.cfc:**

```cfml
component {
    this.name = "MyApplication";
    
    this.customtagpaths = expandPath("./customtags");
    
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

**3. Use in your code:**

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="result">

<cfif status EQ "OK">
    <cfoutput>#result#</cfoutput>
<cfelse>
    <cfoutput>Error: #msg#</cfoutput>
</cfif>
```

---

## System Requirements

### Minimum Requirements

**Adobe ColdFusion:**
- ColdFusion 11 or later
- Java 8 or later

**Lucee:**
- Lucee 5.0 or later
- Java 8 or later

### ⚠️ Adobe ColdFusion 11 Users

**ColdFusion 11 ships with incompatible Apache HttpClient JARs (version 4.2.5).**

cf_httpclient requires Apache HttpClient 4.5.14. **A one-time server-level JAR upgrade is required:**

1. Stop ColdFusion 11 service
2. Navigate to `{cf-install-dir}\cfusion\lib\`
3. Backup old JARs:
   - Rename `httpclient-4.2.5.jar` to `httpclient-4.2.5.jar.BAK`
   - Rename `httpcore-4.2.4.jar` to `httpcore-4.2.4.jar.BAK`
4. Copy new JARs:
   - Copy `httpclient-4.5.14.jar` to `{cf-install-dir}\cfusion\lib\`
   - Copy `httpcore-4.4.16.jar` to `{cf-install-dir}\cfusion\lib\`
5. Start ColdFusion 11 service

**ColdFusion 2016+, Lucee 5+, CommandBox:** No server-level upgrade required.

---

## Usage Examples

### Simple GET Request

```cfml
<cf_httpclient 
    url="https://api.example.com/users"
    method="get"
    out="response">

<cfif status EQ "OK">
    <cfset data = deserializeJSON(response)>
    <cfoutput>#data.users.len()# users found</cfoutput>
</cfif>
```

### POST Form Data

```cfml
<cf_httpclient 
    url="https://api.example.com/login"
    method="post"
    body="username=admin&password=secret"
    headers="Content-Type: application/x-www-form-urlencoded"
    out="response">
```

### POST JSON

```cfml
<cfset jsonData = serializeJSON({
    name: "John Doe",
    email: "john@example.com"
})>

<cf_httpclient 
    url="https://api.example.com/users"
    method="post"
    body="#jsonData#"
    headers="Content-Type: application/json"
    out="response">
```

### File Upload

```cfml
<cf_httpclient 
    url="https://api.example.com/upload"
    method="post"
    file="C:\path\to\document.pdf"
    filefield="attachment"
    out="response">
```

### File Download

```cfml
<cf_httpclient 
    url="https://cdn.example.com/files/report.pdf"
    method="get"
    file="C:\downloads\report.pdf"
    out="response">

<cfif status EQ "OK">
    <cfoutput>File downloaded successfully</cfoutput>
</cfif>
```

### Custom Headers

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    headers="Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...#Chr(10)#X-API-Key: your-api-key-here"
    out="response">
```

### Basic Authentication

```cfml
<cf_httpclient 
    url="https://api.example.com/protected"
    method="get"
    user="admin"
    pass="secret123"
    schemes="basic"
    out="response">
```

### Session Management

```cfml
<!--- First request - establishes session --->
<cf_httpclient 
    url="https://api.example.com/login"
    method="post"
    body="username=admin&password=secret"
    session="my-session-001"
    out="response">

<!--- Subsequent requests - reuses session --->
<cf_httpclient 
    url="https://api.example.com/profile"
    method="get"
    session="my-session-001"
    out="profile">

<!--- Close session when done --->
<cf_httpclient 
    fnc="close"
    session="my-session-001">
```

### Asynchronous Requests

```cfml
<!--- Start async request --->
<cf_httpclient 
    url="https://api.example.com/long-running-task"
    method="get"
    async="y"
    reqid="task-001">

<!--- Do other work --->
<cfset performOtherOperations()>

<!--- Check if complete --->
<cf_httpclient 
    fnc="get"
    reqid="task-001"
    out="result">

<cfif status EQ "OK">
    <cfoutput>Task complete: #result#</cfoutput>
<cfelseif status EQ "IP">
    <cfoutput>Task still in progress...</cfoutput>
</cfif>
```

### SSL/TLS Configuration

```cfml
<cf_httpclient 
    url="https://secure.example.com/api"
    method="get"
    ssl="y"
    out="response">
```

### Proxy Configuration

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    proxyserver="proxy.company.com"
    proxyport="8080"
    proxyuser="proxyuser"
    proxypass="proxypass"
    out="response">
```

### DNS Lookup

```cfml
<cf_httpclient 
    fnc="dns"
    url="example.com"
    out="ipaddress">

<cfoutput>IP Address: #ipaddress#</cfoutput>
```

---

## Migration from CFX_HTTP5

cf_httpclient provides **100% parameter compatibility** with CFX_HTTP5. Migration requires only a tag name change:

**Before (CFX_HTTP5):**
```cfml
<cfx_http5 
    url="https://api.example.com/data"
    method="get"
    out="result">
```

**After (cf_httpclient):**
```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="result">
```

All 56 parameters, output variables, and behaviors remain identical.

---

## Complete Parameter Reference

### Required Parameters

| Parameter | Description |
|-----------|-------------|
| `url` | Target URL (string) |
| `method` | HTTP method: GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH, TRACE |
| `out` | Variable name for response body (string) |

### Optional Parameters

| Parameter | Description |
|-----------|-------------|
| `fnc` | Function: HTTP (default), GET, WAIT, CANCEL, CLOSE, DNS |
| `async` | Asynchronous execution: Y/N (default: N) |
| `reqid` | Request ID for async operations |
| `session` | Session ID for cookie persistence |
| `body` | Request body content |
| `file` | File path for upload/download |
| `filefield` | Form field name for file upload |
| `headers` | Custom headers (newline-separated) |
| `timeout` | Timeout in milliseconds (default: 30000) |
| `user` | Username for authentication |
| `pass` | Password for authentication |
| `schemes` | Auth schemes: BASIC, DIGEST, NTLM |
| `proxyserver` | Proxy server hostname |
| `proxyport` | Proxy server port |
| `proxyuser` | Proxy username |
| `proxypass` | Proxy password |
| `ssl` | Enable SSL/TLS: Y/N |
| `charset` | Character encoding (default: UTF-8) |

...and 37 more parameters. See [Programmer's Reference](docs/Programmer's%20Reference%20-%20cf_httpclient.md) for complete list.

### Output Variables

After execution, the following variables are set in the calling scope:

| Variable | Description |
|----------|-------------|
| `status` | "OK" (success) or "ER" (error) |
| `httpstatus` | HTTP status code (200, 404, 500, etc.) |
| `msg` | Error message (if status="ER") |
| `[out variable]` | Response body content |

---

## Documentation

- **[Programmer's Reference](docs/Programmer's%20Reference%20-%20cf_httpclient.md)** - Complete documentation with all 56 parameters, examples, and troubleshooting

---

## Performance Tuning

cf_httpclient supports optional INI configuration for advanced tuning:

**config/cf_httpclient.ini:**
```ini
[HttpClient]
MaxConnections=200
MaxPerRoute=20
ConnectionTimeout=30000
SocketTimeout=30000
RequestTimeout=30000
KeepAlive=true
```

Place `cf_httpclient.ini` in your application's `/config/` directory. The tag will automatically detect and load it.

---

## License

cf_httpclient is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for details.

### Apache HttpClient

This implementation uses Apache HttpClient 4.5.14:
- **License:** Apache License 2.0
- **Website:** https://hc.apache.org/
- **Copyright:** The Apache Software Foundation

See `lib/LICENSE.txt` and `lib/NOTICE.txt` for Apache HttpClient license information.

---

## Disclaimer

**THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED.**

**USE AT YOUR OWN RISK. THIS PROJECT IS NOT UNDER ACTIVE DEVELOPMENT.**

Users assume all risks associated with the use of this software. The authors and contributors accept no liability for any damages, data loss, system failures, or other issues arising from the use of this software.

---

## Version Information

**Version:** 1.00  
**Date:** 2025-01-06  
**Compatibility:** Adobe ColdFusion 11+ | Lucee 5+  
**Maintenance Status:** Not under active development  

---

## Support

This project is **not under active development**. No support is provided.

For issues, refer to the [Programmer's Reference](docs/Programmer's%20Reference%20-%20cf_httpclient.md) troubleshooting section.

---

**End of README**
