/*
 * Copyright (C) 2025 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class HttpGetTest {

  private HttpGet http;

  @BeforeEach
  void setUp() {
    http = new HttpGet();
  }

  String removeTimestamp(String s) {
    Pattern pattern = Pattern.compile("(<h2>Remote: \\d+\\.\\d+\\.\\d+\\.\\d+ )(\\d+)(</h2>)");
    s = pattern.matcher(s).replaceFirst(m -> m.group(1) + "0" + m.group(3));
    // JDK bug https://bugs.openjdk.org/browse/JDK-8283544
    s = s.replace("<tr><td valign=top><b>Content-Length:</b></td><td> 0</td></tr>\n", "");
    // didn't find a way to remove it from sun.net.www.protocol.http.HttpURLConnection
    s = s.replace("<tr><td valign=top><b>Connection:</b></td><td> keep-alive</td></tr>\n", "");

    String[] headers = {"User-Agent", "Host", "Accept", "Connection"};
    for (int i = headers.length - 1; i >= 0; i--) {
      String header = headers[i];
      s = Pattern.compile("(<table[^>]*>\\n)(.*?)(<tr><td valign=top><b>" + header +
          ":</b></td><td>[^\\n]*</td></tr>\\n)", Pattern.DOTALL)
          .matcher(s).replaceFirst(m -> m.group(1) + m.group(3) + m.group(2));
    }
    return Pattern.compile("<h2>Raw request</h2><pre>.*", Pattern.DOTALL).matcher(s).replaceFirst("");
  }

  /**
   * Consistent request data.
   */
  @Test
  void testRemoveTimestamp() {
    String s1 = "<style>body {color: black; background-color: white;} tr:hover { background: yellow }</style>" +
        "<h2>GET / HTTP/1.1</h2><h2>Remote: 209.113.31.164 97205</h2>" +
        "<table mini:hint=\"folded;Headers\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\">\n" +
        "<tr><td valign=top><b>User-Agent:</b></td><td> Mozilla/5.0</td></tr>\n" +
        "<tr><td valign=top><b>Host:</b></td><td> echo.opera.com</td></tr>\n";
    String s2 = "<style>body {color: black; background-color: white;} tr:hover { background: yellow }</style>" +
        "<h2>GET / HTTP/1.1</h2><h2>Remote: 209.113.31.164 87429</h2>" +
        "<table mini:hint=\"folded;Headers\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\">\n" +
        "<tr><td valign=top><b>Content-Length:</b></td><td> 0</td></tr>\n" +
        "<tr><td valign=top><b>Host:</b></td><td> echo.opera.com</td></tr>\n" +
        "<tr><td valign=top><b>User-Agent:</b></td><td> Mozilla/5.0</td></tr>\n";
    assertEquals(removeTimestamp(s1), removeTimestamp(s2));
  }

  /**
   * Consistent request data.
   */
  @Disabled
  @Test
  void test200() {
    String urlHttp = "http://echo.opera.com/";
    assertEquals(removeTimestamp(http.getUrlConnection(urlHttp)), removeTimestamp(http.getHttpClient(urlHttp)));
    String urlHttps = "https://echo.opera.com/";
    assertEquals(removeTimestamp(http.getUrlConnection(urlHttps)), removeTimestamp(http.getHttpClient(urlHttps)));
  }

  @Disabled
  @Test
  void test404() {
    String url = "https://echo.opera.com/404";
    UncheckedIOException euc = assertThrows(UncheckedIOException.class, () -> http.getUrlConnection(url));
    assertEquals(FileNotFoundException.class, euc.getCause().getClass());
    assertEquals(url, euc.getCause().getMessage());
    UncheckedIOException ehc = assertThrows(UncheckedIOException.class, () -> http.getHttpClient(url));
    assertEquals(FileNotFoundException.class, ehc.getCause().getClass());
    assertEquals(url, ehc.getCause().getMessage());
  }

  @Disabled
  @Test
  void test301() {
    Predicate<String> htmlTest = s -> s.contains("<html");
    String urlHttps = "https://httpstat.us/301";
    assertTrue(htmlTest.test(http.getUrlConnection(urlHttps)));
    assertTrue(htmlTest.test(http.getHttpClient(urlHttps)));
    String urlHttp = "http://httpstat.us/301";
    assertFalse(htmlTest.test(http.getUrlConnection(urlHttp))); // http->https
    assertTrue(htmlTest.test(http.getHttpClient(urlHttp)));
  }

}
