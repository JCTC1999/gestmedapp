package com.calidad.gestemed.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class ByteArrayMultipartFile implements MultipartFile {

    private final byte[] bytes;
    private final String originalFilename;
    private final String contentType;
    private final String name;

    public ByteArrayMultipartFile(byte[] bytes, String originalFilename) {
        this(bytes, originalFilename, "image/png", "file");
    }

    public ByteArrayMultipartFile(byte[] bytes, String originalFilename, String contentType) {
        this(bytes, originalFilename, contentType, "file");
    }

    public ByteArrayMultipartFile(byte[] bytes, String originalFilename, String contentType, String name) {
        this.bytes = bytes;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.name = name;
    }

    @Override public String getName() { return name; }
    @Override public String getOriginalFilename() { return originalFilename; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return bytes == null || bytes.length == 0; }
    @Override public long getSize() { return bytes.length; }
    @Override public byte[] getBytes() { return bytes; }
    @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

    @Override
    public void transferTo(File dest) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(bytes);
        }
    }
}
