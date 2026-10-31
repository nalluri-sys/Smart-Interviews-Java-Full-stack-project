package com.smartinterviewcoach.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ResumeParserService {

    /**
     * Extracts text content from a resume file (PDF, DOCX, or TXT).
     */
    public String extractText(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File has no name");
        }

        String lowerName = originalFilename.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return extractFromPdf(file.getBytes());
        } else if (lowerName.endsWith(".docx")) {
            return extractFromDocx(file.getBytes());
        } else if (lowerName.endsWith(".txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload PDF, DOCX, or TXT.");
        }
    }

    private String extractFromPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Encrypted PDF files are not supported.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return sanitize(text);
        }
    }

    private String extractFromDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(
            new java.io.ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : document.getParagraphs()) {
                sb.append(para.getText()).append("\n");
            }
            return sanitize(sb.toString());
        }
    }

    private String sanitize(String text) {
        if (text == null) return "";
        // Limit to 15000 chars to avoid token limit issues
        String trimmed = text.trim();
        if (trimmed.length() > 15000) {
            trimmed = trimmed.substring(0, 15000);
        }
        // Remove null bytes and non-printable characters (keep newlines)
        return trimmed.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }

    /**
     * Validates the uploaded resume file for type and size.
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is required.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds the 5 MB limit.");
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            throw new IllegalArgumentException("File has no name.");
        }
        String lower = name.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".docx") && !lower.endsWith(".txt")) {
            throw new IllegalArgumentException("Only PDF, DOCX, and TXT files are supported.");
        }
        // Basic MIME type check
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("executable") || contentType.contains("script")) {
                throw new IllegalArgumentException("Invalid file type.");
            }
        }
    }
}
