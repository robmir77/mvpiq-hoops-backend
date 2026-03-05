package com.mvpiq.resource;

import com.mvpiq.model.PositionMetadata;
import com.mvpiq.repositories.PositionMetadataRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/positions")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class PositionMetadataResource {

    @Inject
    PositionMetadataRepository repository;

    @GET
    public List<PositionMetadata> getAll() {
        return repository.listAll(Sort.by("code"));
    }
}
