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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class HttpGet {

  // https://techblog.willshouse.com/2012/01/03/most-common-user-agents/
  public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
      "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36";

  /**
   * java.net.http.HttpClient
   * Content-Length: 0 - JDK bug https://bugs.openjdk.org/browse/JDK-8283544
   * Host: always
   * Accept: if provided
   * User-Agent: Java-http-client/11.0.26
   * follows redirects if configured
   * @since 11
   */
  public String getHttpClient(String url) {
    try {
      HttpClient client = HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .header("User-Agent", USER_AGENT)
          .header("Accept", "text/html, image/gif, image/jpeg, */*; q=0.2") // similar to UrlConnection constant
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      int statusCode = response.statusCode();
      if (statusCode >= 400) { // copy from HttpURLConnection
        if (statusCode == 404 || statusCode == 410) {
          throw new FileNotFoundException(url);
        } else {
          throw new java.io.IOException("Server returned HTTP response code: " + statusCode + " for URL: " + url);
        }
      }
      if (statusCode != 200) throw new IllegalStateException();
      return response.body();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * java.net.URLConnection
   * User-Agent: Java/11.0.26
   * Host: always
   * Accept: text/html, image/gif, image/jpeg, * /*; q=0.2 - always, can be changed
   * Connection: keep-alive - always, can be changed to "close"
   * follows redirects http->http and https->https
   * @since 1.0
   */
  public String getUrlConnection(String url) {
    try {
      URLConnection urlConnection = new URL(url).openConnection();
      urlConnection.setRequestProperty("User-Agent", USER_AGENT);
      try (InputStream inputStream = urlConnection.getInputStream();
          Scanner scanner = new Scanner(inputStream).useDelimiter("\\Z")) {
        return scanner.next();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public String get(String url) {
    return getHttpClient(url);
  }

}
