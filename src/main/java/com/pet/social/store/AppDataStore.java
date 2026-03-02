package com.pet.social.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.social.domain.CircleEntry;
import com.pet.social.domain.CircleInviteEntry;
import com.pet.social.domain.CircleMemberEntry;
import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.PostVisibility;
import com.pet.social.domain.ReportEntry;
import com.pet.social.domain.ReviewStatus;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import com.pet.social.domain.UserStatus;
import com.pet.social.entity.CircleEntryEntity;
import com.pet.social.entity.CircleInviteEntryEntity;
import com.pet.social.entity.CircleMemberEntryEntity;
import com.pet.social.entity.CommentEntryEntity;
import com.pet.social.entity.PetProfileEntity;
import com.pet.social.entity.PostEntryEntity;
import com.pet.social.entity.PostLikeEntity;
import com.pet.social.entity.ReportEntryEntity;
import com.pet.social.entity.UserAccountEntity;
import com.pet.social.repository.CircleEntryRepository;
import com.pet.social.repository.CircleInviteRepository;
import com.pet.social.repository.CircleMemberRepository;
import com.pet.social.repository.CommentEntryRepository;
import com.pet.social.repository.PetProfileRepository;
import com.pet.social.repository.PostEntryRepository;
import com.pet.social.repository.PostLikeRepository;
import com.pet.social.repository.ReportEntryRepository;
import com.pet.social.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AppDataStore {
    private static final String ADMIN_PHONE = "admin@local";
    private static final String ADMIN_WECHAT_KEY = "__admin__";

    private final UserAccountRepository userAccountRepository;
    private final PetProfileRepository petProfileRepository;
    private final PostEntryRepository postEntryRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentEntryRepository commentEntryRepository;
    private final CircleEntryRepository circleEntryRepository;
    private final CircleMemberRepository circleMemberRepository;
    private final CircleInviteRepository circleInviteRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final ObjectMapper objectMapper;

    public AppDataStore(UserAccountRepository userAccountRepository,
                        PetProfileRepository petProfileRepository,
                        PostEntryRepository postEntryRepository,
                        PostLikeRepository postLikeRepository,
                        CommentEntryRepository commentEntryRepository,
                        CircleEntryRepository circleEntryRepository,
                        CircleMemberRepository circleMemberRepository,
                        CircleInviteRepository circleInviteRepository,
                        ReportEntryRepository reportEntryRepository,
                        ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.petProfileRepository = petProfileRepository;
        this.postEntryRepository = postEntryRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentEntryRepository = commentEntryRepository;
        this.circleEntryRepository = circleEntryRepository;
        this.circleMemberRepository = circleMemberRepository;
        this.circleInviteRepository = circleInviteRepository;
        this.reportEntryRepository = reportEntryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserAccount getOrCreatePhoneUser(String phone) {
        return userAccountRepository.findByPhone(phone)
            .map(this::toDomain)
            .orElseGet(() -> createPhoneUser(phone));
    }

    @Transactional
    public UserAccount getOrCreateWechatUser(String wechatKey, String nickname, String avatarUrl) {
        Optional<UserAccountEntity> existing = userAccountRepository.findByWechatKey(wechatKey);
        if (existing.isPresent()) {
            applyUserProfile(existing.get(), nickname, avatarUrl);
            return toDomain(existing.get());
        }
        return createWechatUser(wechatKey, nickname, avatarUrl);
    }

    @Transactional
    public UserAccount getOrCreateAdminUser() {
        return userAccountRepository.findByWechatKey(ADMIN_WECHAT_KEY)
            .map(this::toDomain)
            .orElseGet(this::createAdminUser);
    }

    @Transactional(readOnly = true)
    public UserAccount getUser(long userId) {
        return userAccountRepository.findById(userId).map(this::toDomain).orElse(null);
    }

    @Transactional
    public void updateUserStatus(long userId, UserStatus status) {
        UserAccountEntity user = requireUserEntity(userId);
        user.setStatus(status);
    }

    @Transactional
    public UserAccount updateUserProfile(long userId, String nickname, String avatarUrl) {
        UserAccountEntity user = requireUserEntity(userId);
        applyUserProfile(user, nickname, avatarUrl);
        return toDomain(user);
    }

    @Transactional
    public UserAccount bindWechatUser(long userId, String wechatKey, String nickname, String avatarUrl) {
        UserAccountEntity user = requireUserEntity(userId);
        Optional<UserAccountEntity> existing = userAccountRepository.findByWechatKey(wechatKey);
        if (existing.isPresent() && !existing.get().getId().equals(userId)) {
            throw new IllegalArgumentException("该微信账号已绑定其他用户");
        }
        user.setWechatKey(wechatKey);
        applyUserProfile(user, nickname, avatarUrl);
        return toDomain(user);
    }

    @Transactional
    public PetProfile createPet(long ownerId, String name, String type, Integer ageMonths) {
        PetProfileEntity pet = new PetProfileEntity();
        pet.setOwnerId(ownerId);
        pet.setName(name);
        pet.setType(type);
        pet.setAgeMonths(ageMonths);
        boolean hasDefault = petProfileRepository.existsByOwnerIdAndDefaultPetTrue(ownerId);
        pet.setDefaultPet(!hasDefault);
        return toDomain(petProfileRepository.save(pet));
    }

    @Transactional
    public PetProfile updatePet(long ownerId, long petId, String name, String type, Integer ageMonths) {
        PetProfileEntity pet = requirePetEntity(petId);
        if (!pet.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("只能编辑自己的宠物");
        }
        pet.setName(name);
        pet.setType(type);
        pet.setAgeMonths(ageMonths);
        return toDomain(pet);
    }

    @Transactional
    public void setDefaultPet(long ownerId, long petId) {
        List<PetProfileEntity> pets = petProfileRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        boolean owned = false;
        for (PetProfileEntity candidate : pets) {
            boolean isTarget = candidate.getId().equals(petId);
            candidate.setDefaultPet(isTarget);
            if (isTarget) {
                owned = true;
            }
        }
        if (!owned) {
            throw new IllegalArgumentException("只能设置自己的宠物");
        }
    }

    @Transactional(readOnly = true)
    public List<PetProfile> listPetsByOwner(long ownerId) {
        return petProfileRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PetProfile> findPet(long petId) {
        return petProfileRepository.findById(petId).map(this::toDomain);
    }

    @Transactional
    public CircleEntry createCircle(long ownerId, String name, String description) {
        requireUserEntity(ownerId);
        CircleEntryEntity circle = new CircleEntryEntity();
        circle.setOwnerId(ownerId);
        circle.setName(name);
        circle.setDescription(description);
        circle.setDissolved(false);
        CircleEntryEntity saved = circleEntryRepository.save(circle);
        addCircleMemberInternal(saved.getId(), ownerId);
        return toDomain(saved);
    }

    @Transactional
    public CircleEntry updateCircle(long ownerId, long circleId, String name, String description) {
        CircleEntryEntity circle = requireActiveCircleEntity(circleId);
        if (!circle.getOwnerId().equals(ownerId)) {
            throw new SecurityException("只有圈主可以编辑圈子");
        }
        circle.setName(name);
        circle.setDescription(description);
        return toDomain(circle);
    }

    @Transactional
    public void dissolveCircle(long ownerId, long circleId) {
        CircleEntryEntity circle = requireActiveCircleEntity(circleId);
        if (!circle.getOwnerId().equals(ownerId)) {
            throw new SecurityException("只有圈主可以解散圈子");
        }
        circle.setDissolved(true);
        deactivateCircleInvites(circleId);
    }

    @Transactional(readOnly = true)
    public CircleEntry getCircle(long circleId) {
        return circleEntryRepository.findById(circleId).map(this::toDomain).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CircleEntry> listCirclesByUser(long userId) {
        List<CircleMemberEntryEntity> memberships = circleMemberRepository.findByUserIdOrderByJoinedAtDesc(userId);
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, CircleEntry> circles = new LinkedHashMap<>();
        for (CircleMemberEntryEntity membership : memberships) {
            circleEntryRepository.findById(membership.getCircleId())
                .filter(circle -> !circle.isDissolved())
                .map(this::toDomain)
                .ifPresent(circle -> circles.putIfAbsent(circle.getId(), circle));
        }
        return new ArrayList<>(circles.values());
    }

    @Transactional(readOnly = true)
    public List<CircleMemberEntry> listCircleMembers(long circleId) {
        return circleMemberRepository.findByCircleIdOrderByJoinedAtAsc(circleId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Transactional(readOnly = true)
    public long countCircleMembers(long circleId) {
        return circleMemberRepository.countByCircleId(circleId);
    }

    @Transactional(readOnly = true)
    public boolean isCircleMember(long circleId, long userId) {
        return circleEntryRepository.findById(circleId)
            .filter(circle -> !circle.isDissolved())
            .isPresent() && circleMemberRepository.existsByCircleIdAndUserId(circleId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isCircleOwner(long circleId, long userId) {
        return circleEntryRepository.findById(circleId)
            .filter(circle -> !circle.isDissolved())
            .map(circle -> circle.getOwnerId().equals(userId))
            .orElse(false);
    }

    @Transactional
    public CircleInviteEntry createCircleInvite(long circleId, long createdBy, String token, Instant expiresAt) {
        CircleInviteEntryEntity invite = new CircleInviteEntryEntity();
        invite.setCircleId(circleId);
        invite.setCreatedBy(createdBy);
        invite.setToken(token);
        invite.setActive(true);
        invite.setExpiresAt(expiresAt);
        return toDomain(circleInviteRepository.save(invite));
    }

    @Transactional(readOnly = true)
    public CircleInviteEntry findCircleInviteByToken(String token) {
        return circleInviteRepository.findByToken(token).map(this::toDomain).orElse(null);
    }

    @Transactional
    public boolean addCircleMember(long circleId, long userId) {
        requireActiveCircleEntity(circleId);
        requireUserEntity(userId);
        return addCircleMemberInternal(circleId, userId);
    }

    @Transactional
    public boolean removeCircleMember(long circleId, long userId) {
        Optional<CircleMemberEntryEntity> membership = circleMemberRepository.findByCircleIdAndUserId(circleId, userId);
        if (membership.isEmpty()) {
            return false;
        }
        circleMemberRepository.delete(membership.get());
        return true;
    }

    @Transactional
    public void deactivateCircleInvites(long circleId) {
        circleInviteRepository.findByCircleIdAndActiveTrue(circleId).forEach(invite -> invite.setActive(false));
    }

    @Transactional
    public PostEntry createPost(long authorId, Long petId, String content, List<String> imageUrls,
                                String videoUrl, String topic, PostVisibility visibility, List<Long> visibleCircleIds,
                                ReviewStatus reviewStatus, String reviewReason) {
        PostEntryEntity post = new PostEntryEntity();
        post.setAuthorId(authorId);
        post.setPetId(petId);
        post.setContent(content);
        post.setImageUrlsJson(serializeStringList(imageUrls));
        post.setVideoUrl(videoUrl);
        post.setTopic(topic);
        post.setVisibility(visibility);
        post.setVisibleCircleIdsJson(serializeLongList(visibleCircleIds));
        post.setReviewStatus(reviewStatus);
        post.setReviewReason(reviewReason);
        post.setLikeCount(0);
        post.setCommentCount(0);
        return toDomain(postEntryRepository.save(post));
    }

    @Transactional(readOnly = true)
    public PostEntry getPost(long postId) {
        return postEntryRepository.findById(postId).map(this::toDomain).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<PostEntry> listAllPosts() {
        return postEntryRepository.findAllOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public List<PostEntry> listApprovedVisiblePosts(long userId) {
        return postEntryRepository.findAllOrderByCreatedAtDesc().stream()
            .filter(post -> canUserViewPost(post, userId))
            .map(this::toDomain)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PostEntry> listApprovedPostsForCircle(long circleId, long userId) {
        return postEntryRepository.findAllOrderByCreatedAtDesc().stream()
            .filter(post -> post.getReviewStatus() == ReviewStatus.APPROVED)
            .filter(post -> resolveVisibility(post.getVisibility()) == PostVisibility.CIRCLE)
            .filter(post -> deserializeLongList(post.getVisibleCircleIdsJson()).contains(circleId))
            .filter(post -> canUserViewPost(post, userId))
            .map(this::toDomain)
            .toList();
    }

    @Transactional
    public void updatePostReview(long postId, ReviewStatus status, String reason) {
        PostEntryEntity post = requirePostEntity(postId);
        post.setReviewStatus(status);
        post.setReviewReason(reason);
    }

    @Transactional
    public void deletePost(long postId) {
        PostEntryEntity post = requirePostEntity(postId);
        post.setReviewStatus(ReviewStatus.DELETED);
        post.setReviewReason("已删除");
    }

    @Transactional
    public boolean addLike(long postId, long userId) {
        PostEntryEntity post = requirePostEntity(postId);
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            return false;
        }
        PostLikeEntity like = new PostLikeEntity();
        like.setPostId(postId);
        like.setUserId(userId);
        try {
            postLikeRepository.saveAndFlush(like);
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
        post.setLikeCount(post.getLikeCount() + 1);
        return true;
    }

    @Transactional
    public boolean removeLike(long postId, long userId) {
        PostEntryEntity post = requirePostEntity(postId);
        Optional<PostLikeEntity> like = postLikeRepository.findByPostIdAndUserId(postId, userId);
        if (like.isEmpty()) {
            return false;
        }
        postLikeRepository.delete(like.get());
        postLikeRepository.flush();
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        return true;
    }

    @Transactional(readOnly = true)
    public boolean hasLiked(long postId, long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Transactional
    public CommentEntry createComment(long postId, long authorId, Long parentCommentId, Long replyToUserId,
                                      List<Long> mentionUserIds, String content) {
        PostEntryEntity post = requirePostEntity(postId);
        if (parentCommentId != null) {
            CommentEntryEntity parent = requireCommentEntity(parentCommentId);
            if (!Objects.equals(parent.getPostId(), postId) || parent.isDeleted()) {
                throw new IllegalArgumentException("回复的目标评论不存在");
            }
        }
        CommentEntryEntity comment = new CommentEntryEntity();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setParentCommentId(parentCommentId);
        comment.setReplyToUserId(replyToUserId);
        comment.setMentionUserIdsJson(serializeLongList(mentionUserIds));
        comment.setContent(content);
        comment.setDeleted(false);
        CommentEntryEntity saved = commentEntryRepository.save(comment);
        refreshCommentCount(post);
        return toDomain(saved);
    }

    @Transactional
    public void deleteComment(long commentId) {
        CommentEntryEntity comment = requireCommentEntity(commentId);
        if (!comment.isDeleted()) {
            comment.setDeleted(true);
            PostEntryEntity post = requirePostEntity(comment.getPostId());
            refreshCommentCount(post);
        }
    }

    @Transactional(readOnly = true)
    public List<CommentEntry> listCommentsByPost(long postId) {
        return commentEntryRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Transactional(readOnly = true)
    public CommentEntry getComment(long commentId) {
        return commentEntryRepository.findById(commentId).map(this::toDomain).orElse(null);
    }

    @Transactional
    public ReportEntry createReport(long reporterId, String targetType, long targetId, String reason, String status) {
        ReportEntryEntity report = new ReportEntryEntity();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setStatus(status);
        return toDomain(reportEntryRepository.save(report));
    }

    @Transactional(readOnly = true)
    public Map<Long, UserAccount> snapshotUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, UserAccount> snapshot = new HashMap<>();
        userAccountRepository.findAllById(userIds).forEach(entity -> snapshot.put(entity.getId(), toDomain(entity)));
        return snapshot;
    }

    @Transactional(readOnly = true)
    public boolean canUserViewPost(long postId, long userId) {
        return postEntryRepository.findById(postId).filter(post -> canUserViewPost(post, userId)).isPresent();
    }

    private boolean addCircleMemberInternal(long circleId, long userId) {
        if (circleMemberRepository.existsByCircleIdAndUserId(circleId, userId)) {
            return false;
        }
        CircleMemberEntryEntity membership = new CircleMemberEntryEntity();
        membership.setCircleId(circleId);
        membership.setUserId(userId);
        try {
            circleMemberRepository.saveAndFlush(membership);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private boolean canUserViewPost(PostEntryEntity post, long userId) {
        if (post.getReviewStatus() != ReviewStatus.APPROVED) {
            return false;
        }
        if (resolveVisibility(post.getVisibility()) == PostVisibility.PUBLIC) {
            return true;
        }
        for (Long circleId : deserializeLongList(post.getVisibleCircleIdsJson())) {
            if (isCircleMember(circleId, userId)) {
                return true;
            }
        }
        return false;
    }

    private UserAccount createPhoneUser(String phone) {
        UserAccountEntity user = new UserAccountEntity();
        user.setNickname("用户" + phone.substring(Math.max(0, phone.length() - 4)));
        user.setPhone(phone);
        user.setAvatarUrl(null);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        try {
            return toDomain(userAccountRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            return userAccountRepository.findByPhone(phone).map(this::toDomain).orElseThrow();
        }
    }

    private UserAccount createWechatUser(String wechatKey, String nickname, String avatarUrl) {
        UserAccountEntity user = new UserAccountEntity();
        user.setNickname(nickname == null || nickname.isBlank()
            ? "微信用户" + wechatKey.substring(Math.max(0, wechatKey.length() - 4))
            : nickname);
        user.setWechatKey(wechatKey);
        user.setAvatarUrl(normalizeAvatarUrl(avatarUrl));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        try {
            return toDomain(userAccountRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            return userAccountRepository.findByWechatKey(wechatKey).map(this::toDomain).orElseThrow();
        }
    }

    private UserAccount createAdminUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setNickname("系统管理员");
        user.setPhone(ADMIN_PHONE);
        user.setWechatKey(ADMIN_WECHAT_KEY);
        user.setAvatarUrl(null);
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        try {
            return toDomain(userAccountRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            return userAccountRepository.findByWechatKey(ADMIN_WECHAT_KEY).map(this::toDomain).orElseThrow();
        }
    }

    private UserAccountEntity requireUserEntity(long userId) {
        return userAccountRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private PetProfileEntity requirePetEntity(long petId) {
        return petProfileRepository.findById(petId).orElseThrow(() -> new IllegalArgumentException("宠物不存在"));
    }

    private CircleEntryEntity requireActiveCircleEntity(long circleId) {
        CircleEntryEntity circle = circleEntryRepository.findById(circleId)
            .orElseThrow(() -> new IllegalArgumentException("圈子不存在"));
        if (circle.isDissolved()) {
            throw new IllegalArgumentException("圈子已解散");
        }
        return circle;
    }

    private PostEntryEntity requirePostEntity(long postId) {
        return postEntryRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("动态不存在"));
    }

    private CommentEntryEntity requireCommentEntity(long commentId) {
        return commentEntryRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("评论不存在"));
    }

    private void refreshCommentCount(PostEntryEntity post) {
        post.setCommentCount((int) commentEntryRepository.countByPostIdAndDeletedFalse(post.getId()));
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
            entity.getId(),
            entity.getNickname(),
            entity.getPhone(),
            entity.getWechatKey(),
            entity.getAvatarUrl(),
            entity.getRole(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }

    private void applyUserProfile(UserAccountEntity user, String nickname, String avatarUrl) {
        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname.trim());
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(normalizeAvatarUrl(avatarUrl));
        }
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String normalized = avatarUrl.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private PetProfile toDomain(PetProfileEntity entity) {
        return new PetProfile(
            entity.getId(),
            entity.getOwnerId(),
            entity.getName(),
            entity.getType(),
            entity.getAgeMonths(),
            entity.isDefaultPet(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private CircleEntry toDomain(CircleEntryEntity entity) {
        return new CircleEntry(
            entity.getId(),
            entity.getOwnerId(),
            entity.getName(),
            entity.getDescription(),
            entity.isDissolved(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private CircleMemberEntry toDomain(CircleMemberEntryEntity entity) {
        return new CircleMemberEntry(entity.getId(), entity.getCircleId(), entity.getUserId(), entity.getJoinedAt());
    }

    private CircleInviteEntry toDomain(CircleInviteEntryEntity entity) {
        return new CircleInviteEntry(
            entity.getId(),
            entity.getCircleId(),
            entity.getCreatedBy(),
            entity.getToken(),
            entity.isActive(),
            entity.getExpiresAt(),
            entity.getCreatedAt()
        );
    }

    private PostEntry toDomain(PostEntryEntity entity) {
        PostEntry post = new PostEntry(
            entity.getId(),
            entity.getAuthorId(),
            entity.getPetId(),
            entity.getContent(),
            deserializeStringList(entity.getImageUrlsJson()),
            entity.getVideoUrl(),
            entity.getTopic(),
            resolveVisibility(entity.getVisibility()),
            deserializeLongList(entity.getVisibleCircleIdsJson()),
            entity.getReviewStatus(),
            entity.getReviewReason(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
        post.setLikeCount(entity.getLikeCount());
        post.setCommentCount(entity.getCommentCount());
        return post;
    }

    private CommentEntry toDomain(CommentEntryEntity entity) {
        return new CommentEntry(
            entity.getId(),
            entity.getPostId(),
            entity.getAuthorId(),
            entity.getParentCommentId(),
            entity.getReplyToUserId(),
            deserializeLongList(entity.getMentionUserIdsJson()),
            entity.getContent(),
            entity.isDeleted(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private ReportEntry toDomain(ReportEntryEntity entity) {
        return new ReportEntry(
            entity.getId(),
            entity.getReporterId(),
            entity.getTargetType(),
            entity.getTargetId(),
            entity.getReason(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }

    private PostVisibility resolveVisibility(PostVisibility visibility) {
        return visibility == null ? PostVisibility.PUBLIC : visibility;
    }

    private String serializeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Collections.emptyList() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("列表序列化失败", exception);
        }
    }

    private String serializeLongList(List<Long> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Collections.emptyList() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ID 列表序列化失败", exception);
        }
    }

    private List<String> deserializeStringList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawValue, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("字符串列表反序列化失败", exception);
        }
    }

    private List<Long> deserializeLongList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawValue, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("ID 列表反序列化失败", exception);
        }
    }
}
