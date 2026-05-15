package com.mvpiq.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.microsoft.playwright.options.Margin;
import com.mvpiq.dto.HighlightDTO;
import com.mvpiq.dto.PdfCvDTO;
import com.mvpiq.dto.PlayerCvTeamDTO;
import com.microsoft.playwright.*;
import com.mvpiq.model.PlayerCvHighlight;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class PdfGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(PdfGenerationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject
    PlayerCvService playerCvService;

    public byte[] generatePdf(UUID playerId) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            // Get CV data
            PdfCvDTO cvData = buildPdfCvDTO(playerId);

            // Generate HTML
            String html = generateHtmlTemplate(cvData);

            // Load HTML
            page.setContent(html);

            // Generate PDF
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0.5cm").setRight("0.5cm").setBottom("0.5cm").setLeft("0.5cm")));

            browser.close();
            return pdf;
        }
    }

    public byte[] generatePublicPdf(UUID shareToken) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            // Get public CV data
            PdfCvDTO cvData = buildPublicPdfCvDTO(shareToken);

            // Generate HTML
            String html = generateHtmlTemplate(cvData);

            // Load HTML
            page.setContent(html);

            // Generate PDF
            byte[] pdf = page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("0.5cm").setRight("0.5cm").setBottom("0.5cm").setLeft("0.5cm")));

            browser.close();
            return pdf;
        }
    }

    private PdfCvDTO buildPdfCvDTO(UUID playerId) {
        var cvDto = playerCvService.getCv(playerId);
        var highlights = playerCvService.getHighlights(playerId);

        return PdfCvDTO.builder()
                .playerCvId(null)
                .playerName("")
                .playerEmail("")
                .headline(cvDto.getHeadline())
                .summary(cvDto.getSummary())
                .stats(cvDto.getStats())
                .teams(cvDto.getTeams())
                .highlights(highlights.stream().map(h -> HighlightDTO.builder()
                        .id(h.getId())
                        .title(h.getTitle())
                        .description(h.getDescription())
                        .thumbnailUrl(h.getThumbnailUrl())
                        .externalUrl(h.getExternalUrl())
                        .build()).toList())
                .publicUrl("")
                .shareToken("")
                .build();
    }

    private PdfCvDTO buildPublicPdfCvDTO(UUID shareToken) {
        var cvDto = playerCvService.getPublicCv(shareToken);
        var cv = playerCvService.cvRepository.findByShareToken(shareToken).orElse(null);
        List<PlayerCvHighlight> highlights = cv != null ? playerCvService.highlightRepository.findByCvIdOrderBySortOrder(cv.getId()) : List.of();

        String publicUrl = cv != null ? "https://app.mvpiq-hoops.com/public/cv/" + shareToken : "";
        String playerName = cv != null && cv.getPlayer() != null ? cv.getPlayer().getDisplayName() : "";
        String playerEmail = cv != null && cv.getPlayer() != null ? cv.getPlayer().getEmail() : "";

        return PdfCvDTO.builder()
                .playerCvId(cv != null ? cv.getId() : null)
                .playerName(playerName)
                .playerEmail(playerEmail)
                .headline(cvDto.getHeadline())
                .summary(cvDto.getSummary())
                .stats(cvDto.getStats())
                .teams(cvDto.getTeams())
                .highlights(highlights.stream().map(h -> HighlightDTO.builder()
                        .id(h.getId())
                        .title(h.getTitle())
                        .description(h.getDescription())
                        .thumbnailUrl(h.getThumbnailUrl())
                        .externalUrl(h.getExternalUrl())
                        .build()).toList())
                .publicUrl(publicUrl)
                .shareToken(shareToken.toString())
                .build();
    }

    private String generateHtmlTemplate(PdfCvDTO cv) {
        String qrCodeBase64 = generateQrCodeBase64(cv.getPublicUrl());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
                .append("<html><head>")
                .append("<meta charset='UTF-8'>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; color: #333; }")
                .append(".header { display: flex; align-items: center; margin-bottom: 30px; border-bottom: 3px solid #2563eb; padding-bottom: 20px; }")
                .append(".profile-photo { width: 120px; height: 120px; border-radius: 50%; background: #e5e7eb; margin-right: 20px; display: flex; align-items: center; justify-content: center; font-size: 40px; color: #9ca3af; }")
                .append(".player-info h1 { margin: 0; color: #1e40af; font-size: 28px; }")
                .append(".player-info p { margin: 5px 0; color: #6b7280; }")
                .append(".section { margin-bottom: 30px; }")
                .append(".section h2 { color: #1e40af; border-bottom: 2px solid #e5e7eb; padding-bottom: 10px; margin-bottom: 15px; font-size: 20px; }")
                .append(".stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; }")
                .append(".stat-card { background: #f3f4f6; padding: 15px; border-radius: 8px; text-align: center; }")
                .append(".stat-card .value { font-size: 24px; font-weight: bold; color: #1e40af; }")
                .append(".stat-card .label { font-size: 14px; color: #6b7280; }")
                .append(".team-table { width: 100%; border-collapse: collapse; }")
                .append(".team-table th, .team-table td { padding: 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }")
                .append(".team-table th { background: #f3f4f6; color: #1e40af; }")
                .append(".highlights-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }")
                .append(".highlight-card { border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; }")
                .append(".highlight-thumbnail { width: 100%; height: 120px; background: #e5e7eb; display: flex; align-items: center; justify-content: center; color: #9ca3af; }")
                .append(".highlight-info { padding: 12px; }")
                .append(".highlight-title { font-weight: bold; margin-bottom: 5px; }")
                .append(".highlight-link { color: #2563eb; text-decoration: none; font-size: 12px; }")
                .append(".footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; }")
                .append(".qr-section { display: flex; align-items: center; gap: 15px; }")
                .append(".qr-code { width: 80px; height: 80px; }")
                .append(".qr-text { font-size: 12px; color: #6b7280; }")
                .append("</style>")
                .append("</head><body>");

        // Header
        html.append("<div class='header'>")
                .append("<div class='profile-photo'>").append(cv.getPlayerName() != null ? cv.getPlayerName().charAt(0) : "?").append("</div>")
                .append("<div class='player-info'>")
                .append("<h1>").append(cv.getPlayerName() != null ? cv.getPlayerName() : "Nome Giocatore").append("</h1>")
                .append("<p>").append(cv.getPlayerEmail() != null ? cv.getPlayerEmail() : "").append("</p>")
                .append("</div>")
                .append("</div>");

        // Profile Section
        if (cv.getHeadline() != null || cv.getSummary() != null) {
            html.append("<div class='section'>")
                    .append("<h2>Profilo</h2>");
            if (cv.getHeadline() != null) {
                html.append("<p><strong>").append(cv.getHeadline()).append("</strong></p>");
            }
            if (cv.getSummary() != null) {
                html.append("<p>").append(cv.getSummary()).append("</p>");
            }
            html.append("</div>");
        }

        // Stats Section
        if (cv.getStats() != null && !cv.getStats().isEmpty()) {
            html.append("<div class='section'>")
                    .append("<h2>Statistiche</h2>")
                    .append("<div class='stats-grid'>");
            for (Map.Entry<String, Object> entry : cv.getStats().entrySet()) {
                html.append("<div class='stat-card'>")
                        .append("<div class='value'>").append(entry.getValue() != null ? entry.getValue().toString() : "-").append("</div>")
                        .append("<div class='label'>").append(entry.getKey()).append("</div>")
                        .append("</div>");
            }
            html.append("</div></div>");
        }

        // Career Section
        if (cv.getTeams() != null && !cv.getTeams().isEmpty()) {
            html.append("<div class='section'>")
                    .append("<h2>Carriera</h2>")
                    .append("<table class='team-table'>")
                    .append("<thead><tr><th>Stagione</th><th>Squadra</th><th>Categoria</th><th>Ruolo</th></tr></thead>")
                    .append("<tbody>");
            for (PlayerCvTeamDTO team : cv.getTeams()) {
                html.append("<tr>")
                        .append("<td>").append(formatYearRange(team.getStartYear(), team.getEndYear())).append("</td>")
                        .append("<td>").append(team.getTeamName() != null ? team.getTeamName() : "-").append("</td>")
                        .append("<td>").append(team.getCategoryId() != null ? team.getCategoryId().toString() : "-").append("</td>")
                        .append("<td>").append(team.getPositionId() != null ? team.getPositionId().toString() : "-").append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table></div>");
        }

        // Highlights Section
        if (cv.getHighlights() != null && !cv.getHighlights().isEmpty()) {
            html.append("<div class='section'>")
                    .append("<h2>Highlights</h2>")
                    .append("<div class='highlights-grid'>");
            for (HighlightDTO highlight : cv.getHighlights()) {
                html.append("<div class='highlight-card'>")
                        .append("<div class='highlight-thumbnail'>🎬</div>")
                        .append("<div class='highlight-info'>")
                        .append("<div class='highlight-title'>").append(highlight.getTitle() != null ? highlight.getTitle() : "Highlight").append("</div>");
                if (highlight.getExternalUrl() != null) {
                    html.append("<a class='highlight-link' href='").append(highlight.getExternalUrl()).append("'>Guarda Video</a>");
                }
                html.append("</div></div>");
            }
            html.append("</div></div>");
        }

        // Footer with QR Code
        html.append("<div class='footer'>")
                .append("<div>")
                .append("<p><strong>MVPIQ Hoops</strong></p>")
                .append("<p>Generato il ").append(java.time.LocalDate.now().format(DATE_FORMATTER)).append("</p>")
                .append("</div>")
                .append("<div class='qr-section'>")
                .append("<img class='qr-code' src='data:image/png;base64,").append(qrCodeBase64).append("' alt='QR Code'>")
                .append("<div class='qr-text'>")
                .append("<p>Scansiona per vedere il CV completo</p>")
                .append("<p>").append(cv.getPublicUrl()).append("</p>")
                .append("</div>")
                .append("</div>")
                .append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private String formatYearRange(Integer start, Integer end) {
        if (start == null) return "-";
        if (end == null) return start + " - Presente";
        return start + " - " + end;
    }

    private String generateQrCodeBase64(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 200, 200);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            LOG.error("Error generating QR code", e);
            return "";
        }
    }
}
