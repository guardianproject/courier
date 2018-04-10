package info.guardianproject.securereaderinterface.ui;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.tinymission.rss.MediaContent;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import fi.iki.elonen.NanoHTTPD;
import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.securereader.SocialReader;
import info.guardianproject.securereaderinterface.App;

/**
 * Created by N-Pex on 16-02-02.
 */
public class ProxyMediaStreamServer extends NanoHTTPD {

    private static final int DEFAULT_PORT = 8090;
    public static final boolean LOGGING = false;
    public static final String LOGTAG = "ProxyMediaStreamServer";

    private static ProxyMediaStreamServer mMediaServer;
    private static int mMediaServerPort;

    private ProxyMediaStreamServer(int port) throws IOException {
        super("localhost", port);
    }

    public String getProxyUrlForUrl(String url) {
        return String.format(Locale.US, "http://localhost:%d/%s", mMediaServerPort, Base64.encodeToString(url.getBytes(), Base64.URL_SAFE));
    }

    public String getProxyUrlForMediaContent(MediaContent content) {
        return String.format(Locale.US, "http://localhost:%d/media/%s", mMediaServerPort, content.getDatabaseId());
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {

        if (uri.startsWith("/media/"))
        {
            try
            {
                String mediaId = uri.substring(7);
                File mediaFile = new File(App.getInstance().socialReader.getFileSystemDir(), SocialReader.MEDIA_CONTENT_FILE_PREFIX + mediaId);

                Response response;
                if (headers.containsKey("range"))
                {
                    if (LOGGING)
                        Log.v(LOGTAG, "Range: " + headers.get("range"));
                    SingleRangeFileInputStream fin = new SingleRangeFileInputStream(mediaFile, headers.get("range"));
                    if (fin.validRange())
                    {
                        response = new Response(Response.Status.PARTIAL_CONTENT, "audio/mpeg", fin);
                        response.addHeader("Accept-Ranges", "bytes");
                        response.addHeader("Content-Length", fin.getContentLengthHeader());
                        response.addHeader("Content-Range", fin.getContentRangeHeader());
                    }
                    else
                    {
                        response = new Response(Response.Status.RANGE_NOT_SATISFIABLE, "audio/mpeg", headers.get("range"));
                    }
                }
                else
                {
                    FileInputStream fin = new FileInputStream(mediaFile);
                    response = new Response(Response.Status.OK, "audio/mpeg", fin);
                    response.addHeader("Accept-Ranges", "bytes");
                    response.addHeader("Content-Length", "" + mediaFile.length());
                }
                response.addHeader("Connection", "Close");
                return response;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else {
            uri = uri.substring(1);
            uri = new String(Base64.decode(uri, Base64.URL_SAFE));

            if (LOGGING)
                Log.v(LOGTAG, "Request for: " + uri);

            try {
                SocialReader socialReader = App.getInstance().socialReader;
                HttpClient httpClient = socialReader.getHttpClient();

                HttpGet httpGet = new HttpGet(uri);
                httpGet.setHeader("Accept-Encoding", "gzip");
                httpGet.setHeader("Host", Uri.parse(uri).getHost());
                String userAgent = headers.get("user-agent");
                if (TextUtils.isEmpty(userAgent))
                    userAgent = "stagefright/1.2 (Linux;Android 5.1.1)";
                httpGet.setHeader("User-Agent", userAgent);
                httpGet.setHeader("Connection", "Keep-Alive");

                if (LOGGING) {
                    for (Header header : httpGet.getAllHeaders()) {
                        Log.v(LOGTAG, String.format("Request header: %s -> %s", header.getName(), header.getValue()));
                    }
                }

                HttpResponse response = httpClient.execute(httpGet);

                if (LOGGING)
                    Log.v(LOGTAG, "Response: " + response.toString());

                if (response.getStatusLine().getStatusCode() == 200) {
                    if (LOGGING)
                        Log.v(LOGTAG, "Response Code is good");

                    InputStream is = response.getEntity().getContent();
                    Response ourResponse = new Response(Response.Status.OK, "audio/mpeg", is) {

                        private HttpResponse mOriginalResponse;

                        Response init(HttpResponse originalResponse) {
                            mOriginalResponse = originalResponse;
                            return this;
                        }

                        @Override
                        protected void send(OutputStream outputStream) {
                            String mime = getMimeType();
                            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

                            try {
                                if (getStatus() == null) {
                                    throw new Error("sendResponse(): Status can't be null.");
                                }
                                PrintWriter pw = new PrintWriter(outputStream);
                                pw.print("HTTP/1.1 " + getStatus().getDescription() + " \r\n");

                                if (mime != null) {
                                    pw.print("Content-Type: " + mime + "\r\n");
                                }

                                if (getHeader("Date") == null) {
                                    pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
                                }

                                for (Header header : mOriginalResponse.getAllHeaders()) {
                                    pw.print(header.getName() + ": " + header.getValue() + "\r\n");
                                }

                                if (mOriginalResponse.getFirstHeader("connection") == null)
                                    pw.print("Connection: keep-alive\r\n");

                                pw.print("\r\n");
                                pw.flush();

                                Header transferEncoding = mOriginalResponse.getLastHeader("Transfer-Encoding");
                                if (transferEncoding != null && "chunked".equals(transferEncoding.getValue())) {
                                    int BUFFER_SIZE = 16 * 1024;
                                    byte[] CRLF = "\r\n".getBytes();
                                    byte[] buff = new byte[BUFFER_SIZE];
                                    int read;
                                    while ((read = mOriginalResponse.getEntity().getContent().read(buff)) > 0) {
                                        outputStream.write(String.format("%x\r\n", read).getBytes());
                                        outputStream.write(buff, 0, read);
                                        outputStream.write(CRLF);
                                    }
                                    outputStream.write("0\r\n\r\n".getBytes());
                                } else {
                                    byte[] buffer = new byte[1024];
                                    int read;
                                    while ((read = mOriginalResponse.getEntity().getContent().read(buffer)) > -1) {
                                        outputStream.write(buffer, 0, read);
                                    }
                                }
                                outputStream.flush();
                                safeClose(getData());
                            } catch (IOException ioe) {
                                if (LOGGING)
                                    Log.d(LOGTAG, "Exception during write: " + ioe.toString());
                            }
                        }

                        private void safeClose(Closeable closeable) {
                            if (closeable != null) {
                                try {
                                    closeable.close();
                                } catch (IOException ignored) {
                                }
                            }
                        }
                    }.init(response);

                    for (Header header : response.getAllHeaders()) {
                        if (LOGGING)
                            Log.v(LOGTAG, String.format("Response header: %s -> %s", header.getName(), header.getValue()));
                        ourResponse.addHeader(header.getName(), header.getValue());
                    }
                    return ourResponse;
                } else {
                    if (LOGGING)
                        Log.v(LOGTAG, "Response Code: " + response.getStatusLine().getStatusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static class MediaServerFactory {
        private final Thread mThread;
        private boolean mCreating = true;
        private final Object mLock;

        MediaServerFactory() {
            mLock = new Object();
            mThread = new Thread(mRunnable);
        }

        ProxyMediaStreamServer createMediaServer() {
            mThread.start();
            synchronized (mLock) {
                while (mCreating) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            return mMediaServer;
        }

        Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (mLock) {
                    for (int i = 0; i < 100; i++) {
                        try {
                            mMediaServerPort = DEFAULT_PORT + i;
                            mMediaServer = new ProxyMediaStreamServer(mMediaServerPort);
                            mMediaServer.start();
                            break;
                        } catch (IOException e) {
                            if (LOGGING)
                                Log.d(LOGTAG, "Failed to start service on port " + mMediaServerPort);
                        }
                    }
                    mCreating = false;
                    mLock.notifyAll();
                }
            }
        };

    }

    static public ProxyMediaStreamServer createMediaServer() {
        if (mMediaServer == null) {
            MediaServerFactory factory = new MediaServerFactory();
            mMediaServer = factory.createMediaServer();
        }
        return mMediaServer;
    }
}
