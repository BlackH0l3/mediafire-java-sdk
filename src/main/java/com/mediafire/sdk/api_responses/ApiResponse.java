package com.mediafire.sdk.api_responses;

public class ApiResponse {
    private String action;
    private String message;
    private String result;
    private String error;
    private String current_api_version;
    private String new_key;

    public final String getAction() {
        return action;
    }

    public final String getMessage() {
        return message;
    }

    public final int getError() {
        int intValueOfError;
        if (error == null) {
            intValueOfError = 0;
        } else {
            intValueOfError = Integer.valueOf(error);
        }
        return intValueOfError;
    }

    public final ResponseCode getErrorCode() {
        return ResponseCode.fromInt(getError());
    }

    public final String getResult() {
        return result;
    }

    public final String getCurrentApiVersion() {
        return current_api_version;
    }

    public final boolean hasError() {
        return error != null;
    }

    public boolean needNewKey() {
        return new_key != null && "yes".equals(new_key);
    }

    public enum ResponseCode {
        NO_ERROR(0),
        ERROR_DIRECT_LINK_INTERNAL_ERROR(40),
        ERROR_DIRECT_LINK_ACCOUNT_SUSPENDED(41),
        ERROR_DIRECT_LINK_DMCA_BLOCKED(42),
        ERROR_DIRECT_LINK_DMCA_THROTTLE_LIMIT_REACHED(43),
        ERROR_INTERNAL(100),
        ERROR_MISSING_KEY(102),
        ERROR_INVALID_KEY(103),
        ERROR_MISSING_TOKEN(104),
        ERROR_INVALID_TOKEN(105),
        ERROR_CHANGE_EXTENSION(106),
        ERROR_INVALID_CREDENTIALS(107),
        ERROR_INVALID_USER(108),
        ERROR_INVALID_APPID(109),
        ERROR_INVALID_QUICKKEY(110),
        ERROR_MISSING_QUICKKEY(111),
        ERROR_INVALID_FOLDERKEY(112),
        ERROR_MISSING_FOLDERKEY(113),
        ERROR_ACCESS_DENIED(114),
        ERROR_FOLDER_PATH_CONFLICT(115),
        ERROR_INVALID_DATE(116),
        ERROR_MISSING_FOLDERNAME(117),
        ERROR_INVALID_FILENAME(118),
        ERROR_NO_MF_EMAIL(119),
        ERROR_EMAIL_TAKEN(120),
        ERROR_EMAIL_REJECTED(121),
        ERROR_EMAIL_MISFORMATTED(122),
        ERROR_PASSWORD_MISFORMATTED(123),
        ERROR_API_VERSION_MISSING(124),
        ERROR_OLD_API_VERSION(125),
        ERROR_API_CALL_DEPRECATED(126),
        ERROR_INVALID_SIGNATURE(127),
        ERROR_MISSING_PARAMS(128),
        ERROR_INVALID_PARAMS(129),
        ERROR_NON_PRO_LIMIT_REACHED(130),
        ERROR_ADD_OWNED_FOLDER(131),
        ERROR_REMOVE_OWNED_FOLDER(132),
        ERROR_ADD_ANON_FOLDER(133),
        ERROR_NOT_DMCA_USER(134),
        ERROR_DMCA_IP_OUT_OF_RANGE(135),
        ERROR_DMCA_LIMIT_EXCEEDED(136),
        ERROR_CONTACT_ALREADY_EXISTS(137),
        ERROR_CONTACT_DOES_NOT_EXIST(138),
        ERROR_CONTACT_GROUP_EXISTS(139),
        ERROR_UNKNOWN_CONTACT_GROUP(140),
        ERROR_UNKNOWN_DEVICE(141),
        ERROR_INVALID_FILE_TYPE(142),
        ERROR_FILE_ALREADY_EXISTS(143),
        ERROR_FOLDER_ALREADY_EXISTS(144),
        ERROR_APPLICATION_DISABLED(145),
        ERROR_APPLICATION_SUSPENDED(146),
        ERROR_ZIP_MULTIPLE_OWNERS(147),
        ERROR_ZIP_NON_PRO_DOWNLOAD(148),
        ERROR_ZIP_OWNER_NOT_PRO(149),
        ERROR_ZIP_FILE_TOO_BIG(150),
        ERROR_ZIP_NO_FILES_SELECTED(151),
        ERROR_ZIP_NO_FILES_ZIPPED(152),
        ERROR_ZIP_TOTAL_SIZE_TOO_BIG(153),
        ERROR_ZIP_NUM_FILES_EXCEEDED(154),
        ERROR_ZIP_OWNER_INSUFFICIENT_BANDWIDTH(155),
        ERROR_ZIP_REQUESTER_INSUFFICIENT_BANDWIDTH(156),
        ERROR_ZIP_ALL_INSUFFICIENT_BANDWIDTH(157),
        ERROR_FILE_EXISTS(158),
        ERROR_FOLDER_EXISTS(159),
        ERROR_INVALID_ACCEPTANCE_TOKEN(160),
        ERROR_USER_MUST_ACCEPT_TOS(161),
        ERROR_LIMIT_EXCEEDED(162),
        ERROR_ACCESS_LIMIT_REACHED(163),
        ERROR_DMCA_ALREADY_REPORTED(164),
        ERROR_DMCA_ALREADY_REMOVED(165),
        ERROR_ADD_PRIVATE_FOLDER(166),
        ERROR_FOLDER_DEPTH_LIMIT(167),
        ERROR_INVALID_PRODUCT_ID(168),
        ERROR_UPLOAD_FAILED(169),
        ERROR_TARGET_PLAN_NOT_IN_THE_SAME_CLASS(170),
        ERROR_BIZ_PLAN_RESTRICTION(171),
        ERROR_EXPIRATION_DATE_RESTRICTION(172),
        ERROR_NOT_PREMIUM_USER(173),
        ERROR_INVALID_URL(174),
        ERROR_INVALID_UPLOAD_KEY(175),
        ERROR_STORAGE_LIMIT_RESTRICTION(176),
        ERROR_DUPLICATE_ENTRY(177),
        ERROR_PRODUCT_ID_MATCH(178),
        ERROR_NOT_CURRENT_PRODUCT(179),
        ERROR_BIZ_DOWNGRADE(180),
        ERROR_BUSINESS_UPGRADE(181),
        ERROR_CHANGE_PLAN_CREDIT(182),
        ERROR_BANDWIDTH_ERROR(183),
        ERROR_ALREADY_LINKED(184),
        ERROR_INVALID_FOLDERNAME(185),
        ERROR_ZIP_PASSWORD_BULK(186),
        ERROR_SERVER_NOT_FOUND(187),
        ERROR_NOT_LOGGED_IN(188),
        ERROR_RESELLER_TOS(189),
        ERROR_BUSINESS_SEAT(190),
        ERROR_BANNED_BUYER(191),
        ERROR_RESELLER_CREDITS_ERROR(192),
        ERROR_PURCHASE_BANNED_ERROR(193),
        ERROR_SUBDOMAIN_ERROR(194),
        ERROR_TOO_MANY_FAILED(195),
        ERROR_INVALID_CARD(196),
        ERROR_RECENT_SUBSCRIBER(197),
        ERROR_INVOICE_FAILED(198),
        ERROR_DUPLICATE_API_TRANSACTION(199),
        ERROR_CARDCCV_ERROR(200),
        ERROR_TRANSACTION_DECLINED(201),
        ERROR_PREPAID_CARD(202),
        ERROR_NO_RESPONSE(203),
        ERROR_HIGH_RISK(204),
        ERROR_DECLINED_HIGH_RISK(205),
        ERROR_CARD_STORE_FAILED(206),
        ERROR_COPY_LIMIT_EXCEEDED(207),
        ERROR_ASYNC_JOB_IN_PROGRESS(208),
        ERROR_FOLDER_ALREADY_DELETED(209),
        ERROR_FILE_ALREADY_DELETED(210),
        ERROR_CANT_MODIFY_DELETED_ITEMS(211),
        ERROR_CHANGE_FROM_FREE(212),
        ERROR_DMCA_CANNOT_REPORT(213),
        ERROR_INVALID_FILEDROP_KEY(214),
        ERROR_MISSING_SIGNATURE(215),
        ERROR_EMAIL_ADDRESS_TOO_SHORT(216),
        ERROR_EMAIL_ADDRESS_TOO_LONG(217),
        ERROR_FB_EMAIL_MISSING(218),
        ERROR_FB_EMAIL_EXISTS(219),
        ERROR_AUTH_FACEBOOK(220),
        ERROR_AUTH_TWITTER(221),
        ERROR_UNKNOWN_PATCH(222),
        ERROR_INVALID_REVISION(223),
        ERROR_NO_ACTIVE_INVOICE(224),
        ERROR_APPLICATION_NO_LOGGING(225),
        ERROR_INVALID_INSTALLATION_ID(226),
        ERROR_INCIDENT_MISMATCH(227),
        ERROR_MISSING_FACEBOOK_TOKEN(228),
        ERROR_MISSING_TWITTER_TOKEN(229),
        ERROR_NO_AVATAR(230),
        ERROR_INVALID_SOFTWARE_TOKEN(231),
        ERROR_EMAIL_NOT_VALIDATED(232),
        ERROR_AUTH_GMAIL(233),
        ERROR_FAILED_TO_SEND_MESSAGE(234),
        ERROR_USER_IS_OWNER(235),
        ERROR_USER_IS_FOLLOWER(236),
        ERROR_USER_NOT_FOLLOWER(237),
        ERROR_PATCH_NO_CHANGE(238),
        ERROR_SHARE_LIMIT_REACHED(239),
        ERROR_CANNOT_GRANT_PERMS(240),
        ERROR_INVALID_PRINT_SERVICE(241),
        ERROR_FOLDER_FILES_EXCEEDED(242),
        ERROR_ACCOUNT_TEMPORARILY_LOCKED(243),
        ERROR_NON_US_USER(244),;

        private final int value;

        private static final int ZIP_MAX_FILESIZE_READABLE = 0;
        private static final int ZIP_MAX_TOTAL_FILESIZE = 0;
        private static final int ZIP_MAX_FILES = 0;
        private static final int MAX_OBJECTS = 6000;
        private static final int FOLDER_DEPTH_LIMIT = 130;

        private ResponseCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ResponseCode fromInt(int value) {
            for (final ResponseCode e : values()) {
                if (e.getValue() == value) {
                    return e;
                }
            }
            return null;
        }
    }
}
