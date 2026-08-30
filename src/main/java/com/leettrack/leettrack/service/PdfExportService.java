package com.leettrack.leettrack.service;

import com.leettrack.leettrack.entity.SavedProblem;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates a PDF of a user's saved problems, grouped by canonicalTopic.
 * Uses OpenPDF (free, Apache 2.0 licensed fork of iText 2.x).
 *
 * PDF structure:
 *   Title: "LeetTrack — Interview Revision Notes"
 *   Subtitle: export date
 *   Per topic: bold heading, then each problem as:
 *     • Problem Title [Platform] [Difficulty] — link
 *       Notes: <user's notes if any>
 */
@Service
public class PdfExportService {

    private static final DateTimeFormatter DATE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private final SavedProblemService savedProblemService;

    public PdfExportService(SavedProblemService savedProblemService) {
        this.savedProblemService = savedProblemService;
    }

    public byte[] export(UUID userId) {
        List<SavedProblem> all = savedProblemService.findAllForExport(userId);

        // Group by topic (ordered by canonicalTopicCache since DB query sorts)
        Map<String, List<SavedProblem>> grouped = new LinkedHashMap<>();
        for (SavedProblem sp : all) {
            String topic = sp.getCanonicalTopicCache() != null
                ? sp.getCanonicalTopicCache() : "Uncategorized";
            grouped.computeIfAbsent(topic, k -> new java.util.ArrayList<>()).add(sp);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // ── Fonts ──────────────────────────────────────────────────────
            Font titleFont    = new Font(Font.HELVETICA, 20, Font.BOLD, Color.BLACK);
            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            Font topicFont    = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0x1a, 0x73, 0xe8));
            Font problemFont  = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
            Font metaFont     = new Font(Font.HELVETICA, 9,  Font.NORMAL, Color.GRAY);
            Font notesFont    = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.DARK_GRAY);
            Font linkFont     = new Font(Font.HELVETICA, 9,  Font.UNDERLINE, new Color(0x1a, 0x73, 0xe8));

            // ── Title page header ──────────────────────────────────────────
            Paragraph title = new Paragraph("LeetTrack — Interview Revision Notes", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph("Exported " + DATE_FMT.format(java.time.Instant.now()), subtitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            // ── Divider line ───────────────────────────────────────────────
            com.lowagie.text.pdf.draw.LineSeparator line = new com.lowagie.text.pdf.draw.LineSeparator();
            doc.add(new Chunk(line));
            doc.add(Chunk.NEWLINE);

            // ── Topics ────────────────────────────────────────────────────
            for (Map.Entry<String, List<SavedProblem>> entry : grouped.entrySet()) {
                // Topic heading
                Paragraph topicHeading = new Paragraph(entry.getKey(), topicFont);
                topicHeading.setSpacingBefore(16);
                topicHeading.setSpacingAfter(6);
                doc.add(topicHeading);

                for (SavedProblem sp : entry.getValue()) {
                    // Problem title
                    String revFlag = sp.isMarkedForRevision() ? " ★" : "";
                    Paragraph problemTitle = new Paragraph(
                        "• " + sp.getProblem().getTitle() + revFlag, problemFont);
                    problemTitle.setIndentationLeft(12);
                    problemTitle.setSpacingAfter(2);
                    doc.add(problemTitle);

                    // Metadata: platform, difficulty
                    String meta = String.format("[%s] [%s]",
                        sp.getProblem().getPlatform(),
                        nvl(sp.getProblem().getDifficulty()));
                    Paragraph metaPara = new Paragraph(meta, metaFont);
                    metaPara.setIndentationLeft(24);
                    doc.add(metaPara);

                    // Link
                    if (sp.getProblem().getLink() != null) {
                        Chunk linkChunk = new Chunk(sp.getProblem().getLink(), linkFont);
                        try {
                            linkChunk.setAnchor(sp.getProblem().getLink());
                        } catch (Exception ignored) {}
                        Paragraph linkPara = new Paragraph(linkChunk);
                        linkPara.setIndentationLeft(24);
                        doc.add(linkPara);
                    }

                    // Notes
                    if (sp.getNotes() != null && !sp.getNotes().isBlank()) {
                        Paragraph notesPara = new Paragraph("Notes: " + sp.getNotes(), notesFont);
                        notesPara.setIndentationLeft(24);
                        notesPara.setSpacingAfter(6);
                        doc.add(notesPara);
                    } else {
                        doc.add(new Paragraph(" "));
                    }
                }
            }

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private String nvl(String s) {
        return s != null ? s : "—";
    }
}
