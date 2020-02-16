package com.canon.ccapisample;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.canon.ccapisample.Constants.CCAPI.Method.POST;
import static com.canon.ccapisample.Constants.CCAPI.Method.PUT;

class HttpCommunication {
    private static final String TAG = HttpCommunication.class.getSimpleName();
    private static final int HTTP_BUFFER_SIZE = 1024;
    private String mMethod;
    private String mUrl;
    private byte[] mRequestBody;
    private File mUploadFile;
    private int mTimeout;
    private Boolean mIsStopReadResponse = false;

    HttpCommunication(String method, String url, byte[] body, File uploadFile, int timeout) {
        this.mMethod = method;
        this.mUrl = url;
        this.mRequestBody = body;
        this.mUploadFile = uploadFile;
        this.mTimeout = timeout;
    }

    String getRequestUrl(){
        return mUrl;
    }

    void cancelReadResponse(){
        mIsStopReadResponse = true;
    }

    HttpResultDataSet sendRequest(String authHeader, HttpProgressListener httpProgressListener){
        int responseCode = 0;
        String responseMessage = null;
        Map<String, String> responseHeaderMap = new HashMap<>();
        byte[] bytesResponseBody = null;
        URL url = null;
        HttpURLConnection urlConnection = null;
        OutputStream outputStream = null;

        Log.d(TAG, "Request : " + mMethod + " " + mUrl);

        try {
            url = new URL(mUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(mTimeout);
            urlConnection.setReadTimeout(mTimeout);
            urlConnection.setRequestMethod(mMethod);
            urlConnection.setRequestProperty( "Accept-Encoding", "" );

            // Give an Authorization header for continuation of authenticated state,
            // if it is appointed.
            if(authHeader != null){
                urlConnection.setRequestProperty("Authorization", authHeader);
            }

            if(mMethod.equals(PUT) || mMethod.equals(POST)){
                urlConnection.setDoOutput(true);
                if (mUploadFile != null){
                    urlConnection.setFixedLengthStreamingMode(mUploadFile.length());
                }
            }

            urlConnection.connect();

            // Send the request body.
            if(mUploadFile != null){
                outputStream = urlConnection.getOutputStream();
                uploadFile(outputStream, httpProgressListener);
            }
            else if(mRequestBody != null) {
                outputStream = urlConnection.getOutputStream();
                outputStream.write(mRequestBody);
            }

            Log.d(TAG, "Request : " + mMethod + " " + mUrl + " Complete.");

            // Get the response line.
            responseCode = urlConnection.getResponseCode();
            responseMessage = urlConnection.getResponseMessage();

            if(responseCode != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "ResponseCode : " + mMethod + " " + mUrl);
                Log.d(TAG, String.valueOf(responseCode));
                Log.d(TAG, responseMessage);
            }

            Log.d(TAG, "Response(" + String.valueOf(responseCode) + ") : " + mMethod + " " + mUrl);

            // Get the response header.
            int i = 0;
            while (urlConnection.getHeaderField(i) != null) {
                Log.d(TAG, urlConnection.getHeaderFieldKey(i) + ":" + urlConnection.getHeaderField(i));
                responseHeaderMap.put(urlConnection.getHeaderFieldKey(i), urlConnection.getHeaderField(i));
                i++;
            }

            long contentLength = 0;

            // Get the Content-Length.
            if(responseHeaderMap.containsKey("Content-Length")){
                String str = responseHeaderMap.get("Content-Length");
                try {
                    contentLength = Long.parseLong(str);
                }
                catch(NumberFormatException e){
                    e.printStackTrace();
                }
            }

            Log.d(TAG, String.format("%d", contentLength));

            // Get the response body.
            if(contentLength > 0) {
                try {
                    bytesResponseBody = readBytes(urlConnection.getInputStream(), contentLength, httpProgressListener);

                    if(urlConnection.getContentType().contains("text") || urlConnection.getContentType().contains("json")) {
                        Log.d(TAG, new String(bytesResponseBody, "UTF-8"));
                    }
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                    bytesResponseBody = readBytes(urlConnection.getErrorStream(), contentLength);
                    Log.d(TAG, new String(bytesResponseBody, "UTF-8"));
                }
            }
            Log.d(TAG, "Response(" + String.valueOf(responseCode) + ") : " + mMethod + " " + mUrl+ " Complete.");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null){
                urlConnection.disconnect();
            }
        }
        return new HttpResultDataSet(responseCode, responseMessage, responseHeaderMap, bytesResponseBody, mIsStopReadResponse);
    }

    private void uploadFile(OutputStream outputStream, HttpProgressListener httpProgressListener) throws IOException{
        InputStream fileInputStream = new FileInputStream(mUploadFile);
        byte[] buffer = new byte[HTTP_BUFFER_SIZE];
        int length = (int) mUploadFile.length();
        int currentSize = 0;

        if(httpProgressListener != null){
            httpProgressListener.onHttpProgressing(length, 0, new byte[]{});
        }

        try {
            while (true) {
                int readByte = fileInputStream.read(buffer);
                if (readByte < 0) {
                    break;
                }

                currentSize += readByte;
                outputStream.write(buffer, 0, readByte);

                if (httpProgressListener != null) {
                    httpProgressListener.onHttpProgressing(length, currentSize, buffer);
                }

                if (length <= currentSize) {
                    break;
                }
            }
        }
        finally {
            fileInputStream.close();
        }
    }

    private byte[] readBytes(InputStream inputStream, long length) throws IOException{
        return readBytes(inputStream, length, null);
    }

    private byte[] readBytes(InputStream inputStream, long length, HttpProgressListener httpProgressListener) throws IOException{
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[HTTP_BUFFER_SIZE];
        byte[] ret = null;
        long currentSize = 0;

        if(httpProgressListener != null){
            httpProgressListener.onHttpProgressing(100, 0, new byte[]{});
        }

        try {
            while (!mIsStopReadResponse) {
                int size = buffer.length;
                if(buffer.length > length - currentSize){
                    size = (int)(length - currentSize);
                }

                int readByte = inputStream.read(buffer, 0, size);
                if (readByte < 0) {
                    break;
                }

                currentSize += readByte;

                if(httpProgressListener != null){
                    int progress = (int)((float)currentSize / (float)length * 100);
                    ByteArrayOutputStream currentArrayOutputStream = new ByteArrayOutputStream();
                    currentArrayOutputStream.write(buffer, 0, readByte);
                    httpProgressListener.onHttpProgressing(100, progress, currentArrayOutputStream.toByteArray());
                    currentArrayOutputStream.close();
                    Log.d(TAG, String.format("onHttpProgressing : %d / %d", currentSize, length));
                }
                else{
                    byteArrayOutputStream.write(buffer, 0, readByte);
                }

                if (length <= currentSize) {
                    break;
                }
            }
            ret = byteArrayOutputStream.toByteArray();
        }
        finally {
            byteArrayOutputStream.close();
        }
        return ret;
    }

    private String readLine(InputStream inputStream) throws IOException{
        StringBuilder builder = new StringBuilder();
        int readCount = 0;
        Boolean findLine = true;
        while (true) {
            char ch = (char) inputStream.read();
            if (ch == '\r') {
                ch = (char) inputStream.read();
                if (ch == '\n') {
                    break;
                }
                else {
                    builder.append(ch);
                }
            }
            else {
                builder.append(ch);
            }
            readCount++;

            if(readCount > 256){
                findLine = false;
                break;
            }
        }

        if(!findLine) {
            Log.d(TAG, mUrl + " : not find line.");
            throw new IOException();
        }

        return builder.toString();
    }

    HttpResultDataSet getChunkResponse(String authHeader, ChunkResultListener chunkResultListener){
        Map<String, String> responseHeaderMap = new HashMap<>();
        InputStream inputStream = null;
        Socket socket = null;
        PrintWriter printWriter = null;
        HttpResultDataSet httpResultDataSet = null;
        int responseCode = 0;
        String responseMessage = null;

        try {
            String line = "";
            URL url = new URL(mUrl);

            socket = new Socket(url.getHost(), url.getPort());
            socket.setSoTimeout(mTimeout);

            inputStream = socket.getInputStream();
            printWriter = new PrintWriter(socket.getOutputStream());

            Log.d(TAG, "Request : " + mMethod + " " + mUrl);

            // Send the request line.
            printWriter.print(mMethod + " " + url.getFile() + " HTTP/1.1\r\n");

            Log.d(TAG, mMethod + " " + url.getFile() + " HTTP/1.1\n");

            // Send the request header.
            printWriter.print("HOST: " + url.getHost() + "\r\n");
            printWriter.print("Accept-Encoding: gzip\r\n");

            // Give an Authorization header for continuation of authenticated state,
            // if it is appointed.
            if(authHeader != null){
                printWriter.print("Authorization: " + authHeader + "\r\n");
            }

            printWriter.print("\r\n");
            printWriter.flush();

            Log.d(TAG, "HOST: " + url.getHost() + "\n");
            Log.d(TAG, "Accept-Encoding: gzip\n");

            // Get the request line.
            while (true) {
                line = readLine(inputStream);
                if (line.startsWith("HTTP/1.")) {
                    Log.d(TAG, "ResponseCode : " + mMethod + " " + mUrl);
                    Log.d(TAG, line);

                    Pattern pattern = Pattern.compile("\\s(\\d+)\\s(.+)");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find() && matcher.groupCount() == 2) {
                        responseCode = Integer.parseInt(matcher.group(1));
                        responseMessage = matcher.group(2);

                        if (responseCode != 100) {
                            break;
                        }
                    }
                }
            }

            // Get the response header.
            line = readLine(inputStream);
            Log.d(TAG, line);
            while (!line.isEmpty() && line.contains(":")) {
                String[] header = line.split(":");
                responseHeaderMap.put(header[0].trim(), header[1].trim());
                line = readLine(inputStream);
                Log.d(TAG, line);
            }

            if(responseCode == 200) {
                // Read the chunk.
                httpResultDataSet = readChunk(chunkResultListener, inputStream, responseCode, responseMessage, responseHeaderMap);
            }
            else{
                long contentLength = 0;
                // Get the Content-Length.
                if(responseHeaderMap.containsKey("Content-Length")){
                    String str = responseHeaderMap.get("Content-Length");
                    try {
                        contentLength = Long.parseLong(str);
                    }
                    catch(NumberFormatException e){
                        e.printStackTrace();
                    }
                }

                if(contentLength != 0) {
                    byte[] bytesResponseBody = readBytes(inputStream, contentLength);
                    Log.d(TAG, new String(bytesResponseBody, "UTF-8"));
                    httpResultDataSet = new HttpResultDataSet(responseCode, responseMessage, responseHeaderMap, bytesResponseBody);
                }
                else{
                    httpResultDataSet = new HttpResultDataSet(responseCode, responseMessage, responseHeaderMap, null);
                }
            }
        }
        catch (SocketTimeoutException e){
            e.printStackTrace();
            httpResultDataSet = new HttpResultDataSet(0, "", responseHeaderMap, null);
            Log.d(TAG, "Timeout : " + mMethod + " " + mUrl);
        }
        catch (IOException e) {
            e.printStackTrace();
            httpResultDataSet = new HttpResultDataSet(0, "", responseHeaderMap, null);
        }
        finally {
            if(printWriter != null){
                printWriter.close();
            }

            if(inputStream != null){
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(socket != null){
                try {
                    socket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return httpResultDataSet;
    }

    private HttpResultDataSet readChunk(ChunkResultListener chunkResultListener, InputStream inputStream, int responseCode, String responseMessage, Map<String, String> responseHeaderMap) throws IOException{
        byte[] bytesResponseBody = null;
        String line = "";

        // Chunk loop
        while(!mIsStopReadResponse) {
            int size = 0;

            Log.d(TAG, mUrl + " : chunk loop begin.");
            // Size
            line = readLine(inputStream);
            Log.d(TAG, mUrl + " : " + line);

            try {
                size = Integer.parseInt(line, 16);
                Log.d(TAG, mUrl + " : " + String.valueOf(size));
            }
            catch(NumberFormatException e){
                e.printStackTrace();
                Log.d(TAG, mUrl + " : chunk size is not Integer.");
            }

            if (size != 0) {
                // Data
                bytesResponseBody = readBytes(inputStream, size);

                // \r\n
                readBytes(inputStream, 2);

                // Callback
                if (!chunkResultListener.onChunkResult(bytesResponseBody)) {
                    Log.d(TAG, mUrl + " : onChunkResult return false.");
                    break;
                }
            }
            else {
                Log.d(TAG, mUrl + " : chunk size zero.");
                break;
            }

            Log.d(TAG, mUrl + " : chunk loop end.");
        }
        return new HttpResultDataSet(responseCode, responseMessage, responseHeaderMap, null, mIsStopReadResponse);
    }
}
