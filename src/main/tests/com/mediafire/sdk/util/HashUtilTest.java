package com.mediafire.sdk.util;

import junit.framework.TestCase;

public class HashUtilTest extends TestCase {

    private final String HASH_STRING_A = "The quick brown fox jumps over the lazy dog";
    private final String HASH_STRING_B = "Test vector from febooti.com";
    private final String HASH_STRING_C = "";

    private final String MD5_HASH_STRING_A = "9e107d9d372bb6826bd81d3542a419d6";
    private final String SHA1_HASH_STRING_A = "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12";
    private final String SHA256_HASH_STRING_A = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592";
    
    private final String MD5_HASH_STRING_B = "500ab6613c6db7fbd30c62f5ff573d0f";
    private final String SHA1_HASH_STRING_B = "a7631795f6d59cd6d14ebd0058a6394a4b93d868";
    private final String SHA256_HASH_STRING_B = "077b18fe29036ada4890bdec192186e10678597a67880290521df70df4bac9ab";

    private final String MD5_HASH_STRING_C = "d41d8cd98f00b204e9800998ecf8427e";
    private final String SHA1_HASH_STRING_C = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
    private final String SHA256_HASH_STRING_C = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testMd5StringA() throws Exception {
        assertEquals(MD5_HASH_STRING_A, HashUtil.md5(HASH_STRING_A));
    }

    public void testMd5BytesA() throws Exception {
        assertEquals(MD5_HASH_STRING_A, HashUtil.md5(HASH_STRING_A.getBytes()));
    }

    public void testMd5StringB() throws Exception {
        assertEquals(MD5_HASH_STRING_B, HashUtil.md5(HASH_STRING_B));
    }

    public void testMd5BytesB() throws Exception {
        assertEquals(MD5_HASH_STRING_B, HashUtil.md5(HASH_STRING_B.getBytes()));
    }

    public void testMd5StringC() throws Exception {
        assertEquals(MD5_HASH_STRING_C, HashUtil.md5(HASH_STRING_C));
    }

    public void testMd5BytesC() throws Exception {
        assertEquals(MD5_HASH_STRING_C, HashUtil.md5(HASH_STRING_C.getBytes()));
    }

    public void testSha1StringA() throws Exception {
        assertEquals(SHA1_HASH_STRING_A, HashUtil.sha1(HASH_STRING_A));
    }

    public void testSha1BytesA() throws Exception {
        assertEquals(SHA1_HASH_STRING_A, HashUtil.sha1(HASH_STRING_A.getBytes()));
    }

    public void testSha1StringB() throws Exception {
        assertEquals(SHA1_HASH_STRING_B, HashUtil.sha1(HASH_STRING_B));
    }

    public void testSha1BytesB() throws Exception {
        assertEquals(SHA1_HASH_STRING_B, HashUtil.sha1(HASH_STRING_B.getBytes()));
    }

    public void testSha1StringC() throws Exception {
        assertEquals(SHA1_HASH_STRING_C, HashUtil.sha1(HASH_STRING_C));
    }

    public void testSha1BytesC() throws Exception {
        assertEquals(SHA1_HASH_STRING_C, HashUtil.sha1(HASH_STRING_C.getBytes()));
    }

    public void testSha256StringA() throws Exception {
        assertEquals(SHA256_HASH_STRING_A, HashUtil.sha256(HASH_STRING_A));
    }

    public void testSha256BytesA() throws Exception {
        assertEquals(SHA256_HASH_STRING_A, HashUtil.sha256(HASH_STRING_A));
    }

    public void testSha256StringB() throws Exception {
        assertEquals(SHA256_HASH_STRING_B, HashUtil.sha256(HASH_STRING_B));
    }

    public void testSha256BytesB() throws Exception {
        assertEquals(SHA256_HASH_STRING_B, HashUtil.sha256(HASH_STRING_B));
    }

    public void testSha256StringC() throws Exception {
        assertEquals(SHA256_HASH_STRING_C, HashUtil.sha256(HASH_STRING_C));
    }

    public void testSha256BytesC() throws Exception {
        assertEquals(SHA256_HASH_STRING_C, HashUtil.sha256(HASH_STRING_C));
    }
}