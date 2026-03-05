package com.mvpiq.resource;

import com.mvpiq.dto.RankingDTO;
import com.mvpiq.enums.UserRole;
import com.mvpiq.model.Ranking;
import com.mvpiq.repositories.RankingRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/api")
@RequestScoped
public class RankingResource {

    @Inject
    RankingRepository rankingRepository;

    private static final Set<String> ALLOWED_POSITION_CODES = Set.of("PG","SG","SF","PF","C");

    @GET
    @Path("/ranking/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRankingByRole(@PathParam("role") String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Role is required").build();
        }

        String roleCode = roleStr.trim().toUpperCase();

        if (!ALLOWED_POSITION_CODES.contains(roleCode)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid role. Allowed values: " + ALLOWED_POSITION_CODES).build();
        }

        List<Ranking> rankings = rankingRepository.findByRoleCode(roleCode);
        List<RankingDTO> dtos = rankings.stream()
                .map(RankingDTO::fromEntity)
                .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }

    // Ottieni ranking globale
    @GET
    @Path("/ranking/global")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGlobalRanking() {
        List<Ranking> rankings = rankingRepository.findGlobalRanking();
        List<RankingDTO> dtos = rankings.stream()
                .map(RankingDTO::fromEntity)
                .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }
}
