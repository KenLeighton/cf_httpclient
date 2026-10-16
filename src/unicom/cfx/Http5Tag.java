package unicom.cfx;

import com.allaire.cfx.CustomTag;
import com.allaire.cfx.Request;
import com.allaire.cfx.Response;
import org.apache.hc.client5.http.auth.AuthChallenge;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.apache.hc.client5.http.auth.AuthSchemeFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.AuthenticationException;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.MalformedChallengeException;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.auth.DigestScheme;
import org.apache.hc.client5.http.impl.auth.NTLMScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.ProtocolVersion;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * cfx_http5 - drop-in replacement for the Adiabata CFX_HTTP5 C++ tag (Windows WinHTTP), as a Lucee Java CFX tag on
 * Apache HttpClient 5.x (classic API). Version 1.0.0, October 15, 2026 (release 1.0.0-dev2 adds the log guard;
 * 1.0.0-dev3 decodes bodies without a charset the way CFX_HTTP5 does - see decodeUnnamed). Release 1.0.0-dev5,
 * October 16, 2026: client certificates (CERTSTORENAME, CERTSUBJSTR), SCHEMES with NTLM and Negotiate and the real
 * HTTPSCHEME, FILEUSER / FILEPASS / FILEDOMAIN, the WinHTTP default proxy when PROXYSERVER is empty, WinHTTP's header
 * order, and pooled connections shared only between calls with the same logins.
 *
 * A Java port of an earlier CFML version of this tag (not published), which served as the specification; behaviour
 * and the known differences from the DLL are documented in docs/REFERENCE.md and docs/PARITY.md. It runs as a Java CFX tag
 * because Lucee's javaSettings class loader cannot see jdk.net.* (Apache 5 needs jdk.net.Sockets).
 *
 * Register in Lucee (Java CFX tags): name "http5", class unicom.cfx.Http5Tag. Jars on the server class path
 * (WEB-INF\lib): unicom-cfx.jar, httpclient5-5.6.5, httpcore5-5.4.4, httpcore5-h2-5.4.4 (SLF4J comes with CommandBox).
 * Defaults come from cfxhttp5.ini next to unicom-cfx.jar (same format as CFX_HTTP5; built-in defaults if it is missing).
 */
public class Http5Tag implements CustomTag {
    static final String CRLF = "\r\n";

    // ---- engine state: one per class loader, i.e. per Lucee server ----
    private static final ConcurrentHashMap<String, CloseableHttpClient> CLIENTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Sess> SESSIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AsyncReq> REQUESTS = new ConcurrentHashMap<>();
    private static final Object INI_LOCK = new Object();
    private static volatile String iniKey = "";
    private static volatile Ini ini;
    private static volatile KeyStore trust;
    private static volatile long lastSweep = System.currentTimeMillis();
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "cfx_http5-async");
        t.setDaemon(true);
        return t;
    });

    static final class Sess {
        final HttpClientContext context;
        final String proxy;
        volatile long used = System.currentTimeMillis();
        /** FILEUSER logon of the call that started the session (0 = none); closed with the session */
        volatile long logon;
        volatile String logonUser = "";
        Sess(HttpClientContext context, String proxy) { this.context = context; this.proxy = proxy; }
    }

    static final class AsyncReq {
        final Attrs attrs;
        final boolean discard;
        final String context;
        volatile Result result;
        AsyncReq(Attrs attrs, boolean discard, String context) { this.attrs = attrs; this.discard = discard; this.context = context; }
    }

    static final class Result {
        String status = "OK", msg = "", errn = "", httpstatus = "", httpsession = "", httplength = "-1", httpbytes = "0",
               httptime = "0", httpcontext = "0", httpscheme = "-1", head = "", qhead = "", body = "";
        boolean hasBody;
    }

    static final class Ini {
        boolean keepAlive = true;
        String urlDecode = "N";
        final List<String[]> headers = new ArrayList<>();
    }

    /** An error with a CFX_HTTP5 error class ("session" -> 6, "param" -> 87). */
    static final class CfxError extends Exception {
        private static final long serialVersionUID = 1L;
        final String type;
        CfxError(String type, String message) { super(message); this.type = type; }
    }

    interface Out { void set(String name, String value); }

    /** Windows calls, implemented by Win32 (Java 22+ FFM; loaded by reflection so the tag still runs on Java 21). */
    interface Native {
        /** WinHTTP's default proxy: { access type (1 none, 3 named), proxy list, bypass list } */
        String[] defaultProxy() throws Throwable;
        long logon(String user, String domain, char[] password) throws Throwable;
        void impersonate(long token) throws Throwable;
        void revert() throws Throwable;
        void close(long token);
    }

    /** A Windows error: ERRN is its code, MSG the system's text for it (as CFX_HTTP5 reports them). */
    static final class NativeError extends Exception {
        private static final long serialVersionUID = 1L;
        final int code;
        NativeError(int code, String message) { super(message); this.code = code; }
    }

    private static volatile Native nat;
    private static volatile String natFailure;

    static Native nat() throws CfxError {
        Native n = nat;
        if (n != null) { return n; }
        synchronized (INI_LOCK) {
            if (nat != null) { return nat; }
            if (natFailure == null) {
                try {
                    nat = (Native) Class.forName("unicom.cfx.Win32", true, Http5Tag.class.getClassLoader()).getDeclaredConstructor().newInstance();
                    return nat;
                } catch (Throwable t) {
                    Throwable c = t.getCause() != null ? t.getCause() : t;
                    natFailure = c.getClass().getSimpleName() + ": " + c.getMessage();
                }
            }
            throw new CfxError("native", "Windows calls are not available (they need Java 22 or later on Windows): " + natFailure);
        }
    }

    // =====================================================================================================

    @Override
    public void processRequest(Request request, Response response) throws Exception {
        Attrs a = Attrs.of(request);
        Out out = (n, v) -> response.setVariable(n, v == null ? "" : v);
        String fnc = a.get("fnc", a.get("req", "HTTP")).toUpperCase(Locale.ROOT);
        if (fnc.isEmpty()) { fnc = "HTTP"; }
        switch (fnc) {
            case "HTTP":
                if (a.yes("async") || a.yes("discard")) { startAsync(a, out); } else { applyResult(doRequest(a), a, out); }
                break;
            case "GET":
            case "WGET":
                doGetResult(fnc, a, out);
                break;
            case "WAIT":
                doWait(a, out);
                break;
            case "CANCEL":
                doCancel(a, out);
                break;
            case "CLOSE":
                // CFX_HTTP5 leaves HTTPSESSION as it was (side-by-side test, Oct 15, 2026)
                closeSession(a.get("session"));
                out.set("STATUS", "OK");
                break;
            case "DNS":
                doDns(a, out);
                break;
            default:
                out.set("STATUS", "ER");
                out.set("ERRN", "87");
                out.set("MSG", "Unknown FNC value: " + fnc + CRLF);
        }
    }

    // ---- log guard (release 1.0.0-dev2) ----
    // At DEBUG, Apache HttpClient 5 writes every request and response - headers (Authorization, cookies) and bodies -
    // to the server's log, and CommandBox's TRACE mode turns DEBUG on for every logger. So before each request: if any
    // HttpClient 5 logger is at DEBUG, set org.apache.hc to WARN (Log4j 2, which CommandBox uses); if one is still at
    // DEBUG after that (some other logging setup), refuse to send. Deliberately there is no switch to turn this off.

    private static final Object LOG_LOCK = new Object();
    private static volatile Logger[] hcLogs;

    /** Every logger Apache HttpClient 5 can write to: the fixed names plus what a scan of its jars finds. */
    static Logger[] hcLoggers() {
        Logger[] l = hcLogs;
        if (l != null) { return l; }
        synchronized (LOG_LOCK) {
            if (hcLogs != null) { return hcLogs; }
            Set<String> names = new TreeSet<>(Arrays.asList("org.apache.hc", "org.apache.hc.client5", "org.apache.hc.core5",
                "org.apache.hc.client5.http.wire", "org.apache.hc.client5.http.headers"));
            for (String k : new String[] { "org.apache.hc.client5.http.impl.classic.HttpClients",
                                           "org.apache.hc.core5.http.HttpHost", "org.apache.hc.core5.http2.HttpVersionPolicy" }) {
                scanJar(k, names);
            }
            List<Logger> list = new ArrayList<>();
            for (String n : names) { list.add(LoggerFactory.getLogger(n)); }
            hcLogs = list.toArray(new Logger[0]);
            return hcLogs;
        }
    }

    /** Adds the classes in className's jar that call LoggerFactory, and the org.apache.hc names they use. */
    static void scanJar(String className, Set<String> names) {
        try {
            Class<?> k = Class.forName(className, false, Http5Tag.class.getClassLoader());
            File f = new File(k.getProtectionDomain().getCodeSource().getLocation().toURI());
            Pattern p = Pattern.compile("org\\.apache\\.hc\\.[A-Za-z0-9.]*[A-Za-z0-9]");
            try (JarFile jar = new JarFile(f)) {
                Enumeration<JarEntry> en = jar.entries();
                while (en.hasMoreElements()) {
                    JarEntry e = en.nextElement();
                    if (!e.getName().endsWith(".class")) { continue; }
                    String s;
                    try (InputStream in = jar.getInputStream(e)) { s = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1); }
                    if (!s.contains("org/slf4j/LoggerFactory")) { continue; }
                    String n = e.getName().substring(0, e.getName().length() - 6).replace('/', '.');
                    int d = n.indexOf('$');
                    names.add(d > 0 ? n.substring(0, d) : n);
                    Matcher m = p.matcher(s);
                    while (m.find()) { names.add(m.group()); }
                }
            }
        } catch (Throwable ignored) {
            // the fixed names are still checked (org.apache.hc is the parent of all of them)
        }
    }

    static boolean hcDebugOn() {
        for (Logger l : hcLoggers()) { if (l.isDebugEnabled()) { return true; } }   // TRACE counts as DEBUG
        return false;
    }

    static void quietLogging() throws CfxError {
        if (!hcDebugOn()) { return; }
        synchronized (LOG_LOCK) {
            try {
                ClassLoader cl = Http5Tag.class.getClassLoader();
                Class<?> level = Class.forName("org.apache.logging.log4j.Level", true, cl);
                Class<?> conf = Class.forName("org.apache.logging.log4j.core.config.Configurator", true, cl);
                conf.getMethod("setAllLevels", String.class, level).invoke(null, "org.apache.hc", level.getField("WARN").get(null));
            } catch (Throwable ignored) {
                // not Log4j 2 - the check below decides
            }
        }
        if (hcDebugOn()) {
            throw new CfxError("logging", "The request was NOT sent: debug logging is on for Apache HttpClient 5, which would "
                + "write its headers (passwords, cookies) and body to the server log. Set the org.apache.hc loggers to INFO or higher");
        }
    }

    // ---- defaults (cfxhttp5.ini) ----

    static File iniFile() {
        String p = System.getProperty("unicom.cfxhttp5.ini");
        if (p != null && !p.isEmpty()) { return new File(p); }
        try {
            File jar = new File(Http5Tag.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return new File(jar.getParentFile(), "cfxhttp5.ini");
        } catch (Exception e) {
            return new File("cfxhttp5.ini");
        }
    }

    static Ini loadIni() throws Exception {
        File f = iniFile();
        String key = f.getPath() + "|" + (f.isFile() ? f.lastModified() : "builtin");
        Ini cur = ini;
        if (cur != null && key.equals(iniKey)) { return cur; }
        synchronized (INI_LOCK) {
            if (ini != null && key.equals(iniKey)) { return ini; }
            List<String> lines = new ArrayList<>();
            if (f.isFile()) {
                for (String l : new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).replace("\r", "").split("\n")) {
                    if (!l.isEmpty()) { lines.add(l); }
                }
            } else {
                lines.add("[SERVER]"); lines.add("Keep-Alive=Y"); lines.add("URLDecode=N"); lines.add("[HEADERS]");
                lines.add("Header1=Accept: image/gif, image/x-xbitmap, image/jpeg, image/pjpeg, application/vnd.ms-excel, application/msword, application/vnd.ms-powerpoint, */*");
                lines.add("Header2=Accept-Language: en-us");
                lines.add("Header3=Content-Type: application/x-www-form-urlencoded");
                lines.add("Header4=User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36");
                lines.add("Header5=Pragma: no-cache");
                lines.add("Header6=Cache-Control: no-cache");
                lines.add("Header7=Accept-Encoding: gzip");
            }
            Ini cfg = new Ini();
            String section = "";
            Map<Integer, String[]> numbered = new java.util.TreeMap<>();
            for (String raw : lines) {
                String l = raw.trim();
                if (l.isEmpty() || l.startsWith(";")) { continue; }
                if (l.startsWith("[")) { section = l.replaceAll("[\\[\\]]", "").toUpperCase(Locale.ROOT); continue; }
                int eq = l.indexOf('=');
                if (eq < 1) { continue; }
                String k = l.substring(0, eq).trim();
                String v = l.substring(eq + 1).trim();
                if (section.equals("SERVER")) {
                    if (k.equalsIgnoreCase("Keep-Alive")) { cfg.keepAlive = v.equalsIgnoreCase("Y"); }
                    else if (k.equalsIgnoreCase("URLDecode")) { cfg.urlDecode = v.toUpperCase(Locale.ROOT); }
                } else if (section.equals("HEADERS")) {
                    Matcher hn = Pattern.compile("(?i)^Header(\\d+)$").matcher(k);
                    int c = v.indexOf(':');
                    if (hn.matches() && c > 0) { numbered.put(Integer.valueOf(hn.group(1)), new String[] { v.substring(0, c).trim(), v.substring(c + 1).trim() }); }
                }
            }
            // as CFX_HTTP5: Header1, Header2, ... in sequence - the list ends at the first missing number
            for (int n = 1; numbered.containsKey(n); n++) { cfg.headers.add(numbered.get(n)); }
            ini = cfg;
            iniKey = key;
            return cfg;
        }
    }

    // ---- SSL: Java's roots + the Windows root stores ----

    static KeyStore trustStore() throws Exception {
        KeyStore t = trust;
        if (t != null) { return t; }
        synchronized (CLIENTS) {
            if (trust != null) { return trust; }
            KeyStore merged = KeyStore.getInstance("PKCS12");
            merged.load(null, null);
            int n = 0;
            try (InputStream in = new FileInputStream(System.getProperty("java.home") + "/lib/security/cacerts")) {
                KeyStore jks = KeyStore.getInstance(KeyStore.getDefaultType());
                jks.load(in, null);
                n = copyCerts(jks, merged, "java", n);
            }
            for (String type : new String[] { "Windows-ROOT", "Windows-ROOT-LOCALMACHINE" }) {
                try {
                    KeyStore w = KeyStore.getInstance(type);
                    w.load(null, null);
                    n = copyCerts(w, merged, type, n);
                } catch (Exception ignored) { }
            }
            trust = merged;
            return merged;
        }
    }

    static int copyCerts(KeyStore src, KeyStore dst, String prefix, int n) throws Exception {
        Enumeration<String> al = src.aliases();
        while (al.hasMoreElements()) {
            String a1 = al.nextElement();
            if (src.isCertificateEntry(a1)) {
                Certificate c = src.getCertificate(a1);
                if (c != null) { n++; dst.setCertificateEntry(prefix + "-" + n, c); }
            }
        }
        return n;
    }

    static String[] tlsVersions(String s) {
        switch (s) {
            case "3": return new String[] { "TLSv1" };
            case "4": return new String[] { "TLSv1.1" };
            default: return new String[] { "TLSv1.3", "TLSv1.2" };
        }
    }

    static CloseableHttpClient client(boolean trustAll, String[] protos, Cert cert) throws Exception {
        String key = (trustAll ? "all" : "strict") + "|" + String.join(",", protos) + (cert == null ? "" : "|cert " + cert.thumbprint);
        CloseableHttpClient c = CLIENTS.get(key);
        if (c != null) { return c; }
        synchronized (CLIENTS) {
            c = CLIENTS.get(key);
            if (c != null) { return c; }
            SSLContext ctx;
            KeyManager[] km = cert == null ? null : new KeyManager[] { new OneKey(cert) };
            if (trustAll && cert == null) {
                ctx = SSLContexts.custom().loadTrustMaterial(TrustAllStrategy.INSTANCE).build();
            } else if (trustAll) {
                ctx = SSLContext.getInstance("TLS");
                ctx.init(km, new TrustManager[] { TRUST_ALL }, null);
            } else {
                // strict: Java + Windows roots, and missing intermediates fetched via AIA (as WinHTTP does)
                ctx = SSLContext.getInstance("TLS");
                ctx.init(km, new TrustManager[] { AiaTrustManager.forStore(trustStore()) }, null);
            }
            ClientTlsStrategyBuilder tb = ClientTlsStrategyBuilder.create().setSslContext(ctx).setTlsVersions(protos);
            if (trustAll) {
                tb = tb.setHostVerificationPolicy(HostnameVerificationPolicy.CLIENT).setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            ConnectionConfig cc = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(45000))
                .setSocketTimeout(Timeout.ofMilliseconds(90000))
                .setValidateAfterInactivity(TimeValue.ofSeconds(2))
                .build();
            PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tb.buildClassic())
                .setDefaultConnectionConfig(cc)
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .build();
            c = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultAuthSchemeRegistry(AUTH_SCHEMES)
                .addRequestInterceptorLast(WINHTTP_ORDER)
                .disableRedirectHandling()
                .disableContentCompression()
                .disableAutomaticRetries()
                .disableDefaultUserAgent()
                .evictIdleConnections(TimeValue.ofSeconds(60))
                .build();
            CLIENTS.put(key, c);
            return c;
        }
    }

    /** For SSLERRORS="OK" with a client certificate (without one, TrustAllStrategy is used as before). */
    static final X509ExtendedTrustManager TRUST_ALL = new X509ExtendedTrustManager() {
        public void checkClientTrusted(X509Certificate[] c, String t, Socket s) { }
        public void checkServerTrusted(X509Certificate[] c, String t, Socket s) { }
        public void checkClientTrusted(X509Certificate[] c, String t, SSLEngine e) { }
        public void checkServerTrusted(X509Certificate[] c, String t, SSLEngine e) { }
        public void checkClientTrusted(X509Certificate[] c, String t) { }
        public void checkServerTrusted(X509Certificate[] c, String t) { }
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    };

    // ---- client certificates (release 1.0.0-dev5): CERTSTORENAME + CERTSUBJSTR ----
    // As CFX_HTTP5: a Local Machine store (HKLM\Software\Microsoft\SystemCertificates\<name>); the first certificate
    // with a private key whose subject contains CERTSUBJSTR (any part of any line, any case) is used. Java's SunMSCAPI
    // provider opens the MY and ROOT stores only. The private key is used where Windows keeps it (it is never copied).

    static final class Cert {
        final String alias, thumbprint;
        final PrivateKey key;
        final X509Certificate[] chain;
        final long loaded = System.currentTimeMillis();
        Cert(String alias, String thumbprint, PrivateKey key, X509Certificate[] chain) {
            this.alias = alias; this.thumbprint = thumbprint; this.key = key; this.chain = chain;
        }
    }

    private static final ConcurrentHashMap<String, Cert> CERTS = new ConcurrentHashMap<>();

    static Cert clientCert(String store, String subject, String asUser) throws Exception {
        String s = store.toUpperCase(Locale.ROOT);
        String cacheKey = s + "|" + subject.toLowerCase(Locale.ROOT) + "|" + asUser.toLowerCase(Locale.ROOT);
        Cert c = CERTS.get(cacheKey);
        if (c != null && System.currentTimeMillis() - c.loaded < 60_000L) { return c; }
        String type;
        if (s.equals("MY")) { type = "Windows-MY-LOCALMACHINE"; }
        else if (s.equals("ROOT")) { type = "Windows-ROOT-LOCALMACHINE"; }
        else { throw new CfxError("param", "CERTSTORENAME \"" + store + "\": only the Local Machine stores MY and ROOT can be used"); }
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(null, null);
        String needle = subject.toLowerCase(Locale.ROOT);
        Enumeration<String> al = ks.aliases();
        while (al.hasMoreElements()) {
            String alias = al.nextElement();
            if (!ks.isKeyEntry(alias)) { continue; }
            Certificate cc = ks.getCertificate(alias);
            if (!(cc instanceof X509Certificate)) { continue; }
            X509Certificate x = (X509Certificate) cc;
            if (!needle.isEmpty() && !subjectMatches(x.getSubjectX500Principal(), needle)) { continue; }
            Key k;
            try { k = ks.getKey(alias, null); } catch (Exception e) { continue; }   // a key this account may not use
            if (!(k instanceof PrivateKey)) { continue; }
            List<X509Certificate> chain = new ArrayList<>();
            Certificate[] ch = ks.getCertificateChain(alias);
            if (ch != null) { for (Certificate one : ch) { if (one instanceof X509Certificate) { chain.add((X509Certificate) one); } } }
            if (chain.isEmpty()) { chain.add(x); }
            String tp = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(x.getEncoded()));
            c = new Cert(alias, tp, (PrivateKey) k, chain.toArray(new X509Certificate[0]));
            CERTS.put(cacheKey, c);
            return c;
        }
        throw new CfxError("cert", "No certificate with a private key " + (needle.isEmpty() ? "" : "and \"" + subject + "\" in its subject ")
            + "in LOCAL_MACHINE\\" + s + (asUser.isEmpty() ? "" : " for " + asUser));
    }

    /** CERTSUBJSTR matches any part of the whole subject or of any one line (RDN value) of it, ignoring case. */
    static boolean subjectMatches(X500Principal p, String needle) {
        for (String form : new String[] { X500Principal.RFC1779, X500Principal.RFC2253, X500Principal.CANONICAL }) {
            if (p.getName(form).toLowerCase(Locale.ROOT).contains(needle)) { return true; }
        }
        try {
            for (Rdn r : new LdapName(p.getName(X500Principal.RFC2253)).getRdns()) {
                if (String.valueOf(r.getValue()).toLowerCase(Locale.ROOT).contains(needle)) { return true; }
            }
        } catch (Exception ignored) { }
        return false;
    }

    /** Offers exactly one client certificate. */
    static final class OneKey extends X509ExtendedKeyManager {
        final Cert c;
        OneKey(Cert c) { this.c = c; }
        public String[] getClientAliases(String keyType, Principal[] issuers) { return new String[] { c.alias }; }
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) { return c.alias; }
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) { return c.alias; }
        public String[] getServerAliases(String keyType, Principal[] issuers) { return null; }
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) { return null; }
        public X509Certificate[] getCertificateChain(String alias) { return c.alias.equals(alias) ? c.chain.clone() : null; }
        public PrivateKey getPrivateKey(String alias) { return c.alias.equals(alias) ? c.key : null; }
    }

    // ---- authentication (release 1.0.0-dev5): SCHEMES and HTTPSCHEME ----
    // CFX_HTTP5 numbers: 1 Basic, 2 NTLM, 4 Passport (retired by Microsoft - never used), 8 Digest, 16 Negotiate.
    // Negotiate sends NTLM messages under the "Negotiate" name (what Windows does when Kerberos is not available);
    // Kerberos itself is not attempted. Every scheme records itself per host, so HTTPSCHEME is the scheme really used.

    static final String SCHEME_LOG = "unicom.cfx.schemes";
    static final String USER_HEADERS = "unicom.cfx.userHeaders";

    static final class Tracked implements AuthScheme {
        final AuthScheme d;
        final String name;
        final int code;
        Tracked(AuthScheme d, String name, int code) { this.d = d; this.name = name; this.code = code; }
        public String getName() { return name; }
        public boolean isConnectionBased() { return d.isConnectionBased(); }
        public void processChallenge(AuthChallenge ch, HttpContext x) throws MalformedChallengeException { d.processChallenge(ch, x); }
        public boolean isChallengeExpected() { return d.isChallengeExpected(); }
        public void processChallenge(HttpHost h, boolean challenged, AuthChallenge ch, HttpContext x)
                throws MalformedChallengeException, AuthenticationException { d.processChallenge(h, challenged, ch, x); }
        public boolean isChallengeComplete() { return d.isChallengeComplete(); }
        public String getRealm() { return d.getRealm(); }
        public boolean isResponseReady(HttpHost h, CredentialsProvider p, HttpContext x) throws AuthenticationException { return d.isResponseReady(h, p, x); }
        public Principal getPrincipal() { return d.getPrincipal(); }
        public String generateAuthResponse(HttpHost h, HttpRequest r, HttpContext x) throws AuthenticationException {
            String s = d.generateAuthResponse(h, r, x);
            String dn = d.getName();
            if (!dn.equalsIgnoreCase(name) && s.regionMatches(true, 0, dn + " ", 0, dn.length() + 1)) { s = name + s.substring(dn.length()); }
            Object log = x.getAttribute(SCHEME_LOG);
            if (log instanceof Map && h != null) {
                @SuppressWarnings("unchecked") Map<String, Integer> m = (Map<String, Integer>) log;
                m.put(h.getHostName().toLowerCase(Locale.ROOT), code);
            }
            return s;
        }
    }

    @SuppressWarnings("deprecation")
    static final Lookup<AuthSchemeFactory> AUTH_SCHEMES = RegistryBuilder.<AuthSchemeFactory>create()
        .register("Basic", x -> new Tracked(new BasicScheme(), "Basic", 1))
        .register("Digest", x -> new Tracked(new DigestScheme(), "Digest", 8))
        .register("NTLM", x -> new Tracked(new NTLMScheme(), "NTLM", 2))
        .register("Negotiate", x -> new Tracked(new NTLMScheme(), "Negotiate", 16))
        .build();

    /** SCHEMES (a sum of the numbers above; 0 or missing = any) -> the schemes to try, strongest first, as WinHTTP does. */
    static List<String> schemes(String v) {
        long mask = Attrs.val(v);
        List<String> out = new ArrayList<>();
        if (mask == 0 || (mask & 16) != 0) { out.add("Negotiate"); }
        if (mask == 0 || (mask & 2) != 0) { out.add("NTLM"); }
        if (mask == 0 || (mask & 8) != 0) { out.add("Digest"); }
        if (mask == 0 || (mask & 1) != 0) { out.add("Basic"); }
        return out;
    }

    /** SCHEMES is for the web site only; a proxy may use any of them. */
    static final List<String> PROXY_SCHEMES = schemes("0");

    // ---- header order (release 1.0.0-dev5) ----
    // WinHTTP (side-by-side test, Oct 15, 2026) sends general headers first (Cache-Control, Connection, Pragma, ...),
    // then Content-Type, then all other headers sorted by name (custom ones too), then Content-Length and Host, and the
    // cookies of its own cookie handling last.

    private static final Set<String> GENERAL = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    static {
        GENERAL.addAll(Arrays.asList("Cache-Control", "Connection", "Date", "Pragma", "Trailer", "Transfer-Encoding", "Upgrade", "Via", "Warning"));
    }

    static int headerGroup(String name) {
        if (GENERAL.contains(name)) { return 0; }
        if (name.regionMatches(true, 0, "Content-", 0, 8) && !name.equalsIgnoreCase("Content-Length")) { return 1; }
        return 2;
    }

    static List<String[]> winHttpOrder(List<String[]> headers) {
        List<String[]> out = new ArrayList<>(headers);
        out.sort((x, y) -> {
            int g = Integer.compare(headerGroup(x[0]), headerGroup(y[0]));
            return g != 0 ? g : String.CASE_INSENSITIVE_ORDER.compare(x[0], y[0]);
        });
        return out;
    }

    /** Runs after HttpClient's own interceptors: our headers, then Content-Length, Host, then what HttpClient added. */
    static final HttpRequestInterceptor WINHTTP_ORDER = (req, entity, ctx) -> {
        Object n = ctx.getAttribute(USER_HEADERS);
        if (!(n instanceof Integer)) { return; }
        Header[] all = req.getHeaders();
        int k = Math.min((Integer) n, all.length);
        List<Header> out = new ArrayList<>(Arrays.asList(all).subList(0, k));
        List<Header> rest = Arrays.asList(all).subList(k, all.length);
        for (Header h : rest) { if (h.getName().equalsIgnoreCase("Content-Length")) { out.add(h); } }
        for (Header h : rest) { if (h.getName().equalsIgnoreCase("Host")) { out.add(h); } }
        for (Header h : rest) { if (!h.getName().equalsIgnoreCase("Content-Length") && !h.getName().equalsIgnoreCase("Host")) { out.add(h); } }
        req.setHeaders(out.toArray(new Header[0]));
    };

    // ---- URL, headers, body ----

    static String decodePercent(String s) throws Exception {
        return URLDecoder.decode(s.replace("+", "%2B"), "UTF-8");
    }

    private static final String ALLOWED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=";
    private static final String HEX = "0123456789ABCDEFabcdef";

    /** Escape only characters that are illegal in a URL (space -> %20, | -> %7C); keep valid %XX escapes (mode "E": escape every %). */
    static String encodeUrl(String u, String mode) throws Exception {
        String s = mode.equals("Y") ? decodePercent(u) : u;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            int w = Character.charCount(cp);
            if (cp == '%') {
                boolean esc = !mode.equals("E") && i + 2 < s.length()
                    && HEX.indexOf(s.charAt(i + 1)) >= 0 && HEX.indexOf(s.charAt(i + 2)) >= 0;
                sb.append(esc ? "%" : "%25");
            } else if (cp < 128 && ALLOWED.indexOf(cp) >= 0) {
                sb.append((char) cp);
            } else {
                for (byte b : new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8)) {
                    sb.append('%').append(String.format("%02X", b & 0xFF));
                }
            }
            i += w;
        }
        return sb.toString();
    }

    static List<String[]> buildHeaders(Ini cfg, String userHeaders, boolean keepAlive) {
        List<String[]> list = new ArrayList<>();
        for (String[] h : cfg.headers) { list.add(new String[] { h[0], h[1] }); }
        // ALIVE / ini Keep-Alive: "Keep-Alive" mode or "Close" mode (the connection is not reused) - release 1.0.0-dev5
        list.add(new String[] { "Connection", keepAlive ? "Keep-Alive" : "close" });
        String text = userHeaders.replace(CRLF, "\n").replace("\r", "\n");
        for (String line : text.split("\n")) {
            if (line.isEmpty()) { continue; }
            int c = line.indexOf(':');
            if (c < 0) { continue; }
            String nm = line.substring(0, c).trim();
            String vl = line.substring(c + 1).trim();
            int idx = -1;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j)[0].equalsIgnoreCase(nm)) { idx = j; break; }
            }
            if (vl.isEmpty()) {
                if (idx >= 0) { list.remove(idx); }
            } else if (idx >= 0) {
                list.get(idx)[1] = vl;
            } else {
                list.add(new String[] { nm, vl });
            }
        }
        return list;
    }

    static List<String[]> removeHeader(List<String[]> list, String name) {
        List<String[]> out = new ArrayList<>();
        for (String[] h : list) { if (!h[0].equalsIgnoreCase(name)) { out.add(h); } }
        return out;
    }

    static boolean sendsBody(String method) { return method.equals("POST") || method.equals("PUT") || method.equals("PATCH"); }

    static byte[] buildBody(Attrs a, String method) throws Exception {
        if (!sendsBody(method)) { return null; }
        Charset cs = Charset.forName(a.get("charsetout", "utf-8"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String b = a.raw("body");
        if (!b.isEmpty()) { bos.write(b.getBytes(cs)); }
        String bf = a.get("bodyfile");
        if (!bf.isEmpty()) { bos.write(Files.readAllBytes(Paths.get(bf))); }
        String be = a.raw("bodyend");
        if (!be.isEmpty()) { bos.write(be.getBytes(cs)); }
        return bos.toByteArray();
    }

    /** CFML list functions: split on any of the delimiter characters, dropping empty items. */
    static List<String> items(String s, String delims) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (delims.indexOf(ch) >= 0) { if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); } }
            else { cur.append(ch); }
        }
        if (cur.length() > 0) { out.add(cur.toString()); }
        return out;
    }

    static HttpHost parseProxy(String s) {
        if (s.isEmpty()) { return null; }
        String p = s;
        if (p.contains("=")) { List<String> it = items(p, "="); p = it.isEmpty() ? "" : it.get(it.size() - 1); }
        String scheme = "http";
        int ss = p.indexOf("://");
        if (ss >= 0) { scheme = p.substring(0, ss).toLowerCase(Locale.ROOT); p = p.substring(ss + 3); }
        List<String> hp = items(p, ":/");
        String host = hp.isEmpty() ? "" : hp.get(0);
        int port = 80;
        if (items(p, ":").size() > 1 && hp.size() > 1) { port = (int) Attrs.val(hp.get(1)); }
        return new HttpHost(scheme, host, port);
    }

    static HttpClientContext newContext(boolean cookies) {
        HttpClientContext ctx = HttpClientContext.create();
        if (cookies) { ctx.setCookieStore(new BasicCookieStore()); }
        return ctx;
    }

    static void setCredentials(HttpClientContext context, Attrs a, HttpHost proxyHost, String urlStr) throws Exception {
        BasicCredentialsProvider cp = new BasicCredentialsProvider();
        if (!a.get("user").isEmpty()) {
            URI u = new URI(urlStr);
            String user = a.get("user");
            char[] pass = a.get("pass").toCharArray();
            // only for this exact scheme + host + port (release 1.0.0-dev5): a redirect to another port, or from https
            // to http on the same host, never gets the login
            String scheme = u.getScheme() == null ? "http" : u.getScheme().toLowerCase(Locale.ROOT);
            int port = u.getPort() >= 0 ? u.getPort() : (scheme.equals("https") ? 443 : 80);
            // Basic / Digest send USER as given ("DOMAIN\USER" included, as WinHTTP does); NTLM needs it split
            cp.setCredentials(new AuthScope(scheme, u.getHost(), port, null, null), new UsernamePasswordCredentials(user, pass));
            cp.setCredentials(new AuthScope(scheme, u.getHost(), port, null, "NTLM"), ntCredentials(user, pass));
        }
        if (!a.get("proxyuser").isEmpty() && proxyHost != null) {
            String user = a.get("proxyuser");
            char[] pass = a.get("proxypass").toCharArray();
            cp.setCredentials(new AuthScope(proxyHost), new UsernamePasswordCredentials(user, pass));
            cp.setCredentials(new AuthScope(proxyHost, null, "NTLM"), ntCredentials(user, pass));
        }
        context.setCredentialsProvider(cp);
    }

    /** "DOMAIN\USER" -> domain + user; "user" or "user@domain" -> no domain (the server's own, or the UPN's). */
    static NTCredentials ntCredentials(String user, char[] pass) {
        String dom = null, usr = user;
        int bs = user.indexOf('\\');
        if (bs > 0) { dom = user.substring(0, bs); usr = user.substring(bs + 1); }
        return new NTCredentials(pass, usr, dom, dom == null ? null : dom.toUpperCase(Locale.ROOT));
    }

    /**
     * Pooled connections are kept by HttpClient per route AND per "user token": calls with different logins (or none)
     * must not share a connection - a proxy tunnel or an NTLM connection is authenticated once, for its first caller.
     */
    static String userToken(Attrs a, HttpHost proxyHost) throws Exception {
        String user = a.get("user"), puser = proxyHost == null ? "" : a.get("proxyuser");
        if (user.isEmpty() && puser.isEmpty()) { return null; }
        String all = user + "\u0000" + (user.isEmpty() ? "" : a.get("pass") + "\u0000" + Attrs.val(a.get("schemes"))) + "\u0000" + (proxyHost == null ? "" : proxyHost.toHostString())
            + "\u0000" + puser + "\u0000" + (puser.isEmpty() ? "" : a.get("proxypass"));
        byte[] h = MessageDigest.getInstance("SHA-256").digest(all.getBytes(StandardCharsets.UTF_8));
        return "cfx5-" + HexFormat.of().formatHex(h, 0, 16);
    }

    // ---- the WinHTTP default proxy (release 1.0.0-dev5): used when PROXYSERVER is empty, as CFX_HTTP5 does ----
    // It is the machine-wide setting of "netsh winhttp set proxy" (not the browser's); read again after 60 seconds.

    private static volatile String[] sysProxy;
    private static volatile long sysProxyAt;

    static String[] sysProxyConfig() {
        String[] p = sysProxy;
        long now = System.currentTimeMillis();
        if (p == null || now - sysProxyAt > 60_000L) {
            try { p = nat().defaultProxy(); } catch (Throwable t) { p = new String[] { "1", "", "" }; }
            sysProxy = p;
            sysProxyAt = now;
        }
        return p;
    }

    static HttpHost defaultProxy(String urlStr) throws Exception {
        String[] p = sysProxyConfig();
        if (!"3".equals(p[0]) || p[1].trim().isEmpty()) { return null; }
        URI u = new URI(urlStr);
        String scheme = u.getScheme() == null ? "http" : u.getScheme().toLowerCase(Locale.ROOT);
        if (bypassed(u.getHost() == null ? "" : u.getHost(), p[2])) { return null; }
        String any = null, mine = null;
        for (String e : items(p[1], "; \t\r\n")) {
            int eq = e.indexOf('=');
            if (eq < 0) { if (any == null) { any = e; } }
            else if (e.substring(0, eq).trim().equalsIgnoreCase(scheme) && mine == null) { mine = e.substring(eq + 1).trim(); }
        }
        String chosen = mine != null ? mine : any;
        return chosen == null || chosen.isEmpty() ? null : parseProxy(chosen);
    }

    /** WinHTTP bypass list: "<local>" = names without a dot; other entries are host names with * wildcards. */
    static boolean bypassed(String host, String list) {
        String h = host.toLowerCase(Locale.ROOT);
        for (String e : items(list.toLowerCase(Locale.ROOT), "; ,\t\r\n")) {
            if (e.equals("<local>")) { if (!h.contains(".") && !h.contains(":")) { return true; } continue; }
            String pat = e;
            int ss = pat.indexOf("://");
            if (ss >= 0) { pat = pat.substring(ss + 3); }
            int colon = pat.lastIndexOf(':');
            if (colon > 0 && pat.indexOf(']') < colon) { pat = pat.substring(0, colon); }
            StringBuilder re = new StringBuilder();
            for (char ch : pat.toCharArray()) { re.append(ch == '*' ? ".*" : Pattern.quote(String.valueOf(ch))); }
            if (h.matches(re.toString())) { return true; }
        }
        return false;
    }

    // ---- FILEUSER / FILEPASS / FILEDOMAIN (release 1.0.0-dev5) ----
    // As CFX_HTTP5: log on as that Windows account and use it to read BODYFILE, write the FILE output and open the
    // client certificate. Only those steps run impersonated (on this thread; always reverted). In a session the logon
    // of the call that started it is kept until the session closes.

    interface IO<T> { T run() throws Exception; }

    static <T> T asUser(long logon, IO<T> io) throws Exception {
        if (logon == 0) { return io.run(); }
        Native n = nat();
        try { n.impersonate(logon); } catch (Throwable t) { throw asException(t); }
        try {
            return io.run();
        } finally {
            try { n.revert(); } catch (Throwable t) { throw new IllegalStateException("RevertToSelf failed: " + t.getMessage(), t); }
        }
    }

    static long logon(Attrs a) throws Exception {
        try {
            return nat().logon(a.get("fileuser"), a.get("filedomain"), a.raw("filepass").toCharArray());
        } catch (Throwable t) {
            throw asException(t);
        }
    }

    static Exception asException(Throwable t) {
        if (t instanceof Exception) { return (Exception) t; }
        if (t instanceof Error) { throw (Error) t; }
        return new Exception(t);
    }

    @SuppressWarnings("deprecation")
    static HttpUriRequestBase makeRequest(String method, String urlStr, List<String[]> headers, byte[] body, long timeoutMs,
                                          long maxTime, boolean cookiesOn, HttpHost proxyHost, List<String> schemes) throws Exception {
        HttpUriRequestBase req = new HttpUriRequestBase(method, new URI(urlStr));
        for (String[] h : headers) { req.addHeader(h[0], h[1]); }
        if (sendsBody(method) && body != null) { req.setEntity(new ByteArrayEntity(body, null)); }
        long connectMs = timeoutMs;
        long respMs = Math.max(timeoutMs, 90000);
        if (maxTime > 0) { connectMs = Math.min(connectMs, maxTime); respMs = Math.min(respMs, maxTime); }
        RequestConfig.Builder rc = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectMs))
            .setResponseTimeout(Timeout.ofMilliseconds(respMs))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(60000))
            .setRedirectsEnabled(false)
            .setContentCompressionEnabled(false)
            .setExpectContinueEnabled(false)
            .setCookieSpec(cookiesOn ? StandardCookieSpec.RELAXED : StandardCookieSpec.IGNORE)
            .setTargetPreferredAuthSchemes(schemes)
            .setProxyPreferredAuthSchemes(PROXY_SCHEMES);
        if (proxyHost != null) { rc.setProxy(proxyHost); }
        req.setConfig(rc.build());
        return req;
    }

    static String requestHead(HttpUriRequestBase req) throws Exception {
        URI u = req.getUri();
        String path = (u.getRawPath() == null || u.getRawPath().isEmpty()) ? "/" : u.getRawPath();
        if (u.getRawQuery() != null) { path += "?" + u.getRawQuery(); }
        StringBuilder s = new StringBuilder(req.getMethod()).append(' ').append(path).append(" HTTP/1.1").append(CRLF);
        for (Header h : req.getHeaders()) { s.append(h.getName()).append(": ").append(h.getValue()).append(CRLF); }
        return s.append(CRLF).toString();
    }

    static int gzipModeFor(String g, List<String[]> headers) {
        String m = g.toUpperCase(Locale.ROOT);
        if (m.equals("0") || m.equals("N")) { return 0; }
        if (m.equals("1")) { return 1; }
        if (m.equals("2") || m.equals("Y")) { return 2; }
        for (String[] h : headers) {
            if (h[0].equalsIgnoreCase("Accept-Encoding") && h[1].toLowerCase(Locale.ROOT).contains("gzip")) { return 2; }
        }
        return 0;
    }

    private static final Pattern CHARSET = Pattern.compile("charset\\s*=\\s*\"?([^\";\\s]+)", Pattern.CASE_INSENSITIVE);

    static String charsetFrom(String contentType) {
        Matcher m = CHARSET.matcher(contentType);
        return m.find() ? m.group(1) : "";
    }

    // ---- sessions ----

    static void closeSession(String id) {
        if (id != null && !id.isEmpty()) { release(SESSIONS.remove(id)); }
    }

    static void release(Sess s) {
        if (s != null && s.logon != 0) {
            long h = s.logon;
            s.logon = 0;
            try { nat().close(h); } catch (CfxError ignored) { }
        }
    }

    static void sweepSessions() {
        long now = System.currentTimeMillis();
        if (now - lastSweep < 10 * 60_000L) { return; }
        lastSweep = now;
        for (Map.Entry<String, Sess> e : SESSIONS.entrySet()) {
            if (now - e.getValue().used > 120 * 60_000L && SESSIONS.remove(e.getKey(), e.getValue())) { release(e.getValue()); }
        }
    }

    static String newId() { return UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT); }

    // ---- errors -> WinHTTP-style ERRN/MSG ----

    static void mapError(Throwable e, Result r) {
        StringBuilder txt = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            txt.append(t.getClass().getName()).append(' ').append(t.getMessage()).append(' ');
            if (t.getCause() == t) { break; }
        }
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String st = sw.toString();
        txt.append(st, 0, Math.min(400, st.length()));
        String all = txt.toString();
        if (e instanceof NativeError) {
            // a Windows error (FILEUSER logon, impersonation): its own code and text, as CFX_HTTP5 reports them
            r.errn = String.valueOf(((NativeError) e).code);
            r.msg = e.getMessage() + " [cfx_http5 java: FILEUSER]" + CRLF;
            return;
        }
        String n = "12030";
        String w = "The connection with the server was terminated abnormally";
        String type = (e instanceof CfxError) ? ((CfxError) e).type : "";
        if (type.equals("session")) { n = "6"; w = "The handle is invalid"; }
        else if (type.equals("cert")) { n = "12044"; w = "A certificate is required to complete client authentication"; }
        else if (type.equals("native")) { n = "50"; w = "The request is not supported"; }
        else if (type.equals("param")) { n = "87"; w = "The parameter is incorrect"; }
        else if (type.equals("logging")) { n = "5"; w = "Access is denied"; }
        else if (find("SocketTimeout|ConnectTimeout|timed out|Read timed out", all)) { n = "12002"; w = "The operation timed out"; }
        else if (find("UnknownHost", all)) { n = "12007"; w = "The server name or address could not be resolved"; }
        else if (find("HttpHostConnect|ConnectException|Connection refused", all)) { n = "12029"; w = "A connection with the server could not be established"; }
        else if (find("SSL|PKIX|certificate|handshake|No appropriate protocol", all)) { n = "12175"; w = "A security error occurred"; }
        else if (find("URISyntax|Illegal character", all)) { n = "12005"; w = "The URL is invalid"; }
        r.errn = n;
        String m = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
        r.msg = w + ". [cfx_http5 java] " + m.trim() + CRLF;
    }

    static boolean find(String regex, String s) { return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(s).find(); }

    // ---- one HTTP request (with redirects) ----

    static Result doRequest(Attrs a) {
        Result r = new Result();
        r.httpcontext = a.get("context", "0");
        if (r.httpcontext.isEmpty()) { r.httpcontext = "0"; }
        long t0 = System.currentTimeMillis();
        String sessionId = a.get("session");
        Sess sess = null;
        ClassicHttpResponse response = null;
        long ownLogon = 0;
        try {
            quietLogging();
            sweepSessions();
            Ini cfg = loadIni();
            String url0 = a.get("url");
            if (url0.isEmpty()) { throw new CfxError("param", "URL parameter is required"); }
            String method = a.get("method", "GET").toUpperCase(Locale.ROOT);
            if (method.isEmpty()) { method = "GET"; }
            long waitMs = Attrs.val(a.get("wait", "0"));
            if (waitMs > 0) { Thread.sleep(waitMs); }
            String fullUrl = encodeUrl(url0, a.get("urldecode", cfg.urlDecode).toUpperCase(Locale.ROOT));
            long timeoutMs = Attrs.val(a.get("timeout", "45000"));
            if (timeoutMs <= 0) { timeoutMs = 45000; }
            long maxTime = Attrs.val(a.get("maxtime", "0"));
            boolean keepAlive = cfg.keepAlive;
            if (a.get("alive").equalsIgnoreCase("Y")) { keepAlive = true; } else if (a.get("alive").equalsIgnoreCase("N")) { keepAlive = false; }
            boolean trustAll = a.get("sslerrors").equalsIgnoreCase("OK");
            String[] protos = tlsVersions(a.get("ssl"));

            boolean started = false;
            if (!sessionId.isEmpty()) {
                if (sessionId.equalsIgnoreCase("START")) {
                    sessionId = newId();
                    sess = new Sess(newContext(true), a.get("proxyserver"));
                    SESSIONS.put(sessionId, sess);
                    started = true;
                } else {
                    sess = SESSIONS.get(sessionId);
                    if (sess == null) { throw new CfxError("session", "Invalid session ID: " + sessionId); }
                    sess.used = System.currentTimeMillis();
                }
                r.httpsession = sessionId;
            }
            // FILEUSER: kept by a session that it started; otherwise for this call only
            long logon = 0;
            String logonUser = "";
            if (sess != null && sess.logon != 0) {
                logon = sess.logon;
                logonUser = sess.logonUser;
            } else if (!a.get("fileuser").isEmpty()) {
                logon = logon(a);
                logonUser = (a.get("filedomain").isEmpty() ? "" : a.get("filedomain") + "\\") + a.get("fileuser");
                if (started) { sess.logon = logon; sess.logonUser = logonUser; } else { ownLogon = logon; }
            }
            boolean cookiesOn = sess != null || a.yes("cookie");
            HttpClientContext context = sess != null ? sess.context : newContext(cookiesOn);
            String proxySpec = (sess != null && !sess.proxy.isEmpty()) ? sess.proxy : a.get("proxyserver");
            HttpHost proxyHost = proxySpec.isEmpty() ? defaultProxy(fullUrl) : parseProxy(proxySpec);
            if (!a.get("user").isEmpty() || !a.get("proxyuser").isEmpty()) {
                setCredentials(context, a, proxyHost, fullUrl);
            }
            context.setUserToken(userToken(a, proxyHost));
            Map<String, Integer> schemeLog = new ConcurrentHashMap<>();
            context.setAttribute(SCHEME_LOG, schemeLog);
            List<String> schemes = schemes(a.get("schemes"));
            List<String[]> headers = winHttpOrder(buildHeaders(cfg, a.raw("headers"), keepAlive));
            final String method0 = method;
            byte[] body = asUser(logon, () -> buildBody(a, method0));
            Cert cert = null;
            if (!a.get("certstorename").isEmpty()) {
                final String lu = logonUser;
                cert = asUser(logon, () -> clientCert(a.get("certstorename"), a.raw("certsubjstr").trim(), lu));
            }
            CloseableHttpClient client = client(trustAll, protos, cert);
            boolean follow = !a.get("redirect").equalsIgnoreCase("N");
            boolean allowDowngrade = a.get("redirect").equalsIgnoreCase("Y");
            String currentUrl = fullUrl;
            String currentMethod = method;
            int hops = 0;
            int code;
            while (true) {
                HttpUriRequestBase req = makeRequest(currentMethod, currentUrl, headers, body, timeoutMs, maxTime, cookiesOn, proxyHost, schemes);
                if (hops == 0) { r.qhead = requestHead(req); }
                context.setAttribute(USER_HEADERS, headers.size());
                response = client.executeOpen(null, req, context);
                code = response.getCode();
                Header loc = response.getFirstHeader("Location");
                if (follow && (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) && loc != null && hops < 10) {
                    String nextUrl = new URI(currentUrl).resolve(encodeUrl(loc.getValue(), "N")).toString();
                    if (currentUrl.toLowerCase(Locale.ROOT).startsWith("https:") && nextUrl.toLowerCase(Locale.ROOT).startsWith("http:") && !allowDowngrade) { break; }
                    EntityUtils.consumeQuietly(response.getEntity());
                    response.close();
                    response = null;
                    if ((code == 303 && !currentMethod.equals("HEAD")) || ((code == 301 || code == 302) && currentMethod.equals("POST"))) {
                        currentMethod = "GET";
                        body = null;
                        headers = removeHeader(headers, "Content-Type");
                    }
                    currentUrl = nextUrl;
                    hops++;
                    continue;
                }
                break;
            }

            r.httpstatus = String.valueOf(code);
            String finalHost = new URI(currentUrl).getHost();
            Integer used = finalHost == null ? null : schemeLog.get(finalHost.toLowerCase(Locale.ROOT));
            r.httpscheme = used == null ? "-1" : String.valueOf(used);
            String reason = response.getReasonPhrase() == null ? "" : response.getReasonPhrase();
            ProtocolVersion pv = response.getVersion();
            StringBuilder hb = new StringBuilder(pv == null ? "HTTP/1.1" : pv.toString()).append(' ').append(code);
            if (!reason.isEmpty()) { hb.append(' ').append(reason); }
            hb.append(CRLF);
            String contentLength = "-1", contentType = "", contentEncoding = "";
            for (Header hh : response.getHeaders()) {
                hb.append(hh.getName()).append(": ").append(hh.getValue()).append(CRLF);
                if (hh.getName().equalsIgnoreCase("Content-Length")) { contentLength = hh.getValue(); }
                else if (hh.getName().equalsIgnoreCase("Content-Type")) { contentType = hh.getValue(); }
                else if (hh.getName().equalsIgnoreCase("Content-Encoding")) { contentEncoding = hh.getValue(); }
            }
            r.head = hb.append(CRLF).toString();
            r.httplength = contentLength;

            String outName = a.get("out");
            byte[] bytes = new byte[0];
            HttpEntity entity = response.getEntity();
            if (!currentMethod.equals("HEAD") && !outName.isEmpty() && entity != null) {
                long maxLen = Attrs.val(a.get("maxlen", "0"));
                if (maxLen > 0) {
                    try (InputStream ins = entity.getContent()) { bytes = ins.readNBytes((int) Math.min(maxLen, Integer.MAX_VALUE)); }
                } else {
                    byte[] got = EntityUtils.toByteArray(entity);
                    if (got != null) { bytes = got; }
                }
            }
            int gzMode = gzipModeFor(a.get("gzip"), headers);
            if (bytes.length > 0 && ((gzMode == 2 && contentEncoding.toLowerCase(Locale.ROOT).contains("gzip")) || gzMode == 1)) {
                try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(bytes))) { bytes = gz.readAllBytes(); }
            }
            r.httpbytes = String.valueOf(bytes.length);
            boolean accepted = true;
            String acceptType = a.get("accepttype");
            if (!acceptType.isEmpty() && code == 200) {
                String ct = contentType.contains(";") ? contentType.substring(0, contentType.indexOf(';')) : contentType;
                ct = ct.trim();
                accepted = false;
                for (String t : items(acceptType, ",")) { if (!ct.isEmpty() && t.equalsIgnoreCase(ct)) { accepted = true; break; } }
                if (!accepted) { bytes = new byte[0]; }
            }
            if (!outName.isEmpty()) {
                if (a.yes("file")) {
                    if (code >= 200 && code <= 299 && bytes.length > 0 && accepted) {
                        final byte[] data = bytes;
                        asUser(logon, () -> Files.write(Paths.get(outName), data));
                    }
                } else if (a.yes("base64")) {
                    r.body = Base64.getEncoder().encodeToString(bytes);
                    r.hasBody = true;
                } else {
                    String cs = a.get("charsetin");
                    if (cs.isEmpty()) { cs = charsetFrom(contentType); }
                    String text;
                    if (cs.isEmpty()) { text = decodeUnnamed(bytes); }
                    else { try { text = new String(bytes, Charset.forName(cs)); } catch (Exception e2) { text = decodeUnnamed(bytes); } }
                    r.body = text.indexOf('\0') >= 0 ? text.replace('\0', ' ') : text;
                    r.hasBody = true;
                }
            }
            response.close();
            response = null;
            if (sess != null && a.yes("sessionend")) { closeSession(sessionId); r.httpsession = ""; }
        } catch (Throwable e) {
            if (e instanceof InterruptedException) { Thread.currentThread().interrupt(); }
            r.status = "ER";
            mapError(e, r);
            if (response != null) { try { response.close(); } catch (Exception ignored) { } }
            if (!sessionId.isEmpty() && !sessionId.equalsIgnoreCase("START")) { closeSession(sessionId); }
            r.httpsession = "";
            r.hasBody = false;
        } finally {
            if (ownLogon != 0) { try { nat().close(ownLogon); } catch (CfxError ignored) { } }
        }
        r.httptime = String.valueOf(System.currentTimeMillis() - t0);
        return r;
    }

    /**
     * A body with no charset (none in CHARSETIN or Content-Type, or an unknown name): UTF-8 when the bytes are valid
     * UTF-8, otherwise ISO-8859-1 (one character per byte), as CFX_HTTP5 does - side-by-side cases H27 / H23,
     * Oct 15, 2026. Keeps Latin-1 pages served without a charset readable, and binary data reversible.
     */
    static String decodeUnnamed(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    static void applyResult(Result r, Attrs a, Out out) {
        out.set("STATUS", r.status);
        out.set("HTTPSESSION", r.httpsession);
        out.set("HTTPTIME", r.httptime);
        out.set("HTTPCONTEXT", r.httpcontext);
        if (r.status.equals("ER")) {
            out.set("MSG", r.msg);
            out.set("ERRN", r.errn);
            return;
        }
        out.set("HTTPSTATUS", r.httpstatus);
        out.set("HTTPSCHEME", r.httpscheme);
        out.set("HTTPLENGTH", r.httplength);
        out.set("HTTPBYTES", r.httpbytes);
        if (r.hasBody && !a.get("out").isEmpty()) { out.set(a.get("out"), r.body); }
        if (!a.get("outhead").isEmpty()) { out.set(a.get("outhead"), r.head); }
        if (!a.get("outqhead").isEmpty()) { out.set(a.get("outqhead"), r.qhead); }
    }

    // ---- asynchronous requests (ASYNC / DISCARD, then GET / WGET / WAIT / CANCEL) ----

    static void startAsync(Attrs a, Out out) {
        String reqid = newId();
        Attrs a2 = a.copyWithout("async");
        boolean discard = a.yes("discard");
        AsyncReq rec = new AsyncReq(a2, discard, a.get("context", "0"));
        REQUESTS.put(reqid, rec);
        POOL.submit(() -> {
            Result res = doRequest(a2);
            if (rec.discard) {
                REQUESTS.remove(reqid);
                if (!res.httpsession.isEmpty()) { closeSession(res.httpsession); }
            } else {
                rec.result = res;
            }
        });
        out.set("STATUS", "OK");
        if (!discard) { out.set("HTTPREQID", reqid); }
    }

    static List<String> waitIds(List<String> ids, long timeoutMs) throws InterruptedException {
        long t0 = System.currentTimeMillis();
        while (true) {
            List<String> ready = new ArrayList<>();
            for (String id : ids) {
                AsyncReq rec = REQUESTS.get(id);
                if (rec != null && rec.result != null) { ready.add(id); }
            }
            if (!ready.isEmpty() || (timeoutMs >= 0 && System.currentTimeMillis() - t0 >= timeoutMs)) { return ready; }
            Thread.sleep(20);
        }
    }

    static final int MAX_IDS = 64;   // CFX_HTTP5: functions working on request lists take at most 64 IDs

    static boolean tooMany(List<String> ids, Out out) {
        if (ids.size() <= MAX_IDS) { return false; }
        out.set("STATUS", "ER");
        out.set("ERRN", "87");
        out.set("MSG", "The parameter is incorrect. [cfx_http5 java] REQID lists can hold at most " + MAX_IDS + " request IDs (got " + ids.size() + ")" + CRLF);
        return true;
    }

    static void doGetResult(String fnc, Attrs a, Out out) throws InterruptedException {
        List<String> ids = items(a.get("reqid"), ",");
        if (ids.isEmpty()) { out.set("STATUS", "ER"); out.set("ERRN", "87"); out.set("MSG", "REQID parameter is required" + CRLF); return; }
        if (tooMany(ids, out)) { return; }
        if (fnc.equals("GET")) { ids = new ArrayList<>(ids.subList(0, 1)); }
        for (String id : ids) {
            if (!REQUESTS.containsKey(id)) { out.set("STATUS", "ER"); out.set("ERRN", "6"); out.set("MSG", "Invalid request ID: " + id + CRLF); return; }
        }
        List<String> ready = waitIds(ids, -1);
        String doneId = ready.get(0);
        AsyncReq rec = REQUESTS.remove(doneId);
        applyResult(rec.result, rec.attrs, out);
        if (fnc.equals("WGET")) {
            int pos = ids.indexOf(doneId);
            out.set("HTTPREQID", doneId);
            out.set("REQN", String.valueOf(pos + 1));
            ids.remove(pos);
            out.set("HTTPREQLIST", String.join(",", ids));
        }
    }

    static void doWait(Attrs a, Out out) throws InterruptedException {
        List<String> ids = items(a.get("reqid"), ",");
        if (tooMany(ids, out)) { return; }
        String t = a.get("timeout", "-1");
        List<String> ready = waitIds(ids, t.isEmpty() ? -1 : Attrs.val(t));
        List<String> running = new ArrayList<>();
        List<String> info = new ArrayList<>();
        for (String id : ids) {
            if (!ready.contains(id)) {
                running.add(id);
                AsyncReq rec = REQUESTS.get(id);
                info.add((rec != null ? rec.context : "0") + ",0,0");
            }
        }
        out.set("STATUS", "OK");
        out.set("HTTPREQREADY", String.join(",", ready));
        out.set("HTTPREQLIST", String.join(",", running));
        if (!a.get("info").isEmpty()) { out.set(a.get("info"), String.join("~", info)); }
    }

    static void doCancel(Attrs a, Out out) {
        if (tooMany(items(a.get("reqid"), ","), out)) { return; }
        for (String id : items(a.get("reqid"), ",")) {
            AsyncReq rec = REQUESTS.remove(id);
            if (rec != null) {
                String sid = rec.attrs.get("session");
                if (!sid.isEmpty() && !sid.equalsIgnoreCase("START")) { closeSession(sid); }
            }
        }
        out.set("STATUS", "OK");
        out.set("HTTPSESSION", "");
    }

    static void doDns(Attrs a, Out out) {
        try {
            String addr = a.get("address", a.get("url"));
            if (addr.contains("://")) { addr = new URI(addr).getHost(); }
            InetAddress ia = InetAddress.getByName(addr);
            out.set("IP_ADDR", ia.getHostAddress());
            out.set("DNS_ADDR", ia.getCanonicalHostName());
            out.set("STATUS", "OK");
        } catch (Exception e) {
            out.set("STATUS", "ER");
            out.set("ERRN", "12007");
            out.set("MSG", "The server name or address could not be resolved. " + e.getMessage() + CRLF);
        }
    }
}
