package com.mvpiq.resource;

import com.mvpiq.dto.ApiResponse;
import com.mvpiq.service.SubscriptionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/users/{userId}/subscription")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SubscriptionResource {

    @Inject
    SubscriptionService subscriptionService;

    /**
     * Get user subscription details
     */
    @GET
    public Response getSubscriptionDetails(@PathParam("userId") UUID userId) {
        try {
            SubscriptionService.SubscriptionDetails details = subscriptionService.getSubscriptionDetails(userId);
            return Response.ok(ApiResponse.success(details, "Subscription details retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving subscription details: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if user has premium access
     */
    @GET
    @Path("/premium")
    public Response checkPremiumAccess(@PathParam("userId") UUID userId) {
        try {
            boolean isPremium = subscriptionService.isPremium(userId);
            return Response.ok(ApiResponse.success(isPremium, "Premium access status retrieved")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking premium access: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if user has scout access
     */
    @GET
    @Path("/scout-access")
    public Response checkScoutAccess(@PathParam("userId") UUID userId) {
        try {
            boolean hasScoutAccess = subscriptionService.hasScoutAccess(userId);
            return Response.ok(ApiResponse.success(hasScoutAccess, "Scout access status retrieved")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking scout access: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if user has creator access
     */
    @GET
    @Path("/creator-access")
    public Response checkCreatorAccess(@PathParam("userId") UUID userId) {
        try {
            boolean hasCreatorAccess = subscriptionService.hasCreatorAccess(userId);
            return Response.ok(ApiResponse.success(hasCreatorAccess, "Creator access status retrieved")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking creator access: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get user subscription plan
     */
    @GET
    @Path("/plan")
    public Response getUserPlan(@PathParam("userId") UUID userId) {
        try {
            SubscriptionService.SubscriptionPlan plan = subscriptionService.getUserPlan(userId);
            return Response.ok(ApiResponse.success(plan, "User plan retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving user plan: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Upgrade user to premium
     */
    @POST
    @Path("/upgrade-premium")
    public Response upgradeToPremium(@PathParam("userId") UUID userId) {
        try {
            boolean success = subscriptionService.upgradeToPremium(userId);
            if (success) {
                return Response.ok(ApiResponse.success(null, "Upgraded to premium successfully")).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(ApiResponse.error("Failed to upgrade to premium"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error upgrading to premium: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if user can access a specific feature
     */
    @GET
    @Path("/features/{feature}")
    public Response checkFeatureAccess(@PathParam("userId") UUID userId, @PathParam("feature") String feature) {
        try {
            boolean hasAccess = subscriptionService.canAccessFeature(userId, feature);
            return Response.ok(ApiResponse.success(hasAccess, "Feature access status retrieved")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking feature access: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get video upload limits for user
     */
    @GET
    @Path("/video-upload-limits")
    public Response getVideoUploadLimits(@PathParam("userId") UUID userId) {
        try {
            SubscriptionService.VideoUploadLimits limits = subscriptionService.getVideoUploadLimits(userId);
            return Response.ok(ApiResponse.success(limits, "Video upload limits retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving video upload limits: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get video analysis limits for user
     */
    @GET
    @Path("/video-analysis-limits")
    public Response getVideoAnalysisLimits(@PathParam("userId") UUID userId) {
        try {
            SubscriptionService.VideoAnalysisLimits limits = subscriptionService.getVideoAnalysisLimits(userId);
            return Response.ok(ApiResponse.success(limits, "Video analysis limits retrieved successfully")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error retrieving video analysis limits: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if user can create official content
     */
    @GET
    @Path("/can-create-official-content")
    public Response checkOfficialContentCreation(@PathParam("userId") UUID userId) {
        try {
            boolean canCreate = subscriptionService.canCreateOfficialContent(userId);
            return Response.ok(ApiResponse.success(canCreate, "Official content creation access retrieved")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking official content creation access: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if user can access advanced analytics
     */
    @GET
    @Path("/can-access-analytics")
    public Response checkAdvancedAnalyticsAccess(@PathParam("userId") UUID userId) {
        try {
            boolean canAccess = subscriptionService.canAccessAdvancedAnalytics(userId);
            return Response.ok(ApiResponse.success(canAccess, "Advanced analytics access retrieved")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking advanced analytics access: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Check if user can use advanced filters
     */
    @GET
    @Path("/can-use-advanced-filters")
    public Response checkAdvancedFiltersAccess(@PathParam("userId") UUID userId) {
        try {
            boolean canUse = subscriptionService.canUseAdvancedFilters(userId);
            return Response.ok(ApiResponse.success(canUse, "Advanced filters access retrieved")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.error("Error checking advanced filters access: " + e.getMessage()))
                    .build();
        }
    }
}
