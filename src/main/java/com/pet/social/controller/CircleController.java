package com.pet.social.controller;

import com.pet.social.config.AuthInterceptor;
import com.pet.social.domain.CircleEntry;
import com.pet.social.domain.CircleInviteEntry;
import com.pet.social.domain.CircleMemberEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.UserAccount;
import com.pet.social.service.CircleService;
import com.pet.social.service.PostService;
import com.pet.social.store.AppDataStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CircleController {
    private final CircleService circleService;
    private final PostService postService;
    private final AppDataStore dataStore;

    public CircleController(CircleService circleService, PostService postService, AppDataStore dataStore) {
        this.circleService = circleService;
        this.postService = postService;
        this.dataStore = dataStore;
    }

    @GetMapping("/circles")
    public List<CircleResponse> listCircles(HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        return circleService.listMyCircles(actor).stream().map(this::toResponse).toList();
    }

    @PostMapping("/circles")
    public CircleResponse createCircle(@Valid @RequestBody CircleUpsertRequest request, HttpServletRequest httpRequest) {
        CircleEntry circle = circleService.createCircle(currentUser(httpRequest), request.name(), request.description());
        return toResponse(circle);
    }

    @PutMapping("/circles/{circleId}")
    public CircleResponse updateCircle(@PathVariable long circleId, @Valid @RequestBody CircleUpsertRequest request,
                                       HttpServletRequest httpRequest) {
        CircleEntry circle = circleService.updateCircle(currentUser(httpRequest), circleId, request.name(), request.description());
        return toResponse(circle);
    }

    @DeleteMapping("/circles/{circleId}")
    public ActionResponse dissolveCircle(@PathVariable long circleId, HttpServletRequest request) {
        circleService.dissolveCircle(currentUser(request), circleId);
        return new ActionResponse("圈子已解散");
    }

    @GetMapping("/circles/{circleId}/members")
    public List<MemberResponse> listMembers(@PathVariable long circleId, HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        List<CircleMemberEntry> members = circleService.listMembers(actor, circleId);
        Map<Long, UserAccount> userSnapshot = dataStore.snapshotUsers(members.stream().map(CircleMemberEntry::getUserId).toList());
        CircleEntry circle = circleService.getAccessibleCircle(actor, circleId);
        return members.stream().map(member -> toMemberResponse(member, userSnapshot.get(member.getUserId()), circle)).toList();
    }

    @PostMapping("/circles/{circleId}/invites")
    public InviteResponse createInvite(@PathVariable long circleId, HttpServletRequest request) {
        CircleInviteEntry invite = circleService.createInvite(currentUser(request), circleId);
        String invitePath = "/pages/circle-join/index?token=" + invite.getToken();
        String inviteLink = "pet-social://circle/join?token=" + invite.getToken();
        return new InviteResponse(
            invite.getId(),
            invite.getCircleId(),
            invite.getToken(),
            invitePath,
            inviteLink,
            inviteLink,
            invite.getExpiresAt().toString()
        );
    }

    @PostMapping("/circles/join")
    public CircleResponse joinCircle(@Valid @RequestBody JoinCircleRequest request, HttpServletRequest httpRequest) {
        CircleEntry circle = circleService.joinByToken(currentUser(httpRequest), request.token());
        return toResponse(circle);
    }

    @DeleteMapping("/circles/{circleId}/members/{memberUserId}")
    public ActionResponse removeMember(@PathVariable long circleId, @PathVariable long memberUserId, HttpServletRequest request) {
        circleService.removeMember(currentUser(request), circleId, memberUserId);
        return new ActionResponse("成员已移除");
    }

    @PostMapping("/circles/{circleId}/leave")
    public ActionResponse leaveCircle(@PathVariable long circleId, HttpServletRequest request) {
        circleService.leaveCircle(currentUser(request), circleId);
        return new ActionResponse("已退出圈子");
    }

    @GetMapping("/circles/{circleId}/posts")
    public List<CirclePostResponse> listCirclePosts(@PathVariable long circleId, HttpServletRequest request) {
        UserAccount actor = currentUser(request);
        circleService.getAccessibleCircle(actor, circleId);
        return postService.listCirclePosts(circleId, actor).stream().map(post -> toPostResponse(post, actor)).toList();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(SecurityException.class)
    public ErrorResponse handleSecurity(SecurityException exception) {
        return new ErrorResponse(exception.getMessage());
    }

    private CircleResponse toResponse(CircleEntry circle) {
        UserAccount owner = dataStore.getUser(circle.getOwnerId());
        return new CircleResponse(
            circle.getId(),
            circle.getOwnerId(),
            owner == null ? "未知用户" : owner.getNickname(),
            circle.getName(),
            circle.getDescription(),
            circleService.countMembers(circle.getId()),
            circle.isDissolved(),
            circle.getCreatedAt().toString(),
            circle.getUpdatedAt().toString()
        );
    }

    private MemberResponse toMemberResponse(CircleMemberEntry member, UserAccount user, CircleEntry circle) {
        return new MemberResponse(
            member.getId(),
            member.getUserId(),
            user == null ? "未知用户" : user.getNickname(),
            circle.getOwnerId() == member.getUserId(),
            member.getJoinedAt().toString()
        );
    }

    private CirclePostResponse toPostResponse(PostEntry post, UserAccount actor) {
        UserAccount author = dataStore.getUser(post.getAuthorId());
        PetProfile pet = post.getPetId() == null ? null : dataStore.findPet(post.getPetId()).orElse(null);
        return new CirclePostResponse(
            post.getId(),
            post.getAuthorId(),
            post.getContent(),
            post.getImageUrls(),
            post.getVideoUrl(),
            post.getTopic(),
            post.getVisibleCircleIds(),
            post.getReviewStatus().name(),
            post.getLikeCount(),
            post.getCommentCount(),
            dataStore.hasLiked(post.getId(), actor.getId()),
            author == null ? "未知用户" : author.getNickname(),
            pet == null ? null : pet.getName(),
            post.getCreatedAt().toString()
        );
    }

    private UserAccount currentUser(HttpServletRequest request) {
        return (UserAccount) request.getAttribute(AuthInterceptor.CURRENT_USER);
    }

    public record CircleUpsertRequest(@NotBlank String name, String description) {
    }

    public record JoinCircleRequest(@NotBlank String token) {
    }

    public record CircleResponse(long id, long ownerId, String ownerNickname, String name, String description,
                                 long memberCount, boolean dissolved, String createdAt, String updatedAt) {
    }

    public record MemberResponse(long membershipId, long userId, String nickname, boolean owner, String joinedAt) {
    }

    public record InviteResponse(long id, long circleId, String token, String invitePath, String inviteLink,
                                 String inviteQrContent, String expiresAt) {
    }

    public record CirclePostResponse(long id, long authorId, String content, List<String> imageUrls, String videoUrl,
                                     String topic, List<Long> visibleCircleIds, String reviewStatus, int likeCount,
                                     int commentCount, boolean likedByMe, String authorNickname, String petName,
                                     String createdAt) {
    }

    public record ActionResponse(String message) {
    }

    public record ErrorResponse(String message) {
    }
}
