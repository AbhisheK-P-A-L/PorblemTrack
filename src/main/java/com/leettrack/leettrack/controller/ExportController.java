package com.leettrack.leettrack.controller;

import com.leettrack.leettrack.service.PdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final PdfExportService pdfExportService;

    public ExportController(PdfExportService pdfExportService) {
        this.pdfExportService = pdfExportService;
    }

    /**
     * GET /api/export/pdf
     * Returns a downloadable PDF of the authenticated user's saved problems,
     * grouped by canonical topic, with notes and revision flags.
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        byte[] pdf = pdfExportService.export(userId);

        String filename = "leettrack-notes-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(pdf);
    }
}
