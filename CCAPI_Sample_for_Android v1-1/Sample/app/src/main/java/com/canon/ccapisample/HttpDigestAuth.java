package com.canon.ccapisample;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class HttpDigestAuth {
    private static final String TAG = HttpDigestAuth.class.getSimpleName();
    private String mUserName;
    private String mPassword;
    private Map<String, String> mWWWAuthHeaderMap = null;
    private long mNonceCount = 0;

    HttpDigestAuth(String userName, String password){
        this.mUserName = userName;
        this.mPassword = password;
    }

    String getUserName(){
        return mUserName;
    }

    String getPassword(){
        return mPassword;
    }

    String getDigestAuthHeader(String method, String url, byte[] body, String wwwAuthHeader){
        Map<String, String> headerMap = null;
        // Parse the WWW-Authenticate header.
        if(wwwAuthHeader != null) {
            // The first authentication.
            headerMap = parseAuthHeader(wwwAuthHeader);
            mNonceCount = 1;
        }
        else{
            // Refer to the previous authentication information, if the WWW-Authenticate header is not appointed.
            headerMap = mWWWAuthHeaderMap;
        }

        Log.d(TAG, String.format("NonceCount : %d : %s", mNonceCount, url));

        String realm = headerMap.get("realm");
        String nonce = headerMap.get("nonce");
        String qop = headerMap.get("qop");
        String algorithm = headerMap.get("algorithm");
        String opaque = headerMap.get("opaque");

        URL urlParse = null;
        String uri = null;
        String clientNonce = null;
        String nonceCount = String.format("%08x", mNonceCount);
        String tmpAlgorithm = null;
        String tmpQop = null;

        try {
            urlParse = new URL(url);
            uri = urlParse.getPath();
            if(urlParse.getQuery() != null){
                uri += "?" + urlParse.getQuery();
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }

        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder responseBuilder = new StringBuilder();
        MessageDigest messageDigest = null;
        String a1 = mUserName + ":" + realm + ":" + mPassword;
        String a2 = method + ":" + uri;
        String response = null;

        // Generate a client nonce.
        try {
            byte[] nonceBytes = new byte[16];
            SecureRandom random = null;
            long seed = System.currentTimeMillis();
            random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(seed);
            random.nextBytes(nonceBytes);

            StringBuilder sb = new StringBuilder();
            for (byte d: nonceBytes) {
                sb.append(String.format("%02x", d));
            }
            clientNonce = sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Generate an algorithm.
        if(algorithm == null){
            tmpAlgorithm = "MD5";
        }
        else{
            tmpAlgorithm = algorithm.toUpperCase();
        }

        try {
            if("MD5".equals(tmpAlgorithm) || "MD5-SESS".equals(tmpAlgorithm)){
                messageDigest = MessageDigest.getInstance("MD5");
            }
            else if("SHA-256".equals(tmpAlgorithm) || "SHA-256-SESS".equals(tmpAlgorithm)){
                messageDigest = MessageDigest.getInstance("SHA-256");
            }
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Generate the A1.
        a1 = getHashString(messageDigest, a1);

        if (tmpAlgorithm.contains("-SESS")) {
            a1 += ":" + nonce + ":" + clientNonce;
            a1 = getHashString(messageDigest, a1);
        }

        // The qop determination.
        if(qop != null) {
            if ("auth".equals(qop)) {
                tmpQop = qop;
            }
            else if ("auth-int".equals(qop) && body != null){
                tmpQop = qop;
                a2 += ":" + getHashString(messageDigest, body);
            }
            else {
                String[] qopList = qop.split(",");
                if(Arrays.asList(qopList).contains("auth")){
                    tmpQop = "auth";
                }
                else if(Arrays.asList(qopList).contains("auth-int") && body != null){
                    tmpQop = "auth-int";
                    a2 += ":" + getHashString(messageDigest, body);
                }
            }
        }

        // Generate the A2.
        a2 = getHashString(messageDigest, a2);

        // Generate a response.
        responseBuilder
                .append(a1).append(":")
                .append(nonce).append(":")
        ;

        if(tmpQop != null){
            responseBuilder
                    .append(nonceCount).append(":")
                    .append(clientNonce).append(":")
                    .append(tmpQop).append(":")
            ;
        }

        responseBuilder.append(a2);
        response = getHashString(messageDigest, responseBuilder.toString());

        // Generate an authorization header.
        headerBuilder
                .append("Digest ")
                .append("username=\"").append(mUserName).append("\", ")
                .append("realm=\"").append(realm).append("\", ")
                .append("nonce=\"").append(nonce).append("\", ")
                .append("uri=\"").append(uri).append("\", ")
        ;

        if(algorithm != null) {
            headerBuilder.append("algorithm=").append(algorithm).append(", ");
        }

        if(opaque != null) {
            headerBuilder.append("opaque=\"").append(opaque).append("\", ");
        }

        if(tmpQop != null) {
            headerBuilder
                    .append("nc=").append(nonceCount).append(", ")
                    .append("qop=").append(tmpQop).append(", ")
                    .append("cnonce=\"").append(clientNonce).append("\", ")
            ;

            if(mNonceCount == 0xFFFFFFFF){
                Log.d(TAG, "Next NonceCount : FFFFFFFF -> 00000001");
                mNonceCount = 1;
            }
            else{
                mNonceCount++;
            }
        }
        headerBuilder.append("response=\"").append(response).append("\"");
        mWWWAuthHeaderMap = headerMap;
        Log.d(TAG, headerBuilder.toString());

        return headerBuilder.toString();
    }

    private Map<String, String> parseAuthHeader(String wwwAuthHeader){
        Map<String, String> headerMap = new HashMap<>();
        String[] headerList = wwwAuthHeader.trim().substring(7).split(",");
        for (String aHeaderList : headerList) {
            String[] header = aHeaderList.trim().split("=", 2);
            if (header.length == 2) {
                String key = header[0];
                String value = header[1].replace("\"", "");
                headerMap.put(key, value);
            }
        }
        return headerMap;
    }

    private String getHashString(MessageDigest messageDigest, byte[] bytes){
        String ret = null;
        byte[] hash_bytes = messageDigest.digest(bytes);

        StringBuilder sb = new StringBuilder();
        for (byte d: hash_bytes) {
            sb.append(String.format("%02x", d));
        }
        ret = sb.toString();

        return ret;
    }

    private String getHashString(MessageDigest messageDigest, String str){
        String ret = null;
        try {
            byte[] str_bytes = str.getBytes("UTF-8");
            ret = getHashString(messageDigest, str_bytes);
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
