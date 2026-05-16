package com.mvpiq.service;

import com.mvpiq.dto.*;
import com.mvpiq.model.*;
import com.mvpiq.repositories.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;

@ApplicationScoped
@Transactional
public class PlayerCvService {

    @Inject PlayerRepository playerRepository;
    @Inject PlayerCvRepository cvRepository;
    @Inject PlayerCvTeamRepository teamRepository;
    @Inject PlayerCvHighlightRepository highlightRepository;
    @Inject PositionMetadataRepository positionRepository;
    @Inject MediaAssetRepository mediaAssetRepository;

    @ConfigProperty(name = "app.public.base-url", defaultValue = "https://app.mvpiq-hoops.com")
    String publicBaseUrl;

    // ─── GET CV ───────────────────────────────────────────────
    public PlayerCvDTO getCv(UUID playerId) {
        PlayerCv cv = findOrCreateCv(playerId);
        List<PlayerCvTeam>      teams      = teamRepository.findByPlayerIdColumn(playerId);
        List<PlayerCvHighlight> highlights = highlightRepository.findByCvId(cv.getId());
        return toDTO(cv, teams, highlights);
    }

    // ─── UPDATE CV ────────────────────────────────────────────
    public PlayerCvDTO updateCv(UUID playerId, PlayerCvDTO dto) {
        Player player = requirePlayer(playerId);
        PlayerCv cv = cvRepository.findByPlayerIdNativeQuery(playerId)
                .orElseGet(() -> {
                    PlayerCv newCv = new PlayerCv();
                    newCv.setPlayer(player);
                    cvRepository.persist(newCv);
                    return newCv;
                });

        cv.setHeadline(dto.getHeadline());
        cv.setSummary(dto.getSummary());
        cv.setStats(dto.getStats());
        cv.setUpdatedAt(OffsetDateTime.now());

        // Rebuild teams
        teamRepository.delete("cv.id", cv.getId());
        if (dto.getTeams() != null) {
            for (PlayerCvTeamDTO teamDto : dto.getTeams()) {
                validateTeamYears(teamDto);
                PlayerCvTeam team = new PlayerCvTeam();
                team.setCv(cv);
                team.setTeamName(teamDto.getTeamName());
                team.setCategoryId(teamDto.getCategoryId());
                team.setStartYear(teamDto.getStartYear());
                team.setEndYear(teamDto.getEndYear());
                team.setNotes(teamDto.getNotes());
                if (teamDto.getPositionId() != null) {
                    PositionMetadata pos = positionRepository
                            .find("id = ?1 and isActive = true", teamDto.getPositionId())
                            .firstResultOptional()
                            .orElseThrow(() -> new NotFoundException("Active position not found"));
                    team.setPosition(pos);
                }
                teamRepository.persist(team);
            }
        }

        return getCv(playerId);
    }

    // ─── ENABLE SHARE ─────────────────────────────────────────
    public CvSharingDTO enableSharing(UUID playerId) {
        PlayerCv cv = findOrCreateCv(playerId);
        cv.setShareEnabled(true);
        // shareToken è già generato alla creazione — non si rigenera mai
        return buildSharingDTO(cv);
    }

    // ─── DISABLE SHARE ────────────────────────────────────────
    public CvSharingDTO disableSharing(UUID playerId) {
        PlayerCv cv = findOrCreateCv(playerId);
        cv.setShareEnabled(false);
        return buildSharingDTO(cv);
    }

    // ─── GET HIGHLIGHTS ───────────────────────────────────────
    public List<PlayerCvHighlightDTO> getHighlights(UUID playerId) {
        PlayerCv cv = findOrCreateCv(playerId);
        return highlightRepository.findByCvId(cv.getId())
                .stream().map(PlayerCvHighlightDTO::fromEntity).toList();
    }

    // ─── ADD HIGHLIGHT (link esterno o mediaId esistente) ─────
    public PlayerCvHighlightDTO addHighlight(UUID playerId, AddHighlightRequest req) {
        PlayerCv cv = findOrCreateCv(playerId);

        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }

        MediaAsset media;

        if (req.getExternalUrl() != null && !req.getExternalUrl().isBlank()) {
            // Link esterno: crea un MediaAsset con solo externalUrl
            media = MediaAsset.builder()
                    .id(UUID.randomUUID())
                    .ownerId(playerId)
                    .title(req.getTitle())
                    .description(req.getDescription())
                    .mediaType("highlight")
                    .storageUrl(req.getExternalUrl()) // storageUrl è NOT NULL, usiamo l'url esterno
                    .visibility("PRIVATE")
                    .build();
            mediaAssetRepository.persist(media);

        } else if (req.getMediaId() != null && !req.getMediaId().isBlank()) {
            // MediaAsset già esistente
            media = mediaAssetRepository.findByIdOptional(UUID.fromString(req.getMediaId()))
                    .orElseThrow(() -> new NotFoundException("MediaAsset not found: " + req.getMediaId()));
        } else {
            throw new BadRequestException("Either externalUrl or mediaId is required");
        }

        PlayerCvHighlight highlight = PlayerCvHighlight.builder()
                .id(UUID.randomUUID())
                .cv(cv)
                .media(media)
                .title(req.getTitle())
                .description(req.getDescription())
                .createdAt(OffsetDateTime.now())
                .build();

        highlightRepository.persist(highlight);
        return PlayerCvHighlightDTO.fromEntity(highlight);
    }

    // ─── DELETE HIGHLIGHT ─────────────────────────────────────
    public void deleteHighlight(UUID playerId, UUID highlightId) {
        PlayerCv cv = findOrCreateCv(playerId);
        PlayerCvHighlight highlight = highlightRepository.findByIdOptional(highlightId)
                .orElseThrow(() -> new NotFoundException("Highlight not found"));

        // Verifica che l'highlight appartenga a questo CV
        if (!highlight.getCv().getId().equals(cv.getId())) {
            throw new BadRequestException("Highlight does not belong to this player's CV");
        }

        highlightRepository.delete(highlight);
    }

    // ─── GET PUBLIC CV (senza auth, via shareToken) ───────────
    public PlayerCvDTO getPublicCv(UUID shareToken) {
        PlayerCv cv = cvRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new NotFoundException("CV not found or not public"));

        List<PlayerCvTeam>      teams      = teamRepository.findByPlayerIdColumn(cv.getPlayer().getId());
        List<PlayerCvHighlight> highlights = highlightRepository.findByCvId(cv.getId());
        return toDTO(cv, teams, highlights);
    }

    // ─── HTML PUBBLICO ────────────────────────────────────────
    public String getPublicCvHtml(UUID shareToken) {
        PlayerCvDTO cv = getPublicCv(shareToken);
        return buildHtml(cv, shareToken);
    }

    // ─── HELPER PRIVATI ───────────────────────────────────────

    private PlayerCv findOrCreateCv(UUID playerId) {
        return cvRepository.findByPlayerIdNativeQuery(playerId).orElseGet(() -> {
            Player player = requirePlayer(playerId);
            PlayerCv newCv = new PlayerCv();
            newCv.setPlayer(player);
            cvRepository.persist(newCv);
            return newCv;
        });
    }

    private Player requirePlayer(UUID playerId) {
        return playerRepository.findByIdOptional(playerId)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
    }

    private CvSharingDTO buildSharingDTO(PlayerCv cv) {
        return CvSharingDTO.builder()
                .shareToken(cv.getShareToken().toString())
                .shareEnabled(cv.getShareEnabled())
                .publicUrl(cv.getShareEnabled()
                        ? publicBaseUrl + "/public/cv/" + cv.getShareToken()
                        : null)
                .build();
    }

    private PlayerCvDTO toDTO(PlayerCv cv, List<PlayerCvTeam> teams, List<PlayerCvHighlight> highlights) {
        PlayerCvDTO dto = new PlayerCvDTO();
        dto.setHeadline(cv.getHeadline());
        dto.setSummary(cv.getSummary());
        dto.setStats(cv.getStats());
        dto.setSharing(buildSharingDTO(cv));

        dto.setTeams(teams.stream().map(t -> {
            PlayerCvTeamDTO td = new PlayerCvTeamDTO();
            td.setId(t.getId());
            td.setTeamName(t.getTeamName());
            td.setCategoryId(t.getCategoryId());
            td.setStartYear(t.getStartYear());
            td.setEndYear(t.getEndYear());
            td.setNotes(t.getNotes());
            if (t.getPosition() != null) td.setPositionId(t.getPosition().getId());
            return td;
        }).toList());

        dto.setHighlights(highlights.stream()
                .map(PlayerCvHighlightDTO::fromEntity).toList());

        return dto;
    }

    private void validateTeamYears(PlayerCvTeamDTO dto) {
        Integer start = dto.getStartYear();
        Integer end   = dto.getEndYear();
        int currentYear = Year.now().getValue();

        if (start == null) throw new BadRequestException("Start year is required");
        if (start < 1900 || start > currentYear) throw new BadRequestException("Start year is not valid");
        if (end != null) {
            if (end < start) throw new BadRequestException("End year cannot be before start year");
            if (end > currentYear) throw new BadRequestException("End year cannot be in the future");
        }
    }

    // ─── HTML GENERATOR ───────────────────────────────────────
    private String buildHtml(PlayerCvDTO cv, UUID shareToken) {
        StringBuilder teams = new StringBuilder();
        if (cv.getTeams() != null) {
            for (var t : cv.getTeams()) {
                teams.append("<div class='card'>")
                     .append("<div class='card-title'>").append(esc(t.getTeamName())).append("</div>")
                     .append("<div class='card-meta'>Categoria: ").append(t.getCategoryId()).append("</div>");
                if (t.getStartYear() != null) teams.append("<div class='card-meta'>").append(t.getStartYear())
                        .append(t.getEndYear() != null ? " – " + t.getEndYear() : " – in corso").append("</div>");
                if (t.getNotes() != null) teams.append("<div class='card-note'>").append(esc(t.getNotes())).append("</div>");
                teams.append("</div>");
            }
        }
        if (teams.isEmpty()) teams.append("<p class='empty'>Nessuna squadra registrata</p>");

        StringBuilder highlights = new StringBuilder();
        if (cv.getHighlights() != null) {
            for (var h : cv.getHighlights()) {
                String url = h.getExternalUrl() != null ? h.getExternalUrl() : h.getStorageUrl();
                highlights.append("<div class='card highlight-card'>")
                          .append("<div class='card-title'>").append(esc(h.getTitle())).append("</div>");
                if (h.getDescription() != null) highlights.append("<div class='card-note'>").append(esc(h.getDescription())).append("</div>");
                if (url != null) highlights.append("<a class='hl-link' href='").append(url).append("' target='_blank' rel='noopener'>▶ Guarda video</a>");
                highlights.append("</div>");
            }
        }
        if (highlights.isEmpty()) highlights.append("<p class='empty'>Nessun highlight aggiunto</p>");

        return """
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>CV Sportivo – MVPiQ Hoops</title>
<style>
  :root { --orange: #ff8c00; --bg: #0b0f1a; --card: #1e2433; --border: #2a2a2a; --text: #fff; --muted: #aaa; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; min-height: 100vh; }
  .header { background: linear-gradient(135deg, #0b0f1a 0%, #1a2035 100%); padding: 40px 24px 32px; border-bottom: 2px solid var(--orange); }
  .badge { display: inline-block; background: var(--orange); color: #fff; font-size: 11px; font-weight: 700; letter-spacing: 1px; padding: 4px 10px; border-radius: 20px; margin-bottom: 12px; text-transform: uppercase; }
  .headline { font-size: clamp(20px, 4vw, 28px); font-weight: 800; color: var(--text); margin-bottom: 8px; }
  .summary { color: var(--muted); font-size: 15px; line-height: 1.6; max-width: 680px; }
  .container { max-width: 760px; margin: 0 auto; padding: 32px 24px; }
  .section { margin-bottom: 36px; }
  .section-title { font-size: 13px; font-weight: 700; color: var(--orange); text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
  .section-title::after { content: ''; flex: 1; height: 1px; background: var(--border); }
  .card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 16px 18px; margin-bottom: 10px; }
  .highlight-card { border-left: 3px solid var(--orange); }
  .card-title { font-size: 15px; font-weight: 700; color: var(--text); margin-bottom: 4px; }
  .card-meta { font-size: 13px; color: var(--muted); margin-bottom: 2px; }
  .card-note { font-size: 13px; color: #888; margin-top: 6px; }
  .hl-link { display: inline-block; margin-top: 10px; background: var(--orange); color: #fff; text-decoration: none; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 600; }
  .hl-link:hover { opacity: 0.85; }
  .empty { color: #555; font-style: italic; font-size: 14px; }
  .footer { text-align: center; padding: 32px 24px; border-top: 1px solid var(--border); color: #444; font-size: 12px; }
  .footer a { color: var(--orange); text-decoration: none; }
</style>
</head>
<body>
<div class="header">
  <div style="max-width:760px;margin:0 auto">
    <div class="badge">🏀 MVPiQ Hoops</div>
    <div class="headline">%s</div>
    %s
  </div>
</div>
<div class="container">
  <div class="section">
    <div class="section-title">Carriera</div>
    %s
  </div>
  <div class="section">
    <div class="section-title">Highlights Video</div>
    %s
  </div>
</div>
<div class="footer">
  CV generato con <a href="https://mvpiq-hoops.com">MVPiQ Hoops</a> · %s
</div>
</body>
</html>
""".formatted(
                esc(cv.getHeadline() != null ? cv.getHeadline() : "CV Sportivo"),
                cv.getSummary() != null ? "<div class='summary'>" + esc(cv.getSummary()) + "</div>" : "",
                teams,
                highlights,
                java.time.LocalDate.now().toString()
        );
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
