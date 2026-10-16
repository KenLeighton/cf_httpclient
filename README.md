# cfx_http5 for Lucee (Java CFX tag)

A Java replacement for **CFX_HTTP5** by Adiabata, Inc. The original is a Windows C++ DLL (built on WinHTTP) that only
Adobe ColdFusion can load. This is a **Java CFX tag** registered under the same name, so existing code that calls
`<cfx_http5>` runs on Lucee unchanged.

| Tag | Class | Built on |
|---|---|---|
| `cfx_http5` | `unicom.cfx.Http5Tag` | Apache HttpClient 5.6.5 / HttpCore 5.4.4 (classic API) |

**Version 2.1.0** (October 16, 2026). `dist/unicom-cfx.jar` SHA256
`8B19C49296330CBA529B45C0B15F63D73BAEFDAD2554258E97649B00B2A1E2B9`. See [docs/CHANGES.md](docs/CHANGES.md).

## Documentation

| Document | What's in it |
|---|---|
| [docs/INSTALL.md](docs/INSTALL.md) | Requirements, installing the jars, registering the tag, a quick test, building from source |
| [docs/REFERENCE.md](docs/REFERENCE.md) | Every function, parameter and output variable, the ini file and the error numbers |
| [docs/PARITY.md](docs/PARITY.md) | Item-by-item comparison with the original DLL: what matches, what differs, what isn't done yet, and how each item was tested |
| [docs/CHANGES.md](docs/CHANGES.md) | Version history |

## Status

- **Tested** on Lucee 7.1.1 under CommandBox 6.2 with Java 25, on Windows 11 and Windows Server 2025.
- **Compared with the original DLL:** 53 test cases were run through the DLL on Adobe ColdFusion 11 and through
  this tag on Lucee. The differences that remain are explained in [docs/PARITY.md](docs/PARITY.md). Most are text
  differences in the response-header variable on sites that use HTTP/2, plus content changes on live sites between
  the two runs.
- **Tested against the CFX_HTTP5 manual's rules:** over 100 further checks, covering every parameter the comparison
  didn't. They use local test servers, Windows Authentication on IIS, public test sites for client certificates, and a
  test proxy with throwaway logins.
- **All 7 functions** and every output variable are implemented. All HTTP parameters are implemented except `UTF`
  (a ColdFusion 5-era switch, accepted and ignored). [docs/PARITY.md](docs/PARITY.md) lists what is not possible in
  Java or not done yet (for example HTTP/2 and the `nThreads` limit).
- **Not tested:** Adobe ColdFusion as the host engine, the Lucee installer (Tomcat), Linux and macOS, and BoxLang.
  Also not tested: running `FILEUSER` (another Windows account) and client certificates inside Lucee as the service
  account. Both were tested outside Lucee.

## Why a Java CFX tag?

- **No code changes:** the tag keeps the DLL's name, attributes and output variables, and is registered once per
  server, as the DLL was.
- **HttpClient 5 needs `jdk.net`:** Lucee's per-application jars (`this.javaSettings`) are loaded under its OSGi core,
  which can't see that package. A Java CFX tag is loaded next to `lucee.jar`, which can.

## Security

- **Log guard:** at DEBUG level, HttpClient 5 writes every request and response to the log, including logins, cookies
  and bodies. Before every request the tag checks HttpClient's loggers. If one is at DEBUG it turns them down. If
  that isn't possible, the tag **refuses to send the request** (`STATUS="ER"`, `ERRN="5"`). There is no switch to turn
  this off.
- **Logins stay with their server:** a login (`USER`/`PASS`) is used only for the exact scheme, host and port of the
  request. Pooled connections are shared only between calls with the same logins.
- **`SSLERRORS="OK"`** turns off certificate and host-name checks for that request. Anyone who can intercept the
  traffic could then read or change it. **`http://` URLs** are sent in clear text.

## Credits and licenses

- **CFX_HTTP5** is a product of Adiabata, Inc. This tag reimplements its documented interface (attribute names and
  output variables) so that existing code keeps working. It contains no Adiabata code, its documentation is written
  independently, and this project is not affiliated with or endorsed by Adiabata.
- **Apache HttpComponents** (HttpClient 5.6.5, HttpCore 5.4.4) is licensed under the Apache License 2.0. The jars in
  `dist/` are unchanged; see [`licenses/`](licenses/).
- **`com.allaire.cfx`** (the CFX interfaces) comes from the CFML engine at run time, and **SLF4J** (MIT License) from
  the server. Neither is included.
- **This code** is licensed under the Apache License 2.0; see [LICENSE](LICENSE).

**Disclaimer:** provided "as is", without warranty of any kind. Test it with your own code before you rely on it.
