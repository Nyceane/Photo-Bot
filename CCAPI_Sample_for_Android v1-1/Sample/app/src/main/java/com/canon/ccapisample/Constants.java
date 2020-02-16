package com.canon.ccapisample;

import java.util.HashMap;
import java.util.Map;

public final class Constants {
    static final class CCAPI {
        static final String SUPPORTED_API_VERSION[] = { "ver100", "ver110" };

        final class Method {
            static final String GET = "GET";
            static final String PUT = "PUT";
            static final String POST = "POST";
            static final String DELETE = "DELETE";
        }

        final class Key{
            static final String DEVICESTATUS_STORAGE = "devicestatus/storage";
            static final String FUNCTIONS_DATETIME = "functions/datetime";
            static final String FUNCTIONS_WIFISETTING = "functions/wifisetting";
            static final String FUNCTIONS_CARDFORMAT = "functions/cardformat";
            static final String FUNCTIONS_WIFICONNECTION = "functions/wificonnection";
            static final String FUNCTIONS_NETWORKCONNECTION = "functions/networkconnection";
            static final String SHOOTING_CONTROL_SHOOTINGMODE = "shooting/control/shootingmode";
            static final String SHOOTING_CONTROL_SHUTTERBUTTON = "shooting/control/shutterbutton";
            static final String SHOOTING_CONTROL_SHUTTERBUTTON_MANUAL = "shooting/control/shutterbutton/manual";
            static final String SHOOTING_CONTROL_MOVIEMODE = "shooting/control/moviemode";
            static final String SHOOTING_CONTROL_RECBUTTON = "shooting/control/recbutton";
            static final String SHOOTING_CONTROL_ZOOM = "shooting/control/zoom";
            static final String SHOOTING_CONTROL_DRIVEFOCUS = "shooting/control/drivefocus";
            static final String SHOOTING_CONTROL_AF = "shooting/control/af";
            static final String SHOOTING_LIVEVIEW = "shooting/liveview";
            static final String SHOOTING_LIVEVIEW_FLIP = "shooting/liveview/flip";
            static final String SHOOTING_LIVEVIEW_FLIPDETAIL = "shooting/liveview/flipdetail";
            static final String SHOOTING_LIVEVIEW_SCROLL = "shooting/liveview/scroll";
            static final String SHOOTING_LIVEVIEW_SCROLLDETAIL = "shooting/liveview/scrolldetail";
            static final String SHOOTING_LIVEVIEW_RTPSESSIONDESC = "shooting/liveview/rtpsessiondesc";
            static final String SHOOTING_LIVEVIEW_RTP = "shooting/liveview/rtp";
            static final String SHOOTING_LIVEVIEW_ANGLEINFORMATION = "shooting/liveview/angleinformation";
            static final String SHOOTING_LIVEVIEW_AFFRAMEPOSITION = "shooting/liveview/afframeposition";
            static final String EVENT_POLLING = "event/polling";
            static final String EVENT_MONITORING = "event/monitoring";
        }

        final class Field {
            static final String MESSAGE = "message";
            static final String URL_LIST = "url";
            static final String VALUE = "value";
            static final String ABILITY = "ability";
            static final String ACTION = "action";
            static final String STATUS = "status";
            static final String MIN = "min";
            static final String MAX = "max";
            static final String STEP = "step";

            static final String MOVIEMODE = "moviemode";
            static final String RECBUTTON = "recbutton";
            static final String LIVEVIEW = "liveview";
            static final String LIVEVIEWSIZE = "liveviewsize";
            static final String CAMERADISPLAY = "cameradisplay";

            static final String HISTOGRAM = "histogram";
            static final String AF_FRAME = "afframe";
            static final String IMAGE = "image";
            static final String VISIBLE = "visible";
            static final String ZOOM = "zoom";

            static final String POSITION_X = "positionx";
            static final String POSITION_Y = "positiony";
            static final String POSITION_WIDTH = "positionwidth";
            static final String POSITION_HEIGHT = "positionheight";
            static final String MAGNIFICATION = "magnification";

            static final String SELECT = "select";
            static final String X = "x";
            static final String Y = "y";
            static final String WIDTH = "width";
            static final String HEIGHT = "height";

            static final String CONTENTS_NUMBER = "contentsnumber";
            static final String PAGE_NUMBER = "pagenumber";
            static final String ADDED_CONTENTS = "addedcontents";
            static final String DELETED_CONTENTS = "deletedcontents";
            static final String STORAGE = "storage";
            static final String STORAGE_LIST = "storagelist";
            static final String STORAGE_NAME = "name";
            static final String STORAGE_URL = "url";
            static final String STORAGE_PATH = "path";

            static final String DATETIME = "datetime";
            static final String DST = "dst";

            static final String EXPOSURE_INCREMENTS = "exposureincrements";

            static final String WIFI_SETTINGS = "wifisetting";
            static final String WIFI_SETTINGS_SET_1 = "wifisetting/set1";
            static final String WIFI_SETTINGS_SET_2 = "wifisetting/set2";
            static final String WIFI_SETTINGS_SET_3 = "wifisetting/set3";

            static final String SSID = "ssid";
            static final String METHOD = "method";
            static final String CHANNEL = "channel";
            static final String AUTHENTICATION = "authentication";
            static final String ENCRYPTION = "encryption";
            static final String KEYINDEX = "keyindex";
            static final String PASSWORD = "password";
            static final String IPADDRESSSET = "ipaddressset";
            static final String IPADDRESS = "ipaddress";
            static final String SUBNETMASK = "subnetmask";
            static final String GATEWAY = "gateway";

            static final String CARD_FORMAT = "cardformat";
            static final String CONTENTS_URL = "url";
            static final String CONTENTS_PATH = "path";
        }

        final class Value {
            static final String ON = "on";
            static final String OFF = "off";

            static final String MODE_NOT_SUPPORTED = "Mode not supported";

            static final String INFRASTRUCTURE = "infrastructure";
            static final String CAMERAAP = "cameraap";
            static final String OPEN = "open";
            static final String SHAREDKEY = "sharedkey";
            static final String WPAWPA2PSK = "wpawpa2psk";
            static final String NONE = "none";
            static final String WEP = "wep";
            static final String TKIPAES = "tkipaes";
            static final String AES = "aes";
            static final String AUTO = "auto";
            static final String MANUAL = "manual";

            static final String ROTATE = "rotate";
            static final String PROTECT = "protect";
            static final String ARCHIVE = "archive";
            static final String RATING = "rating";
        }

        static final Map<String, String> UNIT_MAP = new HashMap<String, String>() {
            {  put("remainingtime", "sec");  }
            {  put("filesize", "byte");  }
            {  put("playtime", "sec");  }
        };
    }
    public enum RequestCode {
        ACT_WEB_API,
        GET_DEVICESTATUS_STORAGE,
        GET_FUNCTIONS_DATETIME,
        PUT_FUNCTIONS_DATETIME,
        GET_FUNCTIONS_WIFISETTINGINFORMATION,
        PUT_FUNCTIONS_WIFISETTINGINFORMATION,
        DELETE_FUNCTIONS_WIFISETTINGINFORMATION,
        POST_FUNCTIONS_CARDFORMAT,
        POST_FUNCTIONS_WIFICONNECTION,
        POST_FUNCTIONS_NETWORKCONNECTION,
        GET_SHOOTING_CONTROL_SHOOTINGMODE,
        POST_SHOOTING_CONTROL_SHOOTINGMODE,
        POST_SHOOTING_CONTROL_SHUTTERBUTTON,
        POST_SHOOTING_CONTROL_SHUTTERBUTTON_MANUAL,
        POST_SHOOTING_CONTROL_RECBUTTON,
        GET_SHOOTING_CONTROL_MOVIEMODE,
        POST_SHOOTING_CONTROL_MOVIEMODE,
        GET_SHOOTING_CONTROL_ZOOM,
        POST_SHOOTING_CONTROL_ZOOM,
        POST_SHOOTING_CONTROL_DRIVEFOCUS,
        POST_SHOOTING_CONTROL_AF,
        POST_SHOOTING_LIVEVIEW,
        GET_SHOOTING_LIVEVIEW_FLIP,
        GET_SHOOTING_LIVEVIEW_FLIPDETAIL,
        GET_SHOOTING_LIVEVIEW_SCROLL,
        DELETE_SHOOTING_LIVEVIEW_SCROLL,
        GET_SHOOTING_LIVEVIEW_SCROLLDETAIL,
        DELETE_SHOOTING_LIVEVIEW_SCROLLDETEIL,
        GET_SHOOTING_LIVEVIEW_RTPSESSIONDESC,
        POST_SHOOTING_LIVEVIEW_RTP,
        POST_SHOOTING_LIVEVIEW_ANGLEINFORMATION,
        PUT_SHOOTING_LIVEVIEW_AFFRAMEPOSITION,
        GET_EVENT_POLLING,
        DELETE_EVENT_POLLING,
        GET_EVENT_MONITORING,
        DELETE_EVENT_MONITORING,
    }
    enum EventMethod{
        POLLING,
        POLLING_CONTINUE,
        MONITORING
    }
    enum LiveViewMethod{
        FLIP,
        FLIPDETAIL,
        SCROLL,
        SCROLLDETAIL
    }
    enum LiveViewKind{
        IMAGE,
        IMAGE_AND_INFO,
        INFO
    }
    enum WifiMonitoringResult{
        CONNECTION,
        DISCONNECTION,
        INTERRUPT
    }
    final class Settings {
        final class FileName{
            static final String SETTINGS = "settings.json";
        }
        final class Key{
            static final String SHOOTING_SETTINGS = "ShootingSettings";
            static final String DEVICE_INFORMATION = "DeviceInformation";
            static final String CAMERA_FUNCTIONS = "Functions";
            static final String NAME = "Name";
            static final String KEY = "Key";
        }
    }
    static final class RemoteCapture{
        static final String LV_SIZE_OFF = "off";
        static final String LV_SIZE_SMALL = "small";
        static final String LV_SIZE_MEDIUM = "medium";
        static final String LV_DISPLAY_ON = "on";
        static final String LV_DISPLAY_OFF = "off";
        static final String LV_DISPLAY_KEEP = "keep";
        static final String LV_OFF_ON = LV_SIZE_OFF + "/" + LV_DISPLAY_ON;
        static final String LV_OFF_OFF = LV_SIZE_OFF + "/" + LV_DISPLAY_OFF;
        static final String LV_OFF_KEEP = LV_SIZE_OFF + "/" + LV_DISPLAY_KEEP;
        static final String LV_SMALL_ON = LV_SIZE_SMALL + "/" + LV_DISPLAY_ON;
        static final String LV_SMALL_OFF = LV_SIZE_SMALL + "/" + LV_DISPLAY_OFF;
        static final String LV_SMALL_KEEP = LV_SIZE_SMALL + "/" + LV_DISPLAY_KEEP;
        static final String LV_MIDDLE_ON = LV_SIZE_MEDIUM + "/" + LV_DISPLAY_ON;
        static final String LV_MIDDLE_OFF = LV_SIZE_MEDIUM + "/" + LV_DISPLAY_OFF;
        static final String LV_MIDDLE_KEEP = LV_SIZE_MEDIUM + "/" + LV_DISPLAY_KEEP;
        static final String[] LV_ARRAY = {LV_SMALL_ON, LV_SMALL_OFF, LV_SMALL_KEEP, LV_MIDDLE_ON, LV_MIDDLE_OFF, LV_MIDDLE_KEEP, LV_OFF_ON, LV_OFF_OFF, LV_OFF_KEEP};
    }
    static final class ContentsViewer{
        static final String SAVE_ORIGINAL = "original";
        static final String SAVE_DISPLAY = "display";
        static final String SAVE_EMBEDDED = "embedded";
        static final String[] SAVE_ARRAY = {SAVE_ORIGINAL, SAVE_DISPLAY, SAVE_EMBEDDED};
    }
}
