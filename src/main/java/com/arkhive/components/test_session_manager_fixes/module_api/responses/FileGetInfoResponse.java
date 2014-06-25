package com.arkhive.components.test_session_manager_fixes.module_api.responses;

/**
 * @author
 */
public class FileGetInfoResponse extends ApiResponse {
    //CHECKSTYLE:OFF
    private FileInfo file_info;
    //CHECKSTYLE:ON

    public FileInfo getFileInfo() {
        if (file_info == null) {
            this.file_info = new FileInfo();
        }
        return this.file_info;
    }

    /**
     * class representing contents of "file_info" jsonobject.
     *
     * @author
     */
    public class FileInfo {
        //CHECKSTYLE:OFF
        private String password_protected;
        private String owner_name;
        private String shared_by_user;
        private String parent_folderkey;
        //CHECKSTYLE:ON
        private String quickkey;
        private String filename;
        private String created;
        private String downloads;
        private String description;
        private String size;
        private String privacy;
        private String hash;
        private String filetype;
        private String mimetype;
        private String flag;
        private String permissions;
        private String revision;

        public String getQuickKey() {
            if (this.quickkey == null) {
                this.quickkey = "";
            }
            return this.quickkey;
        }

        public String getFileName() {
            if (this.filename == null) {
                this.filename = "";
            }
            return this.filename;
        }

        public String getCreated() {
            if (this.created == null) {
                this.created = "";
            }
            return this.created;
        }

        public int getDownloads() {
            if (this.downloads == null) {
                this.downloads = "0";
            }
            return Integer.valueOf(this.downloads);
        }

        public String getDescription() {
            if (this.description == null) {
                this.description = "";
            }
            return this.description;
        }

        public int getSize() {
            if (this.size == null) {
                this.size = "0";
            }
            return Integer.valueOf(this.size);
        }

        public boolean isPrivate() {
            if (this.privacy == null) {
                this.privacy = "public";
            }

            return "private".equalsIgnoreCase(this.privacy);
        }

        public boolean isPasswordProtected() {
            if (this.password_protected == null) {
                this.password_protected = "no";
            }

            return "yes".equalsIgnoreCase(this.password_protected);
        }

        public String getHash() {
            if (this.hash == null) {
                this.hash = "";
            }
            return this.hash;
        }

        public String getFileType() {
            if (this.filetype == null) {
                this.filetype = "";
            }
            return this.filetype;
        }

        public String getMimeType() {
            if (this.mimetype == null) {
                this.mimetype = "";
            }
            return this.mimetype;
        }

        public String getOwnerName() {
            if (this.owner_name == null) {
                this.owner_name = "";
            }
            return this.owner_name;
        }

        public int getFlag() {
            if (this.flag == null) {
                this.flag = "-1";
            }
            return Integer.valueOf(this.flag);
        }

        public boolean isSharedByUser() {
            if (this.shared_by_user == null) {
                this.shared_by_user = "yes";
            }

            return !"no".equalsIgnoreCase(this.shared_by_user);
        }

        public int getPermissions() {
            if (this.permissions == null) {
                this.permissions = "-1";
            }

            return Integer.valueOf(this.permissions);
        }

        public String getParentFolderKey() {
            if (this.parent_folderkey == null) {
                this.parent_folderkey = "";
            }
            return this.parent_folderkey;
        }

        public int getRevision() {
            if (this.revision == null) {
                this.revision = "0";
            }
            return Integer.valueOf(this.revision);
        }

    }
}