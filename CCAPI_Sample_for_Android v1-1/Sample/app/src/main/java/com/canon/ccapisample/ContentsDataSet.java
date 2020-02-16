package com.canon.ccapisample;

import java.io.Serializable;
import java.util.regex.Pattern;

class ContentsDataSet implements Serializable {
    enum Kind{
        IMAGE,
        AUDIO,
        MOVIE,
        DIR,
        UNKNOWN
    }

    private String mName;
    private String mUrl;
    private Kind mKind;
    private String mFolder;
    private String mInfo;

    ContentsDataSet(String url){
        String[] url_split = url.split("/");
        String name  = url_split[url_split.length - 1];
        this.mName = name;

        if(url.contains(WebAPI.getInstance().getUrl())){
            this.mUrl = url;
        }else {
            this.mUrl = WebAPI.getInstance().getUrl() + url.replace("/ccapi", "");
        }

        if(name.contains(".")) {
            String folder = url_split[url_split.length - 2];
            String[] name_split = name.split(Pattern.quote("."));
            String kind = name_split[name_split.length - 1];

            switch(kind.toUpperCase()){
                case "JPG":
                case "JPEG":
                case "HIF":
                    this.mKind = Kind.IMAGE;
                    break;
                case "WAV":
                    this.mKind = Kind.AUDIO;
                    break;
                case "MP4":
                    this.mKind = Kind.MOVIE;
                    break;
                case "CR2":
                case "CR3":
                case "MOV":
                case "CRM":
                default:
                    this.mKind = Kind.UNKNOWN;
                    break;
            }

            this.mFolder = folder;
        }
        else{
            this.mKind = Kind.DIR;
            this.mFolder = null;
        }
        this.mInfo = null;
    }

    String getName() {
        return mName;
    }

    String getNameNoExtension(){
        String filename = mName;
        int pos = filename.lastIndexOf(".");

        if(pos != -1 && pos != 0){
            filename = filename.substring(0, pos);
        }

        return filename;
    }

    String getExtension(){
        String ext = "";
        String filename = mName;
        if(filename.contains(".")) {
            String[] name_split = filename.split(Pattern.quote("."));
            ext = name_split[name_split.length - 1];
            ext = ext.toLowerCase();
        }
        return ext;
    }

    String getUrl() {
        return mUrl;
    }

    Kind getKind() {
        return mKind;
    }

    String getFolder() {
        return mFolder;
    }

    String getInfo() {
        return mInfo;
    }

    void setInfo(String info) {
        mInfo = info;
    }
}
