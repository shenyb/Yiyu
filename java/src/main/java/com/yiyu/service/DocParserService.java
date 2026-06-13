package com.yiyu.service;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;

@Service
public class DocParserService {

    private static final Logger log = LoggerFactory.getLogger(DocParserService.class);

    /**
     * 从文件中提取纯文本
     */
    public String extractText(File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            return extractPdf(file);
        } else if (name.endsWith(".docx")) {
            return extractDocx(file);
        } else if (name.endsWith(".txt") || name.endsWith(".md")) {
            return Files.readString(file.toPath());
        } else {
            throw new IllegalArgumentException("不支持的文件格式：" + name);
        }
    }

    private String extractPdf(File file) throws Exception {
        try (var doc = org.apache.pdfbox.Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    private String extractDocx(File file) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
