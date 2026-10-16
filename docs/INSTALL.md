# Installing cfx_http5

## Requirements

- **Lucee 6 or 7** (tested: Lucee 7.1.1 under CommandBox 6.2).
- **Java 21 or newer.** On **Java 22 or newer on Windows** the Windows-only features work as well: `FILEUSER`,
  `FILEPASS` and `FILEDOMAIN`, and the Windows default proxy when `PROXYSERVER` is empty. On Java 21, or on another
  operating system, those return an error (`ERRN="50"`) or fall back to no proxy; everything else works. Tested
  with Java 25.
- **Windows** for client certificates (`CERTSTORENAME`), which come from the Windows certificate store.

## Files

| File | What |
|---|---|
| `dist/unicom-cfx.jar` | the tag |
| `dist/httpclient5-5.6.5.jar`, `dist/httpcore5-5.4.4.jar`, `dist/httpcore5-h2-5.4.4.jar` | Apache HttpComponents 5, unchanged |
| `dist/cfxhttp5.ini` | defaults (same format as the DLL's `cfxhttp5.ini`; an existing one works as it is) |

**SLF4J:** HttpClient 5 logs through the SLF4J API, which isn't included. CommandBox provides it. On other servers, if
the tag fails with `NoClassDefFoundError: org/slf4j/...`, add `slf4j-api` (and a binding such as `slf4j-nop`) next to
the other jars. That case hasn't been tested.

## Install on CommandBox Lucee (tested)

1. **Find the folder that holds `lucee.jar`:** the server home's `WEB-INF/lib` (for CommandBox:
   `<CommandBox home>/server/<id>-<server name>/lucee-<version>/WEB-INF/lib`).
2. **Stop the server** (Windows locks loaded jars). **Copy** the four jars and `cfxhttp5.ini` from `dist/` into that
   folder, then **start** the server.
3. **Register** the tag (next section).

> **That folder belongs to the server home.** `server forget`, an engine upgrade or a new server home starts with an
> empty `WEB-INF/lib`, so copy the files again as part of every upgrade. Never leave two versions of a jar in the
> folder: every jar there is loaded.

**What does not work** (tested on Lucee 7.1; each fails with a `NoClassDefFoundError`):
- **CommandBox `app.libDirs` or the JVM class path:** the jars end up in a parent class loader that can't see
  `com.allaire.cfx.CustomTag`.
- **`this.javaSettings` in Application.cfc:** Lucee's OSGi layer can't see `jdk.net`.
- **`lucee-server/context/lib`:** Lucee's class lookup doesn't see that folder.

The Lucee Dev forum thread [Registering Java CFX tags](https://dev.lucee.org/t/registering-java-cfx-tags/8579) reached
the same conclusion.

### Other Lucee installs (not tested)

The same steps apply: copy the jars and `cfxhttp5.ini` into the folder that holds `lucee.jar` (the forum thread found
`lib/ext` for the Windows installer), restart, and register the tag.

## Register the tag

**In the Lucee Server Administrator:** go to *Archives & Resources > CFX Tags > Java CFX tags*. In the empty row
under the list, enter Name `http5` (**without** `cfx_`) and Class `unicom.cfx.Http5Tag`, then click **save**. The tag
works straight away, with no restart, once its jars are loaded. (The Server Library, Procedure and Keep Alive fields on
that page are for C++ tags.)

**Or in `lucee-server/context/.CFConfig.json`**, with the server stopped (back the file up first):

```json
"cfx": {
  "http5": { "type": "java", "class": "unicom.cfx.Http5Tag" }
}
```

> Every Administrator save rewrites the whole `.CFConfig.json` in Lucee's own format: 2-space indentation and the
> operating system's line endings (CRLF on Windows). Scripts that edit the file shouldn't assume either format.
> CFConfig exports include the `cfx` block, but not the jars.

### Adobe ColdFusion (not tested)

1. Copy the jars and `cfxhttp5.ini` into a folder on ColdFusion's class path, such as `cfusion/wwwroot/WEB-INF/lib`.
2. Restart ColdFusion.
3. In the ColdFusion Administrator, go to *Extensions > CFX Tags > Register Java CFX* and register tag name
   `cfx_http5` with class `unicom.cfx.Http5Tag`.

Unregister the DLL tag first, because the name is the same. Recent ColdFusion versions ship their own Apache
HttpComponents jars, so version clashes are possible.

## Quick test

```cfml
<cfx_http5 url="https://www.example.com/" method="get" out="body" outhead="head">
<cfoutput>#STATUS# #HTTPSTATUS# - #len(body)# characters</cfoutput>
```

Expected: `OK 200 - ...`.

## Java 22+: native access warning

The Windows-only features call Windows directly through Java's Foreign Function & Memory API. The first time they're
used, Java writes a one-time warning to the server's console log ("A restricted method in java.lang.foreign... has
been called"). It is harmless. To silence it, add `--enable-native-access=ALL-UNNAMED` to the server's JVM arguments.
A future Java release may block such calls unless that option is set; the features would then return an error, and
everything else keeps working.

## Client certificates (Windows)

Put the certificate with its private key in the **Local Machine** store `MY` (Personal) and make sure the account the
server runs as can use the key. On Windows' default settings the SYSTEM account can. Then pass
`CERTSTORENAME="MY"` and part of the certificate's subject in `CERTSUBJSTR`. The stores `MY` and `ROOT` can be used
(see [PARITY.md](PARITY.md)).

## Build from source

You need a **JDK 22 or newer**, your engine's `lucee.jar` (for the `com.allaire.cfx` interfaces) and an `slf4j-api`
jar (Maven Central `org.slf4j:slf4j-api`, 1.7.x or 2.x). Neither jar is shipped with the result.

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1 -LuceeJar <path>\lucee.jar -Slf4jApi <path>\slf4j-api.jar -Jdk <JDK folder>
```

```sh
./build.sh /path/to/lucee.jar /path/to/slf4j-api.jar      # Linux / macOS (not tested)
```

The result is `build/unicom-cfx.jar`. `src/` is compiled for Java 21 and `src22/` (the Windows calls) for Java 22, so
the jar loads on Java 21. Built with JDK 25, the published source compiles to class files identical to those in
`dist/unicom-cfx.jar`.
