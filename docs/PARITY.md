# Parity with the original CFX_HTTP5 DLL (version 2.1.0)

Every function, attribute, output variable and ini setting of Adiabata's CFX_HTTP5, with this tag's status and how
it was checked.

**Status:**
- **Same:** identical to the DLL in the side-by-side test. 53 cases were run through the DLL on Adobe ColdFusion 11
  and through this tag on Lucee 7.1.
- **Tested:** implemented and tested against the manual's rules (this tag only; the DLL wasn't run for these).
- **Differs:** works, with the difference given.
- **Not yet:** possible, not done in this version.
- **Not possible:** can't be done in Java, or not on a normal Windows server.

## Functions

| Function | Status | Notes |
|---|---|---|
| HTTP (blocking) | Same | GET, POST, PUT, DELETE, HEAD |
| HTTP with ASYNC + GET | Same | |
| WGET | Tested | first finished request, REQN, HTTPREQLIST; at most 64 IDs |
| WAIT | Differs | TIMEOUT and INFO work; INFO's LENGTH and BYTES are always 0 (not yet) |
| CANCEL | Differs | IDs become invalid, sessions close; a request already on the wire isn't interrupted (not yet) |
| CLOSE | Same | leaves HTTPSESSION as it was, as the DLL does (the manual says it empties it) |
| DNS | Tested | IP_ADDR, DNS_ADDR; DNS_ADDR is Java's canonical name, not compared with the DLL |

## Output variables

| Variable | Status | Notes |
|---|---|---|
| STATUS, MSG, ERRN | Same | MSG adds "[cfx_http5 java]" and the cause after the WinHTTP-style text |
| HTTPSTATUS, HTTPLENGTH, HTTPBYTES, HTTPSESSION, HTTPREQID, OUT | Same | |
| HTTPSCHEME | Tested | the scheme really used (1/2/8/16), -1 if no login was asked for |
| HTTPCONTEXT, REQN, HTTPREQLIST, HTTPREQREADY, IP_ADDR, DNS_ADDR | Tested | |
| HTTPTIME | Differs | includes the WAIT delay; the DLL excludes queue time |
| OUTHEAD | Differs | sites that use HTTP/2 with the DLL: the DLL shows `HTTP/1.1 200 ` (no reason phrase) without Connection/Transfer-Encoding lines; this tag (HTTP/1.1) shows `HTTP/1.1 200 OK` |
| OUTQHEAD | Differs | lists the headers this tag set; Host, Content-Length and login headers added at send time are missing (not yet) |

## HTTP attributes

| Attribute | Status | Notes |
|---|---|---|
| URL, METHOD | Same | |
| URLDECODE | Tested | N, Y, E |
| HEADERS | Same | add, replace and remove defaults; request header order = WinHTTP's (all 8 compared cases identical). Removing a header that isn't a default is ignored (the manual says that's an error - not yet) |
| REDIRECT | Same | following, POST to GET, cookies set during redirects; REDIRECT="N" tested |
| TIMEOUT | Same | the response wait is at least 90 s, as with WinHTTP. A separate DNS or send timeout isn't applied (not yet) |
| MAXTIME | Differs | limits connecting and the response wait; a server that keeps sending slowly can exceed it (a true total limit: not yet) |
| MAXLEN | Tested | |
| ALIVE | Tested | Y = Keep-Alive, N = Close |
| ACCEPTTYPE | Differs | body dropped as the manual says, but read first (HTTPBYTES shows its size) - not yet |
| WAIT | Tested | |
| BODY, BODYFILE, BODYEND | Same / Tested | sent in that order |
| CHARSETOUT | Tested | |
| PROXYSERVER | Tested | host:port; empty = the Windows default proxy with its bypass list. An `http=` / `https=` prefix in the attribute isn't honoured per scheme (not yet). A proxy reached over TLS (`https://proxy`) isn't tested |
| PROXYUSER, PROXYPASS | Tested | with a test proxy: login, no login (407), wrong login, tunnels; connections aren't shared between different logins |
| USER, PASS | Tested | Basic, Digest (local test server), NTLM and Negotiate (IIS with Windows Authentication); the login is tied to the scheme, host and port |
| SCHEMES 1, 2, 8, 16 | Tested | combinations 9 and 18 too |
| SCHEMES 16 with Kerberos | Not possible | needs a Windows domain; Negotiate uses NTLM, as Windows does without one |
| SCHEMES 4 (Passport) | Not possible | Microsoft retired Passport; accepted, never used |
| FILEUSER, FILEPASS, FILEDOMAIN | Tested (partly) | logon errors tested; acting as the account was not tested inside Lucee |
| SSL 5 | Same | 0, 1, 2 and 5 all give TLS 1.3 + 1.2 (the DLL: 5 = TLS 1.2 only - exact mapping not yet) |
| SSL 1, 2 (SSL 2.0 / 3.0) | Not possible | Java has neither |
| SSL 3, 4 (TLS 1.0 / 1.1) | Not possible | Java refuses them; enabling them is JVM-wide only |
| SSLERRORS | Same | expired, wrong host, self-signed, untrusted root, incomplete chain; strict and OK |
| CERTSTORENAME, CERTSUBJSTR | Tested | Local Machine MY / ROOT, subject match on any part of any line; tested outside Lucee as a normal user. Other stores: not yet (would need a Windows key provider of our own) |
| SESSION, COOKIE | Same | including a session closing on error |
| SESSIONEND | Tested | |
| FILE | Same | no file for an error status (tested) |
| BASE64 | Tested | |
| CHARSETIN and decoding | Same | without a charset: UTF-8 if valid, otherwise ISO-8859-1. A binary body in a variable: the DLL kept the first 7,680 characters of 10,240, this tag keeps all of them |
| GZIP | Same / Tested | 0, 1, 2 |
| CONTEXT, DISCARD | Tested | |
| ASYNC | Same | |
| UTF | Not possible | ColdFusion 5 code-page switch; accepted and ignored |

## cfxhttp5.ini and general behaviour

| Item | Status | Notes |
|---|---|---|
| Keep-Alive, URLDecode | Tested | |
| [HEADERS] Header1..n | Tested | the list ends at the first missing number |
| nThreads | Not yet | the DLL runs at most that many requests at once across the server and queues the rest; this tag has no such limit |
| HTTP/2 | Not yet | needs HttpClient's async engine |
| Sessions keep a Windows login | Tested | NTLM connection; the FILEUSER account (not tested inside Lucee) |
| ini changes | better | re-read when the file changes (the DLL needs a restart) |
| Log guard | extra | see the README's Security section |

## Not yet - list

nThreads limit; HTTP/2; certificate stores other than MY and ROOT; OUTQHEAD exactly as sent; WAIT INFO LENGTH and
BYTES; CANCEL interrupting a running request; an error for removing a non-default header; exact SSL version mapping;
per-scheme `http=` / `https=` entries in PROXYSERVER; ACCEPTTYPE stopping before the body; a true total MAXTIME; DNS and
send timeouts.

## Not compared with the DLL

These behave as the manual describes, but the DLL itself wasn't run for them: error wording and numbers for timeouts,
MAXTIME and refused connections; DNS_ADDR; OUTQHEAD's contents; ALIVE="N" on the wire; ACCEPTTYPE's HTTPBYTES; WAIT
INFO; CANCEL; SSL versions; SCHEMES and HTTPSCHEME values; proxy logins; NTLM / Negotiate; FILEUSER; client
certificates.
