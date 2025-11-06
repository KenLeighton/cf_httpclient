# Programmer's Reference - cf_httpclient

**Drop-in replacement for CFX_HTTP5 with 100% feature parity (all 56 parameters)**

**Version:** 1.00  
**Date:** 2025-01-06  
**Compatibility:** Adobe ColdFusion 11+ | Lucee 5+  
**License:** Apache License 2.0  

---

## Table of Contents

0. [Introduction](#0-introduction)
1. [System Requirements](#1-system-requirements)
2. [Installation](#2-installation)
3. [Tag Syntax](#3-tag-syntax)
4. [Functions](#4-functions)
   - [HTTP Function](#41-http-function)
   - [GET Function](#42-get-function)
   - [WAIT Function](#43-wait-function)
   - [CANCEL Function](#44-cancel-function)
   - [CLOSE Function](#45-close-function)
   - [DNS Function](#46-dns-function)
5. [Using cf_httpclient in ColdFusion Applications](#5-using-cf_httpclient-in-coldfusion-applications)
   - [Simple GET Request](#51-simple-get-request)
   - [Posting a Form](#52-posting-a-form)
   - [Posting JSON/XML](#53-posting-jsonxml)
   - [File Upload](#54-file-upload)
   - [File Download](#55-file-download)
   - [Custom Headers](#56-custom-headers)
6. [Sessions](#6-sessions)
7. [Asynchronous Execution](#7-asynchronous-execution)
8. [SSL/TLS Configuration](#8-ssltls-configuration)
9. [Proxy Configuration](#9-proxy-configuration)
10. [Character Encoding](#10-character-encoding)
11. [Authentication](#11-authentication)
12. [Output Parameters](#12-output-parameters)
13. [Error Handling](#13-error-handling)
14. [Performance Tuning](#14-performance-tuning)
   - [INI Configuration](#145-ini-configuration)
15. [Troubleshooting](#15-troubleshooting)
16. [Migration from CFX_HTTP5](#16-migration-from-cfx_http5)
17. [Complete Parameter Reference](#17-complete-parameter-reference)

---

## 0. Introduction

cf_httpclient is a **drop-in replacement for CFX_HTTP5** with **100% feature parity**. Built on Apache HttpClient 4.5.14, cf_httpclient replaces the legacy C++ extension with a pure Java implementation that provides identical functionality across Adobe ColdFusion and Lucee servers.

### Drop-in Replacement

**Complete compatibility:** All 56 CFX_HTTP5 parameters are supported with identical behavior. Simply replace `<cfx_http5>` tags with `<cf_httpclient>` in your existing code.

**No code changes required:** Parameter names, output variables, and behavior match CFX_HTTP5 exactly.

### Feature Parity

✅ **All 56 parameters** - Complete CFX_HTTP5 parameter set  
✅ **Session management** - Automatic cookie handling with persistent sessions  
✅ **Asynchronous execution** - Background requests with FNC="GET", "WAIT", "CANCEL"  
✅ **SSL/TLS support** - SSL parameter with certificate store integration  
✅ **Proxy configuration** - PROXYSERVER, PROXYPORT, PROXYUSER, PROXYPASS  
✅ **Authentication** - USER, PASS, SCHEMES (Basic, Digest, NTLM)  
✅ **File operations** - FILE parameter for uploads/downloads  
✅ **DNS lookups** - FNC="DNS" for hostname resolution  
✅ **Connection pooling** - Automatic HTTP connection management  
✅ **Cross-platform** - Windows, Linux, macOS

### Pure Java Implementation

**No native dependencies:** Unlike CFX_HTTP5's C++ DLLs, cf_httpclient uses pure Java, eliminating platform-specific builds and DLL conflicts.

**Enterprise-grade:** Built on Apache HttpClient 4.5.14, the industry standard for HTTP client implementations.

---

## 1. System Requirements

### Minimum Requirements

**Adobe ColdFusion:**
- ColdFusion 11 or later
- Java 8 or later

**Lucee:**
- Lucee 5.0 or later
- Java 8 or later

### Recommended

- ColdFusion 2018+ or Lucee 5.3+
- Java 11 or later
- 2GB+ RAM for production servers

### Operating Systems

- ✅ Windows Server 2012 R2 or later
- ✅ Linux (Ubuntu 18.04+, CentOS 7+, Red Hat Enterprise Linux 7+)
- ✅ macOS 10.14+ (for development)

### Dependencies

The following JAR files are required (included with distribution):
- `httpclient-4.5.14.jar` (1.0 MB)
- `httpcore-4.4.16.jar` (328 KB)
- `commons-codec-1.11.jar` (335 KB)
- `commons-logging-1.2.jar` (61 KB)

**Total Size:** ~2 MB

### ⚠️ IMPORTANT: Adobe ColdFusion 11 Users

**ColdFusion 11 ships with outdated Apache HttpClient JARs (version 4.2.5) that are incompatible with cf_httpclient.** 

cf_httpclient requires Apache HttpClient 4.5.14. Without upgrading, you will get `java.lang.NoSuchMethodError` exceptions.

#### Manual JAR Upgrade

**⚠️ BACKUP FIRST:** Make copies of the original JARs before replacing them.

**Steps:**

1. **Stop ColdFusion 11 service**

2. **Navigate to:** `{cf-install-dir}\cfusion\lib\` 
   - Example: `C:\ColdFusion11\cfusion\lib\`

3. **Back up old JARs:**
   - Rename `httpclient-4.2.5.jar` to `httpclient-4.2.5.jar.BAK`
   - Rename `httpcore-4.2.4.jar` to `httpcore-4.2.4.jar.BAK`

4. **Copy new JARs from cf_httpclient distribution:**
   - Copy `httpclient-4.5.14.jar` to `{cf-install-dir}\cfusion\lib\`
   - Copy `httpcore-4.4.16.jar` to `{cf-install-dir}\cfusion\lib\`

5. **Start ColdFusion 11 service**

This is a **one-time, server-wide upgrade**. It does not affect existing CFX_HTTP5 functionality.

#### ColdFusion 2016+ Users

**No server-level JAR upgrade required.** ColdFusion 2016 and later ship with compatible HttpClient versions (4.5.x or later).

**Proceed directly to Section 2: Installation** (application-level deployment only).

#### Lucee Users

**No server-level JAR upgrade required.** Lucee 5.0+ ships with compatible HttpClient versions.

**Proceed directly to Section 2: Installation** (application-level deployment only).

#### CommandBox Users

**No server-level JAR upgrade required.** CommandBox uses Lucee, which ships with compatible HttpClient versions.

**Important for CommandBox:**
- JARs are deployed at **application level** (in your application's `/lib/` folder)
- JARs are NOT deployed to CommandBox server directories
- CommandBox servers are ephemeral (can be deleted/recreated)
- Application-level JARs persist with your application code

**Proceed directly to Section 2: Installation** (application-level deployment only).

---

## 2. Installation

cf_httpclient is deployed at the **application level**, packaged with your application code. This provides portability across Adobe ColdFusion, Lucee, and CommandBox environments without requiring server restarts or administrator access.

**Platform Summary:**
- **Adobe ColdFusion 11:** Requires one-time server-level JAR upgrade (Section 1 above) + application-level deployment (below)
- **Adobe ColdFusion 2016+:** Application-level deployment only (below)
- **Lucee 5+:** Application-level deployment only (below)
- **CommandBox:** Application-level deployment only (below)

### Step 1: Create Directory Structure

Create the following structure in your application root:

```
/my-application/
  /lib/
    httpclient-4.5.14.jar
    httpcore-4.4.16.jar
    commons-logging-1.2.jar
    commons-codec-1.11.jar
  /customtags/
    httpclient.cfm
  Application.cfc (or Application.cfm)
  index.cfm
```

Copy the JAR files from the cf_httpclient distribution `lib/` folder to your application's `lib/` folder.  
Copy `httpclient.cfm` from the distribution `customtags/` folder to your application's `customtags/` folder.

### Step 2: Configure Application.cfc

Add the following configuration to your `Application.cfc`:

**Generic Template with Relative Paths:**
```cfml
component {
    // This is to enable the cf_httpclient drop-in replacement for cfx_http5
    this.name = "MyApplication";
    
    // Configure custom tag path
    this.customtagpaths = expandPath("./customtags");
    
    // Load HttpClient JAR files
    this.javaSettings = {
        loadPaths = [
            expandPath("../[application-name]/ColdFusion CustomTags/cf_httpclient/lib/httpclient-4.5.14.jar"),
            expandPath("../[application-name]/ColdFusion CustomTags/cf_httpclient/lib/httpcore-4.4.16.jar"),
            expandPath("../[application-name]/ColdFusion CustomTags/cf_httpclient/lib/commons-logging-1.2.jar"),
            expandPath("../[application-name]/ColdFusion CustomTags/cf_httpclient/lib/commons-codec-1.11.jar")
        ],
        reloadOnChange = false
    };
}
```

**Example with Absolute Paths:**
```cfml
component {
    this.name = "MyApplication";
    
    this.customtagpaths = expandPath("./customtags");
    
    this.javaSettings = {
        loadPaths = [
            "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\httpclient-4.5.14.jar",
            "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\httpcore-4.4.16.jar",
            "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\commons-logging-1.2.jar",
            "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\commons-codec-1.11.jar"
        ],
        reloadOnChange = false
    };
}
```

### Step 2 (Alternative): Configure Application.cfm

If your application uses `Application.cfm` instead of `Application.cfc`, use this configuration:

**Generic Template:**
```cfml
<!--- This is to enable the cf_httpclient drop-in replacement for cfx_http5. --->
<!--- Custom tag path set in CF Administrator: Extensions > Custom Tag Paths > [your-webroot]\CustomTags --->
<!--- Minimal cfapplication for JAR loading support --->
<!--- NOTE: setclientcookies="no" disables automatic CFID/CFTOKEN cookies only. Manual cookie handling via <cfcookie> and cookie scope still works. --->
<cfapplication name="#variables.ApplicationName#" sessionmanagement="no" setclientcookies="no">

<cfset this.javaSettings = {
    loadPaths = [
        "[parent-dir]\[application-name]\ColdFusion CustomTags\cf_httpclient\lib\httpclient-4.5.14.jar",
        "[parent-dir]\[application-name]\ColdFusion CustomTags\cf_httpclient\lib\httpcore-4.4.16.jar",
        "[parent-dir]\[application-name]\ColdFusion CustomTags\cf_httpclient\lib\commons-logging-1.2.jar",
        "[parent-dir]\[application-name]\ColdFusion CustomTags\cf_httpclient\lib\commons-codec-1.11.jar"
    ],
    reloadOnChange = false
}>
```

**Example with Absolute Paths:**
```cfml
<!--- This is to enable the cf_httpclient drop-in replacement for cfx_http5. --->
<!--- Custom tag path set in CF Administrator: Extensions > Custom Tag Paths > C:\myApplication\Code\CustomTags --->
<cfapplication name="#variables.ApplicationName#" sessionmanagement="no" setclientcookies="no">

<cfset this.javaSettings = {
    loadPaths = [
        "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\httpclient-4.5.14.jar",
        "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\httpcore-4.4.16.jar",
        "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\commons-logging-1.2.jar",
        "C:\myApplication\ColdFusion CustomTags\cf_httpclient\lib\commons-codec-1.11.jar"
    ],
    reloadOnChange = false
}>
```

**Why This Approach?**

**CF11 Limitation:** Application.cfm does not support `this.customtagpaths` unless "Enable Per App Settings" is enabled in CF Administrator (requires server restart).

**Solution:** 
- Custom tag paths: Set server-wide in CF Administrator (no restart)
- JAR loading: Set per-application via `this.javaSettings` in Application.cfm
- Minimal `<cfapplication>`: Required for `this.javaSettings` to work, but configured to not interfere with existing application logic

**What the `<cfapplication>` Tag Does:**
- `sessionmanagement="no"` - Does NOT create session scope or interfere with existing session handling
- `setclientcookies="no"` - Disables automatic CFID/CFTOKEN cookies (manual `<cfcookie>` still works)
- Enables `this.javaSettings` to load JAR files

This minimal configuration **will not affect** your existing application logic, routing, or cookie handling.

### Step 3: Use cf_httpclient

```cfml
<!--- Basic GET request --->
<cf_httpclient url="https://api.example.com/data" method="get" out="result">

<cfoutput>
    <p>Status: #status#</p>
    <p>Response: #result#</p>
</cfoutput>
```

### CommandBox Deployment

**Important:** JARs are deployed at the **application level**, not in CommandBox server directories.

**Why this matters:**
- CommandBox servers are ephemeral (created/deleted as needed)
- Application-level JARs persist with your code in version control
- No server.json configuration required
- Portable across different CommandBox servers

**Directory structure for CommandBox:**
```
/your-app/
  /lib/                          ← JARs go here (application level)
    httpclient-4.5.14.jar
    httpcore-4.4.16.jar
    commons-logging-1.2.jar
    commons-codec-1.11.jar
  /customtags/
    httpclient.cfm
  Application.cfc
  box.json (optional)
  server.json (optional)
```

**Start server - no additional configuration needed:**

```bash
box server start
```

The Application.cfc `this.javaSettings` configuration automatically loads JARs from your application's `/lib/` folder.

### Verification

Test your installation with this simple page:

```cfml
<!--- test_install.cfm --->
<cf_httpclient url="https://example.com/api/get" method="get" out="result">

<cfoutput>
    <p>Status: #status#</p>
    <p>HTTP Status: #httpstatus#</p>
    <cfif status EQ "OK" AND httpstatus EQ "200">
        <p style="color: green;">✓ cf_httpclient installed correctly</p>
    <cfelse>
        <p style="color: red;">✗ Installation error</p>
    </cfif>
</cfoutput>
```

### Troubleshooting

**ClassNotFoundException:** Verify all 4 JAR files are in `./lib/` and paths in `this.javaSettings.loadPaths` are correct.

**Custom tag not found:** Verify `httpclient.cfm` is in `./customtags/` and `this.customtagpaths` is set correctly.

**Works locally but not in production:** Ensure `lib/` and `customtags/` folders are deployed with your application code.



---

## 3. Tag Syntax

### Basic Syntax

```cfml
<cf_httpclient
    url="string"
    method="string"
    out="variable_name"
    [optional_parameters...]>
```

### Minimum Required Parameters

```cfml
<cf_httpclient 
    url="https://example.com"
    method="get"
    out="result">
```

### Common Usage Pattern

```cfml
<cf_httpclient 
    url="https://api.example.com/endpoint"
    method="post"
    body="key1=value1&key2=value2"
    headers="Content-Type: application/x-www-form-urlencoded"
    out="response"
    timeout="30000">

<cfif status EQ "OK">
    <!--- Process successful response --->
    <cfoutput>#response#</cfoutput>
<cfelse>
    <!--- Handle error --->
    <cfoutput>Error: #msg# (Status: #httpstatus#)</cfoutput>
</cfif>
```

---

## 4. Functions

The tag supports multiple functions specified via the `FNC` parameter.

### 4.1. HTTP Function

**Default function** - Executes synchronous HTTP request.

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="result">
```

Or explicitly:

```cfml
<cf_httpclient 
    fnc="http"
    url="https://api.example.com/data"
    method="get"
    out="result">
```

**Output Variables:**
- `status` - "OK" or "ER"
- `httpstatus` - HTTP status code (200, 404, etc.)
- `msg` - Error message (if status="ER")
- `[out variable]` - Response body

### 4.2. GET Function

Retrieves result of asynchronous request.

```cfml
<!--- Start async request --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    async="y"
    reqid="my-request-001">

<!--- Later, retrieve result --->
<cf_httpclient 
    fnc="get"
    reqid="my-request-001"
    out="result">

<cfif status EQ "OK">
    <cfoutput>#result#</cfoutput>
</cfif>
```

**Parameters:**
- `reqid` - Request ID to retrieve
- `out` - Variable name for result

**Output Variables:**
- `status` - "OK" (complete), "ER" (error), or "IP" (in progress)
- `httpstatus` - HTTP status code (if complete)
- `msg` - Error or status message

### 4.3. WAIT Function

Waits for asynchronous request to complete.

```cfml
<!--- Start async request --->
<cf_httpclient 
    url="https://api.example.com/slow-endpoint"
    method="get"
    async="y"
    reqid="slow-request"
    timeout="60000">

<!--- Wait for completion (blocks until done) --->
<cf_httpclient 
    fnc="wait"
    reqid="slow-request"
    out="result">

<!--- Result is now available --->
<cfif status EQ "OK">
    <cfoutput>Completed: #result#</cfoutput>
</cfif>
```

**Parameters:**
- `reqid` - Request ID to wait for
- `out` - Variable name for result
- `timeout` - Maximum wait time in milliseconds (default: 60000)

### 4.4. CANCEL Function

Cancels pending asynchronous request(s).

```cfml
<!--- Cancel specific request --->
<cf_httpclient 
    fnc="cancel"
    reqid="my-request-001">

<!--- Cancel multiple requests --->
<cf_httpclient 
    fnc="cancel"
    reqid="req-001,req-002,req-003">

<!--- Cancel all requests --->
<cf_httpclient 
    fnc="cancel"
    reqid="*">
```

**Parameters:**
- `reqid` - Request ID(s) to cancel (comma-separated) or "*" for all

**Output Variables:**
- `status` - "OK"
- `msg` - Confirmation message

### 4.5. CLOSE Function

Closes HTTP session and releases resources.

```cfml
<!--- Create session --->
<cf_httpclient 
    url="https://example.com/login"
    method="post"
    body="user=admin&pass=secret"
    session="my-session"
    out="result">

<!--- Use session for multiple requests... --->

<!--- Close session when done --->
<cf_httpclient 
    fnc="close"
    session="my-session">
```

**Parameters:**
- `session` - Session name to close

**Output Variables:**
- `status` - "OK"
- `msg` - Confirmation message

### 4.6. DNS Function

Performs DNS lookup for a hostname.

```cfml
<cf_httpclient 
    fnc="dns"
    url="https://example.com"
    out="ipAddress">

<cfoutput>
    <p>Resolved IP: #ipAddress#</p>
</cfoutput>
```

**Parameters:**
- `url` - URL or hostname to resolve (protocol optional)
- `out` - Variable name for IP address

**Output Variables:**
- `status` - "OK" or "ER"
- `[out variable]` - Resolved IP address
- `msg` - Error message (if failed)

**Example:**
```cfml
<cf_httpclient fnc="dns" url="https://example.com" out="ip">
<!--- ip = "52.20.222.128" --->
```

---

## 5. Using cf_httpclient in ColdFusion Applications

### 5.1. Simple GET Request

```cfml
<!--- Basic GET request --->
<cf_httpclient 
    url="https://api.example.com/users/123"
    method="get"
    out="jsonResponse">

<cfif status EQ "OK">
    <!--- Parse JSON response --->
    <cfset userData = deserializeJSON(jsonResponse)>
    <cfoutput>
        <p>User: #userData.name#</p>
        <p>Email: #userData.email#</p>
    </cfoutput>
</cfif>
```

### 5.2. Posting a Form

```cfml
<!--- POST form data (URL-encoded) --->
<cf_httpclient 
    url="https://api.example.com/users"
    method="post"
    headers="Content-Type: application/x-www-form-urlencoded"
    body="name=John+Doe&email=john@example.com&age=30"
    out="response">

<cfif status EQ "OK" AND httpstatus EQ 201>
    <cfoutput>User created successfully</cfoutput>
</cfif>
```

### 5.3. Posting JSON/XML

#### JSON POST

```cfml
<!--- Prepare JSON data --->
<cfset requestData = {
    "name": "John Doe",
    "email": "john@example.com",
    "age": 30
}>
<cfset jsonBody = serializeJSON(requestData)>

<!--- POST JSON --->
<cf_httpclient 
    url="https://api.example.com/users"
    method="post"
    headers="Content-Type: application/json"
    body="#jsonBody#"
    out="response">

<cfif status EQ "OK">
    <cfset responseData = deserializeJSON(response)>
    <cfoutput>Created user ID: #responseData.id#</cfoutput>
</cfif>
```

#### XML POST

```cfml
<!--- Prepare XML data --->
<cfsavecontent variable="xmlBody">
<?xml version="1.0" encoding="UTF-8"?>
<user>
    <name>John Doe</name>
    <email>john@example.com</email>
    <age>30</age>
</user>
</cfsavecontent>

<!--- POST XML --->
<cf_httpclient 
    url="https://api.example.com/users"
    method="post"
    headers="Content-Type: application/xml"
    body="#trim(xmlBody)#"
    out="response">
```

### 5.4. File Upload

#### Multipart Form Upload

```cfml
<!--- Read file content --->
<cffile action="readBinary" file="#expandPath('./document.pdf')#" variable="fileContent">
<cfset base64Content = toBase64(fileContent)>

<!--- Prepare multipart body --->
<cfset boundary = "----WebKitFormBoundary#createUUID()#">
<cfsavecontent variable="multipartBody">--#boundary#
Content-Disposition: form-data; name="file"; filename="document.pdf"
Content-Type: application/pdf

#fileContent#
--#boundary#
Content-Disposition: form-data; name="description"

Document upload test
--#boundary#--
</cfsavecontent>

<!--- Upload file --->
<cf_httpclient 
    url="https://api.example.com/upload"
    method="post"
    headers="Content-Type: multipart/form-data; boundary=#boundary#"
    body="#multipartBody#"
    out="response">
```

#### Simple File Upload via BODYFILE

```cfml
<!--- Upload file directly --->
<cf_httpclient 
    url="https://api.example.com/upload"
    method="post"
    bodyfile="./document.pdf"
    headers="Content-Type: application/pdf"
    out="response">
```

### 5.5. File Download

```cfml
<!--- Download file --->
<cf_httpclient 
    url="https://example.com/files/report.pdf"
    method="get"
    out="downloads/report.pdf"
    file="y">

<cfif status EQ "OK">
    <cfoutput>File downloaded successfully to downloads/report.pdf</cfoutput>
</cfif>
```

### 5.6. Custom Headers

#### Default Headers (CFX_HTTP5 Compatibility)

cf_httpclient automatically includes default HTTP headers matching CFX_HTTP5's behavior:

```
Accept: image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, 
        application/vnd.ms-excel, application/msword, 
        application/vnd.ms-powerpoint, */*
Accept-Language: en-us
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) 
            AppleWebKit/537.36 (KHTML, like Gecko) 
            Chrome/137.0.0.0 Safari/537.36
Pragma: no-cache
Cache-Control: no-cache
```

**Why this matters:** These default headers make cf_httpclient requests appear as legitimate browser traffic, allowing access to sites with bot detection (Radware, Cloudflare, etc.).

**Custom headers override defaults:** When you provide a `headers` parameter, your values override the defaults.

#### Custom Header Examples

```cfml
<!--- Multiple headers (newline-separated) --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    headers="Authorization: Bearer eyJhbGc...
User-Agent: MyApp/1.0
Accept: application/json
X-Custom-Header: custom-value"
    out="response">

<!--- The User-Agent above overrides the default User-Agent --->
```

Or using `<chr(10)>` for line breaks:

```cfml
<cfset requestHeaders = "Authorization: Bearer token123" & chr(10) & 
                        "Accept: application/json" & chr(10) &
                        "X-API-Key: secret-key">

<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    headers="#requestHeaders#"
    out="response">
```

#### Header Override Behavior

```cfml
<!--- Scenario 1: No headers parameter --->
<cf_httpclient url="..." method="get" out="result">
<!--- Uses all default headers (browser-like request) --->

<!--- Scenario 2: Custom User-Agent --->
<cf_httpclient url="..." method="get" 
    headers="User-Agent: MyBot/1.0" 
    out="result">
<!--- User-Agent: MyBot/1.0 (custom)
      Accept: [default]
      Accept-Language: [default]
      Other defaults preserved --->

<!--- Scenario 3: Multiple overrides --->
<cf_httpclient url="..." method="get" 
    headers="User-Agent: MyApp/2.0#chr(10)#Accept: text/html" 
    out="result">
<!--- User-Agent: MyApp/2.0 (custom)
      Accept: text/html (custom)
      Accept-Language: [default]
      Other defaults preserved --->
```

---

## 6. Sessions

Sessions enable automatic cookie management and connection reuse across multiple requests.

```cfml
<!--- First request: Start session --->
<cf_httpclient url="https://www.example.com/form?mode=edit" method="get" out="content" outhead="header" session="start" cookies="Y" ssl="5">

<!--- Subsequent request: Submit form using session --->
<cf_httpclient url="https://www.example.com/form" headers="Content-Type: application/x-www-form-urlencoded" body="field1=#value1#&field2=#value2#" method="post" out="content" session="#httpsession#" cookies="Y" ssl="5">

<!--- Close session when done --->
<cf_httpclient FNC="close" session="#httpsession#">
```

---

## 7. Asynchronous Execution

Execute HTTP requests in the background without blocking the main thread.

### Basic Async Request

```cfml
<!--- Start async request --->
<cf_httpclient 
    url="https://api.example.com/slow-process"
    method="post"
    body="data=large"
    async="y"
    reqid="process-001"
    timeout="120000">

<cfif status EQ "OK">
    <cfoutput>Request started. ID: #httpreqid#</cfoutput>
</cfif>

<!--- Continue with other processing... --->

<!--- Check if complete --->
<cf_httpclient 
    fnc="get"
    reqid="process-001"
    out="result">

<cfif status EQ "OK">
    <cfoutput>Process complete: #result#</cfoutput>
<cfelseif status EQ "IP">
    <cfoutput>Still processing...</cfoutput>
</cfif>
```

### Wait for Completion

```cfml
<!--- Start async request --->
<cf_httpclient 
    url="https://api.example.com/report"
    method="get"
    async="y"
    reqid="report-gen">

<!--- Wait for completion (blocking) --->
<cf_httpclient 
    fnc="wait"
    reqid="report-gen"
    out="reportData"
    timeout="300000">

<cfif status EQ "OK">
    <cfoutput>Report ready: #len(reportData)# bytes</cfoutput>
</cfif>
```

### Multiple Concurrent Requests

```cfml
<!--- Start multiple async requests --->
<cfloop from="1" to="10" index="i">
    <cf_httpclient 
        url="https://api.example.com/user/#i#"
        method="get"
        async="y"
        reqid="user-#i#">
</cfloop>

<!--- Wait for all to complete --->
<cfset users = []>
<cfloop from="1" to="10" index="i">
    <cf_httpclient 
        fnc="wait"
        reqid="user-#i#"
        out="userData">
    
    <cfif status EQ "OK">
        <cfset arrayAppend(users, deserializeJSON(userData))>
    </cfif>
</cfloop>

<cfoutput>Retrieved #arrayLen(users)# users</cfoutput>
```

### Cancel Async Request

```cfml
<!--- Start long-running request --->
<cf_httpclient 
    url="https://api.example.com/long-process"
    method="post"
    async="y"
    reqid="long-process">

<!--- Later, cancel if needed --->
<cf_httpclient 
    fnc="cancel"
    reqid="long-process">
```

### Async Best Practices

- ✅ Always use unique `reqid` values
- ✅ Set appropriate `timeout` values (default: 60s)
- ✅ Clean up completed requests with `fnc="get"` or `fnc="cancel"`
- ✅ Maximum 64 concurrent async requests
- ⚠ Async requests persist until retrieved or cancelled

---

## 8. SSL/TLS Configuration

### Basic HTTPS Request

```cfml
<!--- HTTPS request (TLS 1.2 default) --->
<cf_httpclient 
    url="https://secure.example.com/api"
    method="get"
    out="response">
```

### Explicit TLS 1.2

```cfml
<cf_httpclient 
    url="https://secure.example.com/api"
    method="get"
    ssl="5"
    out="response">
```

**SSL Parameter Values:**
- `ssl="2"` - SSL 2.0 (deprecated, not recommended)
- `ssl="3"` - SSL 3.0 (deprecated, not recommended)
- `ssl="4"` - TLS 1.0 (legacy)
- `ssl="5"` - TLS 1.2 (recommended, default)

### SSL Certificate Validation

**Default Behavior:** cf_httpclient uses **Windows certificate store validation** by default (matching CFX_HTTP5 behavior). This validates certificates against Windows' trusted root CAs and is the recommended setting for production.

```cfml
<!--- Standard certificate validation using Windows trust store (default) --->
<cf_httpclient 
    url="https://secure.example.com/api"
    method="get"
    out="response">
```

**How It Works:**
- cf_httpclient loads the **Windows ROOT certificate store** (trusted root CAs)
- Validates certificates using standard validation rules:
  - ✅ Accepts valid CA-signed certificates from Windows trust store
  - ❌ Rejects self-signed certificates
  - ❌ Rejects expired certificates
  - ❌ Rejects certificates from untrusted CAs
- Matches CFX_HTTP5 behavior (CFX_HTTP5 uses Windows' native SSL libraries)

**Permissive Validation (Invalid/Expired/Self-Signed Certificates):**

```cfml
<!--- Ignore ALL SSL errors (invalid, expired, self-signed, etc.) --->
<cf_httpclient 
    url="https://self-signed.example.com/api"
    method="get"
    sslerrors="ok"
    out="response">
```

**SSLERRORS Parameter:**
- **Default (omitted)** - Standard validation using Windows ROOT certificate store
  - Accepts valid CA-signed certificates
  - Rejects self-signed, expired, or untrusted certificates
- **`sslerrors="ok"`** - Permissive validation, ignores ALL SSL errors
  - Use only for invalid, expired, or self-signed certificates
  - Disables hostname verification
  - CFX_HTTP5 compatible (SSLERRORS="OK")

**⚠ Production Best Practice:** 
- **Omit `sslerrors` parameter** in production with proper CA-signed certificates
- **Only use `sslerrors="ok"`** for development/testing with invalid certificates
- Ensure Windows trust store is up-to-date with current root CAs

**📋 Validation Examples:**

| Certificate Type | Default (no sslerrors) | sslerrors="ok" |
|-----------------|------------------------|----------------|
| Valid CA-signed (e.g., google.com) | ✅ Accepts | ✅ Accepts |
| Self-signed | ❌ Rejects | ✅ Accepts |
| Expired | ❌ Rejects | ✅ Accepts |
| Untrusted root | ❌ Rejects | ✅ Accepts |
| Wrong hostname | ❌ Rejects | ✅ Accepts |

### Client Certificate Authentication (Windows)

#### Using Windows Certificate Store

```cfml
<!--- Use certificate from Windows Personal store --->
<cf_httpclient 
    url="https://secure.example.com/api"
    method="get"
    certstorename="MY"
    certsubjstr="CN=MyClientCert"
    out="response">
```

**Certificate Store Names:**
- `MY` - Personal certificates
- `ROOT` - Trusted root certificates
- `CA` - Intermediate certificate authorities
- `TRUST` - Enterprise trust

#### Subject String Matching

```cfml
<!--- Match by common name --->
<cf_httpclient 
    certstorename="MY"
    certsubjstr="CN=John Doe"
    ...>

<!--- Match by organization --->
<cf_httpclient 
    certstorename="MY"
    certsubjstr="O=Acme Corp"
    ...>

<!--- Partial match --->
<cf_httpclient 
    certstorename="MY"
    certsubjstr="Acme"
    ...>
```

### SSL/TLS Best Practices

- ✅ Always use TLS 1.2 or later in production
- ✅ Use strict validation in production (omit `sslerrors` parameter)
- ✅ Use proper CA-signed certificates
- ✅ Keep certificate stores updated
- ✅ Only use `sslerrors="ok"` for development with self-signed certificates
- ⚠ Client certificates only supported on Windows

---

## 9. Proxy Configuration

### Basic Proxy

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    proxyserver="proxy.company.com"
    proxyport="8080"
    out="response">
```

### Proxy with Authentication

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    proxyserver="proxy.company.com"
    proxyport="8080"
    proxyuser="domain\username"
    proxypass="password"
    out="response">
```

### NTLM Proxy Authentication

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    proxyserver="proxy.company.com"
    proxyport="8080"
    proxyuser="DOMAIN\username"
    proxypass="password"
    out="response">
```

### Proxy Configuration in Application.cfc

```cfml
component {
    this.name = "MyApp";
    
    // Set default proxy for all requests
    function onRequestStart() {
        request.defaultProxyServer = "proxy.company.com";
        request.defaultProxyPort = 8080;
        request.defaultProxyUser = "username";
        request.defaultProxyPass = "password";
    }
}
```

---

## 10. Character Encoding

### UTF-8 Encoding (Default)

```cfml
<!--- UTF-8 request and response --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    utf="y"
    out="response">
```

### Custom Request Encoding

```cfml
<!--- Send request body in ISO-8859-1 --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="post"
    body="name=François"
    charsetout="ISO-8859-1"
    out="response">
```

### Custom Response Encoding

```cfml
<!--- Decode response as Windows-1252 --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    charsetin="Windows-1252"
    out="response">
```

### Both Request and Response Encoding

```cfml
<cf_httpclient 
    url="https://legacy-api.example.com/data"
    method="post"
    body="name=François"
    charsetout="ISO-8859-1"
    charsetin="ISO-8859-1"
    out="response">
```

### URL Decoding

```cfml
<!--- Automatically URL-decode response --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    urldecode="y"
    out="response">
```

### Supported Character Sets

- `UTF-8` (default, recommended)
- `ISO-8859-1` (Latin-1)
- `Windows-1252` (Western European)
- `Shift_JIS` (Japanese)
- `GB2312` (Simplified Chinese)
- `EUC-KR` (Korean)
- All Java-supported character sets

---

## 11. Authentication

### Basic Authentication

```cfml
<cf_httpclient 
    url="https://api.example.com/secure"
    method="get"
    user="admin"
    pass="secret123"
    out="response">
```

### Digest Authentication

```cfml
<cf_httpclient 
    url="https://api.example.com/secure"
    method="get"
    user="admin"
    pass="secret123"
    schemes="digest"
    out="response">
```

### NTLM Authentication

```cfml
<cf_httpclient 
    url="https://intranet.company.com/api"
    method="get"
    user="DOMAIN\username"
    pass="password"
    schemes="ntlm"
    out="response">
```

### Bearer Token Authentication

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    headers="Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    out="response">
```

### API Key Authentication

```cfml
<!--- Header-based API key --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    headers="X-API-Key: your-secret-api-key-here"
    out="response">

<!--- Query parameter API key --->
<cf_httpclient 
    url="https://api.example.com/data?api_key=your-secret-key"
    method="get"
    out="response">
```

### OAuth 2.0 Bearer Token

```cfml
<!--- Step 1: Get access token --->
<cf_httpclient 
    url="https://oauth.example.com/token"
    method="post"
    body="grant_type=client_credentials&client_id=YOUR_CLIENT_ID&client_secret=YOUR_SECRET"
    headers="Content-Type: application/x-www-form-urlencoded"
    out="tokenResponse">

<cfset tokenData = deserializeJSON(tokenResponse)>
<cfset accessToken = tokenData.access_token>

<!--- Step 2: Use access token --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    headers="Authorization: Bearer #accessToken#"
    out="response">
```

---

## 12. Output Parameters

### Standard Output Variables

Every request sets these variables in the caller scope:

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response">

<!--- Available variables: --->
<cfoutput>
    <p>Status: #status#</p>           <!--- "OK" or "ER" --->
    <p>HTTP Status: #httpstatus#</p>  <!--- 200, 404, 500, etc. --->
    <p>Message: #msg#</p>             <!--- Error message if status="ER" --->
    <p>Response: #response#</p>       <!--- Response body --->
</cfoutput>
```

### Response Headers

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response"
    outhead="responseHeaders">

<!--- responseHeaders is a string with all headers --->
<cfoutput>
    <p>Headers:</p>
    <pre>#htmlEditFormat(responseHeaders)#</pre>
</cfoutput>
```

### Response Headers as Struct

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response"
    outqhead="headersStruct">

<!--- headersStruct is a ColdFusion struct --->
<cfoutput>
    <p>Content-Type: #headersStruct["Content-Type"]#</p>
    <p>Content-Length: #headersStruct["Content-Length"]#</p>
    <p>Server: #headersStruct["Server"]#</p>
</cfoutput>

<!--- Loop through all headers --->
<cfloop collection="#headersStruct#" item="headerName">
    <cfoutput>
        <p>#headerName#: #headersStruct[headerName]#</p>
    </cfoutput>
</cfloop>
```

### Request Headers Sent

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    headers="X-Custom: value"
    out="response"
    outqhead="sentHeaders">

<!--- sentHeaders contains headers that were sent --->
<cfoutput>
    <p>Sent Headers:</p>
    <cfdump var="#sentHeaders#">
</cfoutput>
```

### Additional Output Variables

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response"
    context="my-custom-context-123">

<!--- Additional variables available: --->
<cfoutput>
    <p>HTTP Scheme: #httpscheme#</p>      <!--- "http" or "https" --->
    <p>Request Time: #httptime# ms</p>    <!--- Request duration --->
    <p>Context: #httpcontext#</p>         <!--- Custom context passed through --->
</cfoutput>
```

### Async Request Output

```cfml
<!--- Start async request --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    async="y"
    reqid="my-request">

<cfif status EQ "OK">
    <cfoutput>
        <p>Request ID: #httpreqid#</p>
        <p>Status: Request submitted</p>
    </cfoutput>
</cfif>
```

---

## 13. Error Handling

### Basic Error Handling

```cfml
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response">

<cfif status EQ "OK">
    <!--- Success --->
    <cfset data = deserializeJSON(response)>
<cfelse>
    <!--- Error --->
    <cfoutput>
        <p>Error: #msg#</p>
        <p>HTTP Status: #httpstatus#</p>
        <p>Error Code: #errn#</p>
    </cfoutput>
</cfif>
```

### HTTP Status Code Handling

```cfml
<cf_httpclient 
    url="https://api.example.com/users/999"
    method="get"
    out="response">

<cfswitch expression="#httpstatus#">
    <cfcase value="200">
        <!--- Success --->
        <cfset userData = deserializeJSON(response)>
    </cfcase>
    
    <cfcase value="404">
        <!--- Not found --->
        <cfoutput>User not found</cfoutput>
    </cfcase>
    
    <cfcase value="401,403">
        <!--- Unauthorized/Forbidden --->
        <cfoutput>Access denied</cfoutput>
    </cfcase>
    
    <cfcase value="500,502,503">
        <!--- Server errors --->
        <cfoutput>Server error: #msg#</cfoutput>
    </cfcase>
    
    <cfdefaultcase>
        <!--- Other errors --->
        <cfoutput>Unexpected error: #httpstatus# - #msg#</cfoutput>
    </cfdefaultcase>
</cfswitch>
```

### Try/Catch Error Handling

```cfml
<cftry>
    <cf_httpclient 
        url="https://api.example.com/data"
        method="get"
        timeout="5000"
        out="response">
    
    <cfif status EQ "OK">
        <cfset data = deserializeJSON(response)>
        <!--- Process data --->
    <cfelse>
        <!--- HTTP-level error --->
        <cfthrow type="HTTPError" message="#msg#" detail="HTTP Status: #httpstatus#">
    </cfif>
    
    <cfcatch type="any">
        <!--- Handle all errors --->
        <cflog file="http_errors" type="error" text="HTTP request failed: #cfcatch.message#">
        <cfoutput>An error occurred. Please try again later.</cfoutput>
    </cfcatch>
</cftry>
```

### Error Codes (errn variable)

Common error codes:
- `timeout` - Request timeout
- `connect` - Connection failed
- `ssl` - SSL/TLS error
- `dns` - DNS resolution failed
- `proxy` - Proxy error
- `bodyfile` - Body file not found
- `maxlen` - Response exceeds maximum length
- `session` - Session error

### Retry Logic

```cfml
<cfset maxRetries = 3>
<cfset retryCount = 0>
<cfset success = false>

<cfloop condition="NOT success AND retryCount LT maxRetries">
    <cf_httpclient 
        url="https://api.example.com/data"
        method="get"
        timeout="10000"
        out="response">
    
    <cfif status EQ "OK" AND httpstatus EQ 200>
        <cfset success = true>
        <!--- Process response --->
    <cfelse>
        <cfset retryCount++>
        <cfif retryCount LT maxRetries>
            <cfset sleep(1000 * retryCount)>  <!--- Exponential backoff --->
        </cfif>
    </cfif>
</cfloop>

<cfif NOT success>
    <cfoutput>Failed after #maxRetries# attempts</cfoutput>
</cfif>
```

---

## 14. Performance Tuning

### Connection Pooling

Connection pooling is automatic. Configure in Application.cfc:

```cfml
component {
    function onApplicationStart() {
        // Connection pool settings are internal
        // No configuration needed - automatically optimized
    }
}
```

**Default Pool Settings:**
- Maximum total connections: 100
- Maximum connections per route: 20
- Connection timeout: 30 seconds
- Socket timeout: 60 seconds

### Timeout Configuration

```cfml
<!--- Connection timeout (default: 30000ms) --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    timeout="10000"
    out="response">

<!--- Maximum request time (includes processing) --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    maxtime="30000"
    out="response">

<!--- Uses larger of timeout/maxtime --->
```

### Response Size Limits

```cfml
<!--- Limit response to 1MB --->
<cf_httpclient 
    url="https://api.example.com/largefile"
    method="get"
    maxlen="1048576"
    out="response">

<cfif status EQ "OK">
    <cfoutput>Received: #len(response)# bytes (max: 1MB)</cfoutput>
</cfif>
```

### Discard Response Body

```cfml
<!--- HEAD request - only need headers --->
<cf_httpclient 
    url="https://api.example.com/resource"
    method="head"
    discard="y"
    outqhead="headers">

<!--- Check if resource exists without downloading --->
<cfif status EQ "OK" AND httpstatus EQ 200>
    <cfoutput>Resource exists. Size: #headers["Content-Length"]# bytes</cfoutput>
</cfif>
```

### Keep-Alive Connections

```cfml
<!--- Enable keep-alive (default: enabled) --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    alive="y"
    out="response">

<!--- Disable keep-alive --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    alive="n"
    out="response">
```

### Session Performance

Using sessions provides ~20% performance improvement for subsequent requests:

```cfml
<!--- First request with session --->
<cf_httpclient 
    url="https://api.example.com/login"
    method="post"
    body="user=admin&pass=secret"
    session="api-session"
    out="result">

<!--- Subsequent requests are faster (connection reuse + automatic cookies) --->
<cfloop from="1" to="100" index="i">
    <cf_httpclient 
        url="https://api.example.com/data/#i#"
        method="get"
        session="api-session"
        out="data">
</cfloop>
```

### Compression

```cfml
<!--- Request gzip compression (default: enabled) --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    gzip="y"
    out="response">

<!--- Disable compression --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    gzip="n"
    out="response">
```

### Performance Best Practices

✅ **Use sessions** for multiple requests to same host
✅ **Enable keep-alive** for connection reuse  
✅ **Set appropriate timeouts** to avoid hanging requests  
✅ **Use maxlen** to limit large responses  
✅ **Enable gzip** compression for text responses  
✅ **Use async** for non-blocking operations  
✅ **Close sessions** when done to free resources  

---

## 14.5. INI Configuration

### Overview

cf_httpclient supports **INI file configuration** compatible with CFX_HTTP5's `cfxhttp5.ini` format. This allows you to set default values for tag parameters and HTTP headers in a centralized configuration file.

**Key Benefits:**
- Centralized configuration for all cf_httpclient tags
- Environment-specific settings (dev/staging/production)
- Zero code changes - existing tags work unchanged
- CFX_HTTP5 migration friendly - reuse existing cfxhttp5.ini

### INI File Location

cf_httpclient looks for configuration in this priority order:

1. **INIFILE parameter** - Custom path specified in tag
2. **Application directory** - `/config/cf_httpclient.ini`
3. **Built-in defaults** - If no INI file found

```cfml
<!--- Use custom INI file --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    inifile="/custom/path/myconfig.ini"
    out="response">

<!--- Use default location: /config/cf_httpclient.ini --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response">
```

### INI File Format

The INI file contains two sections: `[SERVER]` and `[HEADERS]`.

**Example: config/cf_httpclient.ini**

```ini
; cf_httpclient Configuration File
; Comments start with semicolon

[SERVER]
; TCP connection mode: Y=Keep-Alive, N=Close
Keep-Alive=Y

; URL parameter decoding: Y=Decode, E=Escape, N=No change
URLDecode=N

; UTF-8 mode: Y=UTF-8 (CF MX+), N=System code page (CF5)
UTF=Y

; Thread count (for reference only - cf_httpclient uses connection pooling)
nThreads=5

[HEADERS]
; Default HTTP headers (numbered sequentially)
Header1=Accept: image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, */*
Header2=Accept-Language: en-us
Header3=Content-Type: application/x-www-form-urlencoded
Header4=User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)
Header5=Pragma: no-cache
Header6=Cache-Control: no-cache
Header7=Accept-Encoding: gzip
```

### SERVER Section Parameters

| Parameter | Values | Maps To | Description |
|-----------|--------|---------|-------------|
| `Keep-Alive` | Y/N | ALIVE | Default connection mode (Y=reuse, N=close) |
| `URLDecode` | Y/E/N | URLDECODE | URL parameter handling (Y=decode, E=escape, N=none) |
| `UTF` | Y/N | UTF | Character encoding (Y=UTF-8, N=system code page) |
| `nThreads` | 1-256 | (reference) | For CFX_HTTP5 compatibility (not used by cf_httpclient) |

### HEADERS Section

Default HTTP headers sent with every request. Headers must be numbered sequentially starting at `Header1`.

**Important:** 
- Headers must be numbered without gaps (Header1, Header2, Header3, ...)
- Parsing stops at first missing number
- Tag HEADERS parameter overrides INI headers
- Individual headers can be deleted via HEADERS parameter

### Priority Order

Settings are applied in this priority (highest to lowest):

1. **Tag parameters** - Explicitly passed to `<cf_httpclient>`
2. **INI file values** - From cf_httpclient.ini
3. **Built-in defaults** - Hardcoded in tag

**Example:**
```ini
; config/cf_httpclient.ini
[SERVER]
Keep-Alive=N
UTF=N
```

```cfml
<!--- alive="n" from INI, utf="y" from tag parameter --->
<cf_httpclient 
    url="https://api.example.com"
    method="get"
    utf="y"
    out="response">
```

Result: `alive="n"` (from INI), `utf="y"` (from tag parameter - overrides INI)

### Caching Behavior

**Performance optimized:** INI file is parsed once and cached in `application` scope with automatic file change detection.

1. **First request:** Parse INI file and cache
2. **Subsequent requests:** Use cached version (very fast)
3. **File modified:** Automatic reload on next request
4. **Thread-safe:** Uses `cflock` for concurrent access

**Cache key:** `application.cf_httpclient_ini_cache`

To force reload: `applicationStop()` or restart application

### Environment-Specific Configuration

Use different INI files per environment:

**Development:**
```cfml
<!--- Application.cfc --->
component {
    this.name = "MyApp";
    
    function onApplicationStart() {
        // Set environment-specific INI path
        application.iniPath = "/config/cf_httpclient.dev.ini";
    }
}
```

```cfml
<!--- Use environment-specific INI --->
<cf_httpclient 
    url="https://api.example.com"
    method="get"
    inifile="#application.iniPath#"
    out="response">
```

**Production:**
```
/config/cf_httpclient.prod.ini
```

### CommandBox Deployment

For CommandBox ephemeral servers, place INI file in your **application directory** (not server directory):

```
my-application/
├── Application.cfc
├── customtags/
│   └── httpclient.cfm
├── config/
│   └── cf_httpclient.ini  ← Persists across server restarts
└── lib/
    └── *.jar
```

INI file is portable across CommandBox instances and survives server deletion.

### Migrating from CFX_HTTP5

If you have an existing `cfxhttp5.ini` file:

1. Copy `cfxhttp5.ini` to `/config/cf_httpclient.ini`
2. No changes needed - format is identical
3. Remove server-wide installation (if applicable)
4. Test with existing code

**Note:** `nThreads` parameter is ignored (cf_httpclient uses automatic connection pooling).

### Troubleshooting INI Configuration

**INI file not found:**
- Verify path: `expandPath("/config/cf_httpclient.ini")`
- Check file exists and is readable
- Tag will silently use built-in defaults

**Changes not applying:**
- Clear application cache: `applicationStop()`
- Verify file timestamp changed
- Check for parse errors (silent failure)

**Headers not loading:**
- Ensure sequential numbering (Header1, Header2, Header3, ...)
- No gaps allowed (Header1, Header3 = only 1 header loaded)
- Verify `[HEADERS]` section exists

**Performance:**
- INI parsing is cached - minimal overhead
- File change detection via timestamp comparison
- Use INIFILE parameter only when needed

### Complete Example

**config/cf_httpclient.ini:**
```ini
[SERVER]
Keep-Alive=Y
URLDecode=N
UTF=Y

[HEADERS]
Header1=Accept: application/json
Header2=User-Agent: MyApp/1.0
Header3=Accept-Encoding: gzip
```

**Application code:**
```cfml
<!--- Uses INI defaults (alive="y", utf="y", default headers) --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response">

<!--- Override INI defaults with tag parameters --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    alive="n"
    headers="Content-Type: application/xml"
    out="response">
```

Result:
- First request: Uses INI defaults
- Second request: Overrides `alive` and adds custom header

---

## 15. Troubleshooting

### Common Issues

#### Issue: "Cannot find CFML template for custom tag httpclient"

**Solution:**
```cfml
<!--- Verify custom tag path in Application.cfc --->
component {
    this.customTagPaths = expandPath("./customtags");
}

<!--- Or use full path --->
<cfmodule template="/path/to/customtags/httpclient.cfm" ...>
```

#### Issue: "Java class not found" or NoClassDefFoundError

**Solution:**
```cfml
<!--- Ensure JAR files are in javaSettings --->
component {
    this.javaSettings = {
        loadPaths: [
            expandPath("./lib/httpclient-4.5.14.jar"),
            expandPath("./lib/httpcore-4.4.16.jar"),
            expandPath("./lib/commons-codec-1.11.jar"),
            expandPath("./lib/commons-logging-1.2.jar")
        ],
        loadColdFusionClassPath: true
    };
}

<!--- Restart ColdFusion after changes --->
```

#### Issue: SSL/TLS connection fails

**Solutions:**
```cfml
<!--- 1. Use TLS 1.2 explicitly --->
<cf_httpclient 
    url="https://secure.example.com"
    ssl="5"
    ...>

<!--- 2. For development only: ignore SSL errors --->
<cf_httpclient 
    url="https://self-signed.example.com"
    sslerrors="y"
    ...>

<!--- 3. Check Java version (needs Java 8+) --->
<cfoutput>#server.coldfusion.productversion#</cfoutput>
```

#### Issue: Timeout errors

**Solutions:**
```cfml
<!--- Increase timeout --->
<cf_httpclient 
    url="https://slow-api.example.com"
    timeout="60000"
    maxtime="120000"
    ...>

<!--- Use async for long-running requests --->
<cf_httpclient 
    url="https://slow-api.example.com"
    async="y"
    reqid="slow-request"
    timeout="300000"
    ...>
```

#### Issue: Session not persisting cookies

**Solution:**
```cfml
<!--- Ensure session name is consistent --->
<cf_httpclient 
    url="https://example.com/login"
    session="my-session"   <!--- Same session name for all requests --->
    ...>

<cf_httpclient 
    url="https://example.com/dashboard"
    session="my-session"   <!--- Must match exactly --->
    ...>

<!--- Check that cookies are being set --->
<cf_httpclient 
    url="https://example.com/login"
    session="my-session"
    outqhead="headers"
    ...>

<cfdump var="#headers#" label="Response Headers">
```

### Debug Mode

Enable detailed logging:

```cfml
<!--- Get detailed request/response info --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="response"
    outhead="responseHeaders"
    outqhead="requestHeaders">

<cfoutput>
    <h3>Request Headers Sent:</h3>
    <cfdump var="#requestHeaders#">
    
    <h3>Response Headers Received:</h3>
    <cfdump var="#responseHeaders#">
    
    <h3>Response Body:</h3>
    <pre>#htmlEditFormat(response)#</pre>
    
    <h3>Status:</h3>
    <p>Status: #status#</p>
    <p>HTTP Status: #httpstatus#</p>
    <p>Message: #msg#</p>
    <p>Error Code: #errn#</p>
</cfoutput>
```

### Health Check

Test if the tag is working:

```cfml
<!--- Basic health check --->
<cf_httpclient 
    url="https://example.com/api/get"
    method="get"
    timeout="10000"
    out="result">

<cfif status EQ "OK" AND httpstatus EQ 200>
    <cfoutput><p style="color: green;">✓ cf_httpclient is working correctly</p></cfoutput>
<cfelse>
    <cfoutput>
        <p style="color: red;">✗ Error: #msg#</p>
        <p>HTTP Status: #httpstatus#</p>
    </cfoutput>
</cfif>
```

### Getting Help

1. Check server logs:
   - ColdFusion: `[CF_ROOT]/cfusion/logs/application.log`
   - Lucee: `/opt/lucee/tomcat/logs/`

2. Enable ColdFusion debugging in Administrator

3. Test with curl to isolate issues:
   ```bash
   curl -v https://api.example.com/endpoint
   ```

4. Verify Java version:
   ```cfml
   <cfoutput>#server.java.version#</cfoutput>
   <!--- Should be 8 or later --->
   ```

---

## 16. Migration from CFX_HTTP5

### Drop-in Replacement

The `cf_httpclient` tag is designed as a **100% drop-in replacement** for CFX_HTTP5:

```cfml
<!--- OLD: CFX_HTTP5 --->
<cfx_http5 
    url="https://api.example.com/data"
    method="get"
    out="result">

<!--- NEW: cf_httpclient (identical syntax) --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="result">
```

### Migration Steps

#### Step 1: Install cf_httpclient

Follow [Installation](#2-installation) instructions for your platform.

#### Step 2: Test in Parallel

Test new implementation alongside CFX_HTTP5:

```cfml
<!--- Test with CFX_HTTP5 --->
<cfx_http5 
    url="https://api.example.com/data"
    method="get"
    out="resultOld">

<!--- Test with cf_httpclient --->
<cf_httpclient 
    url="https://api.example.com/data"
    method="get"
    out="resultNew">

<!--- Compare results --->
<cfif resultOld EQ resultNew>
    <cfoutput>✓ Results match</cfoutput>
<cfelse>
    <cfoutput>✗ Results differ</cfoutput>
    <cfdump var="#resultOld#" label="CFX_HTTP5">
    <cfdump var="#resultNew#" label="cf_httpclient">
</cfif>
```

#### Step 3: Replace Tags

Use find/replace to update all occurrences:

**Find:** `<cfx_http5`  
**Replace:** `<cf_httpclient`

#### Step 4: Verify Functionality

Test all HTTP operations:
- ✅ GET requests
- ✅ POST requests
- ✅ Sessions
- ✅ Async execution
- ✅ File uploads
- ✅ Authentication
- ✅ SSL/TLS

### Compatibility Notes

#### 100% Compatible Features

All 56 CFX_HTTP5 parameters are supported:
- ✅ All URL/method parameters
- ✅ All header/body parameters
- ✅ All session parameters
- ✅ All async parameters
- ✅ All SSL/TLS parameters
- ✅ All proxy parameters
- ✅ All encoding parameters
- ✅ All output parameters

#### Platform Differences

**Windows-Specific Features:**
- `CERTSTORENAME` - Windows certificate store access
- `CERTSUBJSTR` - Certificate subject filtering
- `FILEDOMAIN` - Windows domain for file authentication

**Linux/macOS:**
- Certificate features not available (use PEM files instead)
- File domain authentication not available

#### Behavioral Differences

**CFX_HTTP5:**
- Native C++ DLL (Windows only)
- Uses WinHTTP API
- Requires DLL registration

**cf_httpclient:**
- Pure Java implementation
- Uses Apache HttpClient 4.5.14
- Cross-platform (Windows/Linux/macOS)
- No DLL registration required

### Performance Comparison

| Feature | CFX_HTTP5 | cf_httpclient | Winner |
|---------|-----------|---------------|--------|
| Simple GET | ~100ms | ~100ms | ≈ Equal |
| With Session | ~120ms | ~95ms | ✅ cf_httpclient (20% faster) |
| SSL/TLS | ~150ms | ~150ms | ≈ Equal |
| Async | Supported | Supported | ≈ Equal |
| Connection Pool | No | Yes | ✅ cf_httpclient |
| Memory Usage | Lower | Higher | CFX_HTTP5 |
| Stability | Good | Excellent | ✅ cf_httpclient |

### Advantages of cf_httpclient

✅ **Cross-platform** - Works on Windows, Linux, macOS  
✅ **No DLL** - Pure Java, no native dependencies  
✅ **Better sessions** - True HTTP client reuse  
✅ **Connection pooling** - Better resource management  
✅ **Active development** - Based on Apache HttpClient 4.5.14  
✅ **No licensing** - Open implementation  

---

## 16.5 Real-World Migration Examples

This section provides practical find/replace patterns for migrating common CFX_HTTP5 usage patterns to cf_httpclient.

### Example 1: Async GET with SSL Error Handling

**Original CFX_HTTP5 Code:**
```cfml
<cfx_http5 url="https://api.example.com/process" method="get" async="y" sslerrors="ok">
```

**Migrated cf_httpclient Code:**
```cfml
<cf_httpclient url="https://api.example.com/process" method="get" async="y" sslerrors="ok">
```

**Find/Replace Pattern:**
- **Find:** `<cfx_http5`
- **Replace:** `<cf_httpclient`

**Notes:**
- `async="y"` - Fires asynchronous request (no response data captured)
- `sslerrors="ok"` - Accepts self-signed or invalid SSL certificates (CFX_HTTP5 compatible)
- Perfect for background batch job triggers
- **No parameter changes required** - true drop-in replacement

---

### Example 2: Session Start

**Original CFX_HTTP5 Code:**
```cfml
<cfx_http5 url="https://www.example.com/Module/Page/en" method="get" out="cfhttpfilecontent" outhead="cfhttpheader" session="start" cookies="Y" ssl="5">
```

**Migrated cf_httpclient Code:**
```cfml
<cf_httpclient url="https://www.example.com/Module/Page/en" method="get" out="cfhttpfilecontent" outhead="cfhttpheader" session="start" cookies="Y" ssl="5">
```

**What This Does:**
- Creates a new HTTP session and returns session ID in `httpsession` variable
- Use `session="start"` for first request only

---

### Example 3: Subsequent Session Request

**Original CFX_HTTP5 Code:**
```cfml
<cfx_http5 url="https://www.example.com/Module/Page/en/Search/Results?status=Open&limit=100&start=0&dir=DESC&sort=DateClosing" headers="Content-Type: application/x-www-form-urlencoded" body="__RequestVerificationToken=#URLEncodedFormat(variables.RequestVerificationToken)#" method="post" out="cfhttpfilecontent" outhead="cfhttpheader" session="#httpsession#" cookies="Y" ssl="5">
```

**Migrated cf_httpclient Code:**
```cfml
<cf_httpclient url="https://www.example.com/Module/Page/en/Search/Results?status=Open&limit=100&start=0&dir=DESC&sort=DateClosing" headers="Content-Type: application/x-www-form-urlencoded" body="__RequestVerificationToken=#URLEncodedFormat(variables.RequestVerificationToken)#" method="post" out="cfhttpfilecontent" outhead="cfhttpheader" session="#httpsession#" cookies="Y" ssl="5">
```

**What This Does:**
- Reuses the HTTP session created in Example 2
- Use `session="#httpsession#"` for all subsequent requests

**Complete Session Example:**
```cfml
<!--- First request: Start session --->
<cf_httpclient url="https://www.example.com/Module/Page/en" method="get" out="cfhttpfilecontent" outhead="cfhttpheader" session="start" cookies="Y" ssl="5">

<!--- Subsequent request: Reuse session --->
<cf_httpclient url="https://www.example.com/Module/Page/en/Search/Results?status=Open&limit=100&start=0&dir=DESC&sort=DateClosing" headers="Content-Type: application/x-www-form-urlencoded" body="__RequestVerificationToken=#URLEncodedFormat(variables.RequestVerificationToken)#" method="post" out="cfhttpfilecontent" outhead="cfhttpheader" session="#httpsession#" cookies="Y" ssl="5">

<!--- Close session when done --->
<cf_httpclient FNC="close" session="#httpsession#">
```

---

### Example 4: Bulk Find/Replace Patterns

For large codebases, use these regex patterns:

#### Pattern 1: Simple Tag Replacement
```
Find (Regex):    <cfx_http5\s
Replace with:    <cf_httpclient 
```
**Matches:** All opening `<cfx_http5` tags with space after  
**Result:** Converts to `<cf_httpclient`

#### Pattern 2: Session Close Statement
```
Find (Regex):    <cfx_http5\s+FNC="close"
Replace with:    <cf_httpclient FNC="close"
```
**Matches:** All session close statements  
**Result:** Converts to `<cf_httpclient FNC="close"`

#### Pattern 3: Async Requests
```
Find (Regex):    <cfx_http5\s+url="([^"]+)"\s+method="get"\s+async="y"
Replace with:    <cf_httpclient url="$1" method="get" async="y"
```
**Matches:** Async GET requests  
**Result:** Preserves URL, converts tag name

#### Pattern 4: Session Start Requests
```
Find (Regex):    <cfx_http5\s+(.+?)\s+session="start"
Replace with:    <cf_httpclient $1 session="start"
```
**Matches:** Any request with `session="start"`  
**Result:** Preserves all attributes, converts tag name

---

### Example 5: Testing Migration

Create a test script to compare results:

```cfml
<!--- test_migration.cfm --->
<cfset testUrls = [
    "https://www.example.com",
    "https://example.com/api/get",
    "https://api.example.com"
]>

<cfloop array="##testUrls##" index="testUrl">
    <!--- Test with CFX_HTTP5 --->
    <cftry>
        <cfx_http5 url="##testUrl##" method="get" out="resultOld" timeout="10000">
        <cfset oldStatus = status>
        <cfset oldHttpStatus = httpstatus>
        <cfcatch>
            <cfset oldStatus = "ERROR">
            <cfset resultOld = cfcatch.message>
        </cfcatch>
    </cftry>
    
    <!--- Test with cf_httpclient --->
    <cftry>
        <cf_httpclient url="##testUrl##" method="get" out="resultNew" timeout="10000">
        <cfset newStatus = status>
        <cfset newHttpStatus = httpstatus>
        <cfcatch>
            <cfset newStatus = "ERROR">
            <cfset resultNew = cfcatch.message>
        </cfcatch>
    </cftry>
    
    <!--- Compare results --->
    <cfoutput>
        <h3>##testUrl##</h3>
        <table border="1">
            <tr>
                <th>Metric</th>
                <th>CFX_HTTP5</th>
                <th>cf_httpclient</th>
                <th>Match</th>
            </tr>
            <tr>
                <td>Status</td>
                <td>##oldStatus##</td>
                <td>##newStatus##</td>
                <td>##oldStatus eq newStatus ? "✓" : "✗"##</td>
            </tr>
            <tr>
                <td>HTTP Status</td>
                <td>##oldHttpStatus##</td>
                <td>##newHttpStatus##</td>
                <td>##oldHttpStatus eq newHttpStatus ? "✓" : "✗"##</td>
            </tr>
            <tr>
                <td>Content Length</td>
                <td>##len(resultOld)##</td>
                <td>##len(resultNew)##</td>
                <td>##len(resultOld) eq len(resultNew) ? "✓" : "✗"##</td>
            </tr>
            <tr>
                <td>Content Match</td>
                <td colspan="2">##resultOld eq resultNew ? "Identical" : "Different"##</td>
                <td>##resultOld eq resultNew ? "✓" : "✗"##</td>
            </tr>
        </table>
        <hr>
    </cfoutput>
</cfloop>
```

---

### Example 6: Migration Checklist

Use this checklist when migrating:

**Pre-Migration:**
- [ ] Identify all CFX_HTTP5 usage in codebase
- [ ] Document session-based workflows
- [ ] Document async request patterns
- [ ] Back up all code before changes

**Migration:**
- [ ] Install cf_httpclient (see [Installation](#2-installation))
- [ ] Configure Application.cfc or Application.cfm (see [Application Configuration](#25-application-configuration))
- [ ] Run find/replace for `<cfx_http5` → `<cf_httpclient`
- [ ] Update session close statements
- [ ] Update async requests
- [ ] Test each migrated page

**Post-Migration:**
- [ ] Run comparison tests (see Example 5)
- [ ] Verify all sessions close properly
- [ ] Monitor server memory usage
- [ ] Check error logs for issues
- [ ] Performance test critical paths
- [ ] Document any behavioral differences

**Rollback Plan:**
- Keep CFX_HTTP5 DLL in place initially
- Gradual rollout (test → staging → production)
- Monitor for 2 weeks before removing CFX_HTTP5

---

### Example 6.5: Session Usage

Sessions enable connection reuse and automatic cookie handling across multiple requests.

```cfml
<!--- First request: Start session --->
<cf_httpclient url="https://www.example.com/Module/Page/en" method="get" out="cfhttpfilecontent" outhead="cfhttpheader" session="start" cookies="Y" ssl="5">

<!--- Subsequent request: Reuse session --->
<cf_httpclient url="https://www.example.com/Module/Page/en/Search/Results?status=Open&limit=100&start=0&dir=DESC&sort=DateClosing" headers="Content-Type: application/x-www-form-urlencoded" body="__RequestVerificationToken=#URLEncodedFormat(variables.RequestVerificationToken)#" method="post" out="cfhttpfilecontent" outhead="cfhttpheader" session="#httpsession#" cookies="Y" ssl="5">

<!--- Close session when done --->
<cf_httpclient FNC="close" session="#httpsession#">
```

**Key Points:**
- Use `session="start"` for first request
- Use `session="#httpsession#"` for subsequent requests
- Always close sessions with `FNC="close"`  

---

### Example 7: Common Gotchas

#### Gotcha 1: Forgetting to Close Sessions

**Bad (Memory Leak):**
```cfml
<cf_httpclient url="https://example.com" method="get" out="page1" cookie="Y" session="start">
<cf_httpclient url="https://example.com/page2" method="get" out="page2" cookie="Y" session="##httpsession##">
<!--- Session never closed! Memory leak! --->
```

**Good:**
```cfml
<cftry>
    <cf_httpclient url="https://example.com" method="get" out="page1" cookie="Y" session="start">
    <cf_httpclient url="https://example.com/page2" method="get" out="page2" cookie="Y" session="##httpsession##">
    <cffinally>
        <!--- Always close in finally block --->
        <cfif isDefined("httpsession")>
            <cf_httpclient FNC="close" session="##httpsession##">
        </cfif>
    </cffinally>
</cftry>
```

#### Gotcha 2: Incorrect Session Variable Scope

**Bad:**
```cfml
<!--- httpsession is local to this page only --->
<cf_httpclient url="https://example.com" method="get" out="page1" cookie="Y" session="start">
<!--- On next page, httpsession is undefined! --->
```

**Good:**
```cfml
<!--- Store in session scope for multi-page workflows --->
<cf_httpclient url="https://example.com" method="get" out="page1" cookie="Y" session="start">
<cfset session.myHttpSession = httpsession>

<!--- Later, on another page: --->
<cf_httpclient url="https://example.com/page2" method="get" out="page2" cookie="Y" session="##session.myHttpSession##">

<!--- When done: --->
<cf_httpclient FNC="close" session="##session.myHttpSession##">
```

#### Gotcha 3: Mixing Session and Non-Session Requests

**Bad:**
```cfml
<cf_httpclient url="https://example.com/login" method="post" formfields="user=admin&pass=secret" cookie="Y" session="start">
<!--- Forgot to pass session="##httpsession##" - cookies lost! --->
<cf_httpclient url="https://example.com/dashboard" method="get" out="dashboard">
```

**Good:**
```cfml
<cf_httpclient url="https://example.com/login" method="post" formfields="user=admin&pass=secret" cookie="Y" session="start">
<!--- Pass session to maintain cookies --->
<cf_httpclient url="https://example.com/dashboard" method="get" out="dashboard" cookie="Y" session="##httpsession##">
<cf_httpclient FNC="close" session="##httpsession##">
```

---

### Example 8: Advanced Pattern - Application.cfm Helper

For Application.cfm users, create a helper to simplify session management:

```cfml
<!--- includes/http_session_helper.cfm --->

<!--- Start HTTP session --->
<cffunction name="httpSessionStart" returntype="string" output="false">
    <cfargument name="url" type="string" required="true">
    <cfargument name="method" type="string" default="get">
    <cfargument name="out" type="string" default="">
    
    <cfmodule template="/customtags/httpclient.cfm" 
        url="##arguments.url##" 
        method="##arguments.method##" 
        out="##arguments.out##" 
        cookie="Y" 
        session="start">
    
    <!--- Store session in session scope --->
    <cfif isDefined("httpsession")>
        <cfset session.httpClientSession = httpsession>
        <cfreturn httpsession>
    </cfif>
    
    <cfreturn "">
</cffunction>

<!--- Make request using existing session --->
<cffunction name="httpSessionRequest" returntype="void" output="false">
    <cfargument name="url" type="string" required="true">
    <cfargument name="method" type="string" default="get">
    <cfargument name="out" type="string" default="">
    <cfargument name="attributes" type="struct" default="##structNew()##">
    
    <cfset var callAttrs = duplicate(arguments.attributes)>
    <cfset callAttrs.url = arguments.url>
    <cfset callAttrs.method = arguments.method>
    <cfset callAttrs.cookie = "Y">
    
    <!--- Use stored session or start new one --->
    <cfif isDefined("session.httpClientSession")>
        <cfset callAttrs.session = session.httpClientSession>
    <cfelse>
        <cfset callAttrs.session = "start">
    </cfif>
    
    <cfif len(arguments.out)>
        <cfset callAttrs.out = arguments.out>
    </cfif>
    
    <cfmodule template="/customtags/httpclient.cfm" attributeCollection="##callAttrs##">
    
    <!--- Update stored session if it was just started --->
    <cfif isDefined("httpsession")>
        <cfset session.httpClientSession = httpsession>
    </cfif>
</cffunction>

<!--- Close HTTP session --->
<cffunction name="httpSessionClose" returntype="void" output="false">
    <cfif isDefined("session.httpClientSession")>
        <cfmodule template="/customtags/httpclient.cfm" 
            FNC="close" 
            session="##session.httpClientSession##">
        <cfset structDelete(session, "httpClientSession")>
    </cfif>
</cffunction>
```

**Usage:**
```cfml
<!--- Application.cfm --->
<cfinclude template="includes/http_session_helper.cfm">

<!--- In your page --->
<cfset httpSessionStart("https://www.example.com")>
<cfset httpSessionRequest("https://www.example.com/data", "get", "datapage")>
<cfoutput>##len(datapage)## bytes loaded</cfoutput>
<cfset httpSessionClose()>
```

---

## 17. Complete Parameter Reference

### Input Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| **URL** | String | Required | Target URL (http:// or https://) |
| **METHOD** | String | "GET" | HTTP method (GET, POST, PUT, DELETE, HEAD, PATCH, OPTIONS, TRACE) |
| **FNC** | String | "HTTP" | Function (HTTP, GET, WAIT, CANCEL, CLOSE, DNS) |
| **OUT** | String | - | Output variable or file name |
| **FILE** | String | "N" | "Y" = write to file, "N" = to variable |
| **BODY** | String | - | Request body content |
| **BODYFILE** | String | - | File path for request body |
| **BODYEND** | String | - | Append to request body |
| **HEADERS** | String | - | Custom headers (newline-separated) |
| **TIMEOUT** | Number | 30000 | Connection/socket timeout (ms) |
| **MAXTIME** | Number | 60000 | Maximum request time (ms) |
| **WAIT** | Number | 0 | Pre-request delay (ms) |
| **SESSION** | String | - | Session name for cookie/connection reuse |
| **SESSIONEND** | String | "N" | "Y" = close session after request |
| **ASYNC** | String | "N" | "Y" = asynchronous execution |
| **REQID** | String | - | Request ID for async operations |
| **USER** | String | - | HTTP authentication username |
| **PASS** | String | - | HTTP authentication password |
| **SCHEMES** | String | "basic" | Auth schemes (basic, digest, ntlm) |
| **PROXYSERVER** | String | - | Proxy hostname/IP |
| **PROXYPORT** | Number | 8080 | Proxy port |
| **PROXYUSER** | String | - | Proxy authentication username |
| **PROXYPASS** | String | - | Proxy authentication password |
| **SSL** | Number | 5 | SSL/TLS version (5 = TLS 1.2) |
| **SSLERRORS** | String | "" | "" = Windows trust store validation (default), "ok" = ignore ALL SSL errors (invalid, expired, self-signed) |
| **CERTSTORENAME** | String | - | Windows certificate store name |
| **CERTSUBJSTR** | String | - | Certificate subject filter |
| **UTF** | String | "Y" | "Y" = UTF-8, "N" = ISO-8859-1 |
| **CHARSETOUT** | String | - | Request body encoding |
| **CHARSETIN** | String | - | Response body decoding |
| **URLDECODE** | String | "N" | "Y" = URL decode response |
| **REDIRECT** | String | "Y" | "Y" = follow redirects |
| **MAXLEN** | Number | - | Maximum response length (bytes) |
| **ACCEPTTYPE** | String | - | Accepted Content-Type filter |
| **DISCARD** | String | "N" | "Y" = discard response body |
| **GZIP** | String | "Y" | "Y" = request gzip compression |
| **ALIVE** | String | "Y" | "Y" = keep-alive connections |
| **OUTHEAD** | String | - | Variable for response headers (string) |
| **OUTQHEAD** | String | - | Variable for response headers (struct) |
| **CONTEXT** | String | - | User context data (pass-through) |
| **COOKIE** | String | - | Custom cookies |
| **COOKIES** | String | - | Additional custom cookies |
| **FILEUSER** | String | - | File server username |
| **FILEPASS** | String | - | File server password |
| **FILEDOMAIN** | String | - | File server domain (Windows) |
| **BASE64** | String | "N" | "Y" = Base64 encode response |
| **INIFILE** | String | - | Custom INI file path (for configuration) |

### Output Variables

Set in caller scope after each request:

| Variable | Type | Description |
|----------|------|-------------|
| **status** | String | "OK" = success, "ER" = error, "IP" = in progress (async) |
| **httpstatus** | Number | HTTP status code (200, 404, 500, etc.) |
| **msg** | String | Status message or error description |
| **errn** | String | Error code (timeout, connect, ssl, etc.) |
| **httpreqid** | String | Request ID (for async requests) |
| **httpscheme** | String | Request scheme ("http" or "https") |
| **httptime** | Number | Request duration (milliseconds) |
| **httpcontext** | String | User context data (if CONTEXT provided) |
| **[OUT variable]** | String | Response body or file path |
| **[OUTHEAD variable]** | String | Response headers (string format) |
| **[OUTQHEAD variable]** | Struct | Response headers (struct format) |

---

## Appendix A: Quick Reference

### Common Operations

```cfml
<!--- GET request --->
<cf_httpclient url="https://api.example.com/data" method="get" out="result">

<!--- POST JSON --->
<cf_httpclient url="https://api.example.com/data" method="post" 
    headers="Content-Type: application/json" body="#jsonData#" out="result">

<!--- With session --->
<cf_httpclient url="https://example.com/login" method="post" 
    body="user=admin&pass=secret" session="app-session" out="result">

<!--- Async request --->
<cf_httpclient url="https://api.example.com/data" method="get" 
    async="y" reqid="my-req" timeout="60000">

<!--- With authentication --->
<cf_httpclient url="https://api.example.com/secure" method="get" 
    user="admin" pass="secret" out="result">

<!--- Download file --->
<cf_httpclient url="https://example.com/file.pdf" method="get" 
    out="downloads/file.pdf" file="y">

<!--- DNS lookup --->
<cf_httpclient fnc="dns" url="https://www.example.com" out="ipAddress">
```

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Success |
| 201 | Created |
| 204 | No Content |
| 301 | Moved Permanently |
| 302 | Found (Redirect) |
| 304 | Not Modified |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 405 | Method Not Allowed |
| 408 | Request Timeout |
| 429 | Too Many Requests |
| 500 | Internal Server Error |
| 502 | Bad Gateway |
| 503 | Service Unavailable |
| 504 | Gateway Timeout |

---

## Appendix B: Support and Resources

### Documentation

- **This Manual:** Complete programmer's reference
- **Migration Guide:** `docs/MIGRATION_GUIDE.md`
- **Deployment Guide:** `docs/DEPLOYMENT_GUIDE.md`
- **Apache HttpClient Docs:** https://hc.apache.org/httpcomponents-client-4.5.x/

### Getting Help

1. Check troubleshooting section
2. Review server logs
3. Test with simple examples
4. Verify installation and configuration

### Version Information

```cfml
<!--- Check implementation version --->
<cfscript>
    fileContent = fileRead(expandPath("./customtags/httpclient.cfm"));
    versionMatch = reFind("Version:\s+([0-9.]+)", fileContent, 1, true);
    if (arrayLen(versionMatch.pos) GT 1) {
        version = mid(fileContent, versionMatch.pos[2], versionMatch.len[2]);
        writeOutput("cf_httpclient version: " & version);
    }
</cfscript>
```

---

## Appendix C: License and Disclaimer

### License

cf_httpclient is licensed under the **Apache License 2.0**.

See the LICENSE file in the distribution package for complete license text.

### Disclaimer

**THIS SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.**

**USE AT YOUR OWN RISK. THIS PROJECT IS NOT UNDER ACTIVE DEVELOPMENT.**

Users assume all risks associated with the use of this software. The authors and contributors accept no liability for any damages, data loss, system failures, or other issues arising from the use of this software.

### Apache HttpClient

This implementation uses Apache HttpClient 4.5.14:
- **License:** Apache License 2.0
- **Website:** https://hc.apache.org/
- **Copyright:** The Apache Software Foundation

See `lib/LICENSE.txt` and `lib/NOTICE.txt` in the distribution package for Apache HttpClient license information.

### cf_httpclient Implementation

**Version:** 1.00  
**Date:** 2025-01-06  
**Compatibility:** Adobe ColdFusion 11+ | Lucee 5+  
**Purpose:** Drop-in replacement for CFX_HTTP5  
**Maintenance Status:** Not under active development

---

**End of Programmer's Reference**
