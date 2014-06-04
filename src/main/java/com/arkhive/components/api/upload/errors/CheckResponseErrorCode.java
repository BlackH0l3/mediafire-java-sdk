package com.arkhive.components.api.upload.errors;

/**
 * This enum represents the preupload error code that can
 * appear in the data structure returned by the pre upload 
 * response as called by /api/upload/pre_upload.php.
 * @author Chris Najar
 *
 */
public enum CheckResponseErrorCode {
  NON_OWNER_UPLOAD_WITHOUT_WRITE_PERMISSIONS_TO_FOLDER(114), 
  NO_ERROR(0),
    ;

    private int value;

    private CheckResponseErrorCode(int value) { this.value = value; }

    public int getValue() { return this.value; }

    public static CheckResponseErrorCode fromInt(int value) {
        for (final CheckResponseErrorCode e: values()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String response;
        switch(this.value) {
            case 114: response = "Success"; break;
            default: response = "No error code associated with: " + this.value; break;
        }
        return response;
    }
}
