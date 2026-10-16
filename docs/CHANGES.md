# Changes

## 2.1.0 - October 16, 2026

- Client certificates from the Windows Local Machine store (`CERTSTORENAME`, `CERTSUBJSTR`).
- `SCHEMES` with NTLM and Negotiate (NTLM messages) besides Basic and Digest. `HTTPSCHEME` now reports the scheme
  really used (it used to report 1 whenever `USER` was given).
- `FILEUSER` / `FILEPASS` / `FILEDOMAIN` (Windows, Java 22+).
- An empty `PROXYSERVER` uses the Windows default proxy (Java 22+ on Windows).
- Request headers are sent in WinHTTP's order.
- Pooled connections are shared only between calls with the same logins, so a call without a proxy login can't use a
  tunnel opened with one. A web-site login is used only for its exact scheme, host and port.
- `ALIVE="N"` (and `Keep-Alive=N` in the ini) closes the connection.
- The ini's `[HEADERS]` list ends at the first missing `HeaderN`.
- WGET, WAIT and CANCEL refuse lists of more than 64 request IDs (`ERRN="87"`).
- Built in two parts: the Windows calls (`src22/`) need Java 22; the rest still loads on Java 21.

## 2.0.0 - October 15, 2026

- Removed the Java version of `cfx_exec`; use CFML's built-in `cfexecute`.

## 1.0.1 - October 15, 2026

- A response without a charset is read as UTF-8 when the bytes are valid UTF-8, otherwise as ISO-8859-1, as the DLL
  does.

## 1.0.0 - October 15, 2026

- First release: `cfx_http5` as a Java CFX tag on Apache HttpClient 5, with the log guard.

## About this repository

The repository's history was restarted on October 16, 2026. It used to hold a CFML custom tag (`cf_httpclient`, on
Apache HttpClient 4.5) whose documentation claimed full compatibility with CFX_HTTP5. Testing showed that claim was
wrong, so that tag has been withdrawn.
