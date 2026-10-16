# cfx_http5 reference

This reference describes the tag as implemented in this repository. It follows the interface of Adiabata's CFX_HTTP5
(same attribute and variable names) so existing code keeps working; where this tag behaves differently from the
DLL, the difference is noted here and in [PARITY.md](PARITY.md).

## Conventions

- Every attribute is a string. **An empty attribute counts as omitted.** Leading and trailing blanks are removed from
  every attribute except `BODY`, `BODYEND` and `HEADERS`.
- `FNC` chooses the function: `HTTP` (default), `GET`, `WGET`, `WAIT`, `CANCEL`, `CLOSE` or `DNS`. The older name
  `REQ` is accepted for `FNC`.
- Results are set as variables in the calling page. Every function sets **`STATUS`**: `OK` or `ER`. When `STATUS` is
  `ER`, **`ERRN`** holds an error number (see [Error numbers](#error-numbers)) and **`MSG`** the error text. `MSG`
  starts with the WinHTTP-style text, followed by `[cfx_http5 java]` and the underlying cause, and ends with CRLF.

```cfml
<cfx_http5 url="https://www.example.com/data.xml" out="result">
<cfif STATUS eq "ER">
    <cfoutput>Error #ERRN#: #MSG#</cfoutput>
<cfelse>
    <cfoutput>HTTP #HTTPSTATUS#, #len(result)# characters</cfoutput>
</cfif>
```

## FNC="HTTP" - send a request

### Request

| Attribute | Meaning |
|---|---|
| `URL` | Full URL including `http://` or `https://`. Default ports are 80 and 443. Characters that aren't allowed in a URL (spaces, `\|` ...) are escaped; existing `%xx` escapes are kept. |
| `URLDECODE` | `N` = use the URL as given, `Y` = decode `%xx` escapes first (illegal characters are then escaped again), `E` = escape every `%` (so `%41` is sent as `%2541`). Default: the ini's `URLDecode`. |
| `METHOD` | `GET` (default), `POST`, `PUT`, `DELETE`, `HEAD` (others such as `PATCH` are passed through). |
| `HEADERS` | Extra request headers, one `Name: value` per line, each line ending in CRLF. A header with the same name as an ini default replaces it; `Name:` with no value removes that default header. |
| `REDIRECT` | Redirects (301, 302, 303, 307, 308) are followed automatically, up to 10. `N` = don't follow (the 30x response is returned). A redirect from https to http is only followed with `REDIRECT="Y"`. After a 303, or a 301/302 for a POST, the next request is a GET without a body. |
| `TIMEOUT` | Milliseconds (default 45000) for connecting. The wait for the response is at least 90 seconds, whatever `TIMEOUT` says (as with WinHTTP). |
| `MAXTIME` | Milliseconds: an upper limit for connecting and for the response wait. A request that hits it ends with `ERRN="12002"`. |
| `MAXLEN` | Bytes: keep at most this much of the response body (no error). |
| `ALIVE` | `Y` = keep the connection open for reuse (`Connection: Keep-Alive`), `N` = close it after the response (`Connection: close`). Default: the ini's `Keep-Alive`. NTLM needs keep-alive. |
| `ACCEPTTYPE` | Comma-separated content types (`text/html,text/plain`). On a 200 response with another type, the body is dropped: an empty variable, or no file. The other variables are set as usual. |
| `WAIT` | Milliseconds to wait before sending. `HTTPTIME` includes this wait. |
| `CONTEXT` | Any integer, returned as `HTTPCONTEXT` (and in `WAIT`'s `INFO`). Default 0. |

### Request body (`POST`, `PUT`, `PATCH` only)

| Attribute | Meaning |
|---|---|
| `BODY` | Text sent first. |
| `BODYFILE` | Full path of a file whose bytes are sent after `BODY`. |
| `BODYEND` | Text sent after the file. |
| `CHARSETOUT` | Character set for `BODY` and `BODYEND` (for example `windows-1252`). Default UTF-8. |

`Content-Length` is added automatically.

### Proxy

| Attribute | Meaning |
|---|---|
| `PROXYSERVER` | `host`, `host:port`, `http://host:port` or `http=host:port` (default port 80; in this attribute an `http=` / `https=` prefix is ignored and the proxy is used for every URL). **Empty = the Windows default proxy**, which is the machine-wide setting made with `netsh winhttp set proxy`, not the browser's. It honours its bypass list (`<local>` and `*` wildcards). In a session, the proxy given when the session started is used for the whole session. |
| `PROXYUSER`, `PROXYPASS` | Proxy login (Basic, Digest or NTLM). Pass them on every request, sessions included. |

### Web-site login

| Attribute | Meaning |
|---|---|
| `USER`, `PASS` | Login for a site that asks for one (HTTP 401). `DOMAIN\user` is accepted. The login is used only for the request's exact scheme, host and port: a redirect to another host, another port, or from https to http doesn't carry it. |
| `SCHEMES` | Which schemes may be used: the sum of 1 Basic, 2 NTLM, 4 Passport, 8 Digest, 16 Negotiate. 0 or omitted = any, strongest first (Negotiate, NTLM, Digest, Basic). Passport is accepted but never used (Microsoft retired it). **Negotiate** sends NTLM messages under the Negotiate name, which is what Windows does when Kerberos isn't available; Kerberos itself isn't supported. |

### Windows account (Windows, Java 22+)

| Attribute | Meaning |
|---|---|
| `FILEUSER`, `FILEPASS`, `FILEDOMAIN` | A Windows account used to read `BODYFILE`, to write the `FILE` output, and to open the client certificate. `FILEDOMAIN="."` = a local account. Only those steps run as that account, and the tag always switches back afterwards. In a session, the account given when the session started is kept until the session closes. A failed logon returns Windows' own error number and text, such as `1326` "The user name or password is incorrect." |

### TLS (https)

| Attribute | Meaning |
|---|---|
| `SSL` | `0`, `1`, `2`, `5` or omitted = TLS 1.3 and 1.2. `3` = TLS 1.0, `4` = TLS 1.1. Java refuses TLS 1.0 and 1.1 by default, so those fail with `ERRN="12175"`. SSL 2 and SSL 3 don't exist in Java. |
| `SSLERRORS` | `OK` = accept any certificate and host name (expired, self-signed, wrong host...). Otherwise certificates are checked against Java's root certificates plus, on Windows, the Windows root stores. A missing intermediate certificate is downloaded from the certificate's AIA address, as Windows does. |
| `CERTSTORENAME`, `CERTSUBJSTR` | Client certificate (Windows): the **Local Machine** store `MY` or `ROOT`, and any part of any line of the certificate's subject (case doesn't matter). The first certificate with a private key that matches is used. Another store name gives `ERRN="87"`; no match gives `ERRN="12044"`. |

### Sessions

| Attribute | Meaning |
|---|---|
| `SESSION` | `START` = begin a new session (its ID is returned in `HTTPSESSION`); an ID = continue that session. A session keeps its cookies, its proxy, its connections (so an NTLM login holds) and its `FILEUSER` account. |
| `SESSIONEND` | `Y` = close the session after this request. |
| `COOKIE` | `Y` = handle cookies for a request outside a session (redirects that set cookies). Inside a session, cookies are always handled. |

A session closes with `SESSIONEND="Y"`, `FNC="CLOSE"`, `FNC="CANCEL"` of one of its requests, or **any error** (an
HTTP error status such as 404 is not an error). An unused session is removed after two hours.

### Output

| Attribute | Meaning |
|---|---|
| `OUT` | Name of the variable for the response body; with `FILE="Y"`, the full path of the file to write. Without `OUT`, only the headers are read. |
| `FILE` | `Y` = write the body to the `OUT` file (replaced if it exists), only for a 2xx response with a body. |
| `OUTHEAD` | Variable for the response headers: the status line, then one header per line, CRLF separated, ending with an empty line. |
| `OUTQHEAD` | Variable for the request headers this tag set (the request line and headers; headers that HttpClient adds at send time, such as Host and Content-Length, are not listed). |
| `BASE64` | `Y` = return the body Base64-encoded (for binary data in a variable). |
| `CHARSETIN` | Character set for decoding the body. Default: the `charset` of the response's Content-Type. Without one, the body is read as UTF-8 if it is valid UTF-8, otherwise as ISO-8859-1 (one character per byte). Zero bytes become blanks. |
| `GZIP` | `0` = never decompress, `1` = always, `2` = when the response says `Content-Encoding: gzip`. Default: `2` if the request sends `Accept-Encoding: gzip` (as the ini's defaults do), otherwise `0`. |

### Execution mode

| Attribute | Meaning |
|---|---|
| `ASYNC` | `Y` = start the request and return at once with `HTTPREQID`; collect the result with `GET` or `WGET`. Every request ID must be collected or cancelled exactly once. |
| `DISCARD` | `Y` = run the request detached: nothing is returned, and a session it belongs to closes when it finishes. Useful with `FILE="Y"`. |
| `UTF` | Accepted and ignored (a ColdFusion 5-era code-page switch; Lucee is always Unicode). |

### Variables set by HTTP (and by GET / WGET)

| Variable | Value |
|---|---|
| `HTTPSTATUS` | The HTTP status code of the final response. |
| `HTTPSCHEME` | The login scheme that answered the site's challenge: 1 Basic, 2 NTLM, 8 Digest, 16 Negotiate; `-1` if the site didn't ask for a login. |
| `HTTPSESSION` | The session ID, or empty when there's no session (or it closed). Always set, also after an error. |
| `HTTPLENGTH` | The response's Content-Length, or `-1`. |
| `HTTPBYTES` | The number of body bytes returned (after decompression; less with `MAXLEN`). |
| `HTTPTIME` | Milliseconds the request took. |
| `HTTPCONTEXT` | The `CONTEXT` value; also set after an error. |
| `HTTPREQID` | With `ASYNC="Y"`: the request ID. |
| *OUT*, *OUTHEAD*, *OUTQHEAD* | The variables named by those attributes. |

The order of the request headers follows WinHTTP's: general headers first (Cache-Control, Connection, Pragma, ...),
then Content-Type, then the other headers by name, then Content-Length and Host. Responses come over HTTP/1.1, so
`OUTHEAD`'s status line includes the reason phrase (`HTTP/1.1 200 OK`).

## FNC="GET" and FNC="WGET" - results of asynchronous requests

| Attribute | Meaning |
|---|---|
| `REQID` | `GET`: one request ID; waits until it finishes. `WGET`: a comma-separated list of up to 64 IDs; waits until the first of them finishes. |

Both set the variables of the original `HTTP` call. `WGET` also sets `HTTPREQID` (the request that finished), `REQN`
(its 1-based position in `REQID`) and `HTTPREQLIST` (the IDs still running). A request ID can be used only once.

## FNC="WAIT" - wait for asynchronous requests

| Attribute | Meaning |
|---|---|
| `REQID` | Up to 64 request IDs, comma separated. |
| `TIMEOUT` | Milliseconds to wait for at least one of them to finish. `0` = just check; negative or omitted = wait as long as it takes. |
| `INFO` | Optional variable name: for each request still running, `CONTEXT,LENGTH,BYTES`, joined with `~` (LENGTH and BYTES are 0 in this version). |

Sets `HTTPREQREADY` (finished IDs) and `HTTPREQLIST` (IDs still running). Collect each finished request with `GET`.

## FNC="CANCEL" - give up asynchronous requests

`REQID`: up to 64 request IDs. Their results are dropped, the IDs become invalid, and a session they belong to closes.
(A request already on the wire runs to its end in the background.)

## FNC="CLOSE" - close a session

`SESSION`: the session ID. `HTTPSESSION` is left as it was, as the DLL does.

## FNC="DNS" - look up a host

`ADDRESS` (or `URL`): a host name or IP address. Sets `IP_ADDR` (the address) and `DNS_ADDR` (Java's canonical host
name for it). An unknown host gives `ERRN="12007"`.

## cfxhttp5.ini

The tag reads `cfxhttp5.ini` from the folder its `unicom-cfx.jar` was loaded from, or the file named by the JVM option
`-Dunicom.cfxhttp5.ini=<path>`. It re-reads the file when it changes. Without a file, built-in defaults are used (the
same as `dist/cfxhttp5.ini`).

```ini
[SERVER]
Keep-Alive=Y          ; default for ALIVE
URLDecode=N           ; default for URLDECODE

[HEADERS]
Header1=Accept: */*   ; default request headers, Header1, Header2, ... - the list ends at the first missing number
Header2=Accept-Encoding: gzip
```

`nThreads` (the DLL's limit on simultaneous requests) and `UTF` are read but not applied.

## Error numbers

| ERRN | Meaning |
|---|---|
| 5 | Access is denied - the log guard refused to send (see the README's Security section) |
| 6 | The handle is invalid - an unknown session or request ID |
| 50 | The request is not supported - a Windows-only feature on Java 21 or on another operating system |
| 87 | The parameter is incorrect - missing `URL` / `REQID`, an unknown `FNC`, more than 64 request IDs, an unusable certificate store |
| 1326 and other Windows numbers | `FILEUSER` logon errors, with Windows' own text |
| 12002 | The operation timed out |
| 12005 | The URL is invalid |
| 12007 | The server name or address could not be resolved |
| 12029 | A connection with the server could not be established |
| 12030 | The connection with the server was terminated abnormally (also the general fallback) |
| 12044 | A certificate is required - no matching client certificate |
| 12175 | A security error occurred - certificate or TLS problems |

## Examples

**Form POST, response to a file:**

```cfml
<cfx_http5 url="https://www.example.com/report" method="post"
           body="from=2026-01-01&to=2026-12-31" out="C:\reports\report.pdf" file="Y">
```

**A session with a login page:**

```cfml
<cfx_http5 url="https://portal.example.com/login" method="post" body="#loginForm#"
           session="start" out="page">
<cfset sid = HTTPSESSION>
<cfx_http5 url="https://portal.example.com/list" session="#sid#" sessionend="Y" out="page">
```

**Three requests at once, fastest first:**

```cfml
<cfset ids = "">
<cfloop list="a,b,c" index="p">
    <cfx_http5 url="https://www.example.com/#p#" async="Y" out="r">
    <cfset ids = listAppend(ids, HTTPREQID)>
</cfloop>
<cfloop condition="len(ids)">
    <cfx_http5 fnc="WGET" reqid="#ids#">
    <cfset ids = HTTPREQLIST>
    <!--- use r, HTTPSTATUS ... --->
</cfloop>
```
