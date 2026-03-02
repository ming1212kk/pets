package com.pet.social.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.ReviewStatus;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import com.pet.social.domain.UserStatus;
import com.pet.social.entity.CommentEntryEntity;
import com.pet.social.entity.PetProfileEntity;
import com.pet.social.entity.PostEntryEntity;
import com.pet.social.entity.PostLikeEntity;
import com.pet.social.entity.UserAccountEntity;
import com.pet.social.repository.CommentEntryRepository;
import com.pet.social.repository.PetProfileRepository;
import com.pet.social.repository.PostEntryRepository;
import com.pet.social.repository.PostLikeRepository;
import com.pet.social.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final ObjectMapper objectMapper;

    public AppDataStore(UserAccountRepository userAccountRepository,
                        PetProfileRepository petProfileRepository,
                        PostEntryRepository postEntryRepository,
                        PostLikeRepository postLikeRepository,
                        CommentEntryRepository commentEntryRepository,
                        ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.petProfileRepository = petProfileRepository;
        this.postEntryRepository = postEntryRepository;
        this.postLikeRepository = postLikeRepository;
        this.commentEntryRepository = commentEntryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserAccount getOrCreatePhoneUser(String phone) {
        return userAccountRepository.findByPhone(phone)
            .map(this::toDomain)
            .orElseGet(() -> createPhoneUser(phone));
    }

    @Transactional
    public UserAccount getOrCreateWechatUser(String wechatKey, String nickname) {
        return userAccountRepository.findByWechatKey(wechatKey)
            .map(this::toDomain)
            .orElseGet(() -> createWechatUser(wechatKey, nickname));
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
    public PostEntry createPost(long authorId, Long petId, String content, List<String> imageUrls,
                                String videoUrl, String topic, ReviewStatus reviewStatus, String reviewReason) {
        PostEntryEntity post = new PostEntryEntity();
        post.setAuthorId(authorId);
        post.setPetId(petId);
        post.setContent(content);
        post.setImageUrlsJson(serializeImageUrls(imageUrls));
        post.setVideoUrl(videoUrl);
        post.setTopic(topic);
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
    public List<PostEntry> listApprovedPublicPosts() {
        return postEntryRepository.findByReviewStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED).stream().map(this::toDomain).toList();
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
    public CommentEntry createComment(long postId, long authorId, String content) {
        PostEntryEntity post = requirePostEntity(postId);
        CommentEntryEntity comment = new CommentEntryEntity();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
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

    @Transactional(readOnly = true)
    public Map<Long, UserAccount> snapshotUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, UserAccount> snapshot = new HashMap<>();
        userAccountRepository.findAllById(userIds).forEach(entity -> snapshot.put(entity.getId(), toDomain(entity)));
        return snapshot;
    }

    private UserAccount createPhoneUser(String phone) {
        UserAccountEntity user = new UserAccountEntity();
        user.setNickname("用户" + phone.substring(Math.max(0, phone.length() - 4)));
        user.setPhone(phone);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        try {
            return toDomain(userAccountRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            return userAccountRepository.findByPhone(phone).map(this::toDomain).orElseThrow();
        }
    }

    private UserAccount createWechatUser(String wechatKey, String nickname) {
        UserAccountEntity user = new UserAccountEntity();
        user.setNickname(nickname == null || nickname.isBlank()
            ? "微信用户" + wechatKey.substring(Math.max(0, wechatKey.length() - 4))
            : nickname);
        user.setWechatKey(wechatKey);
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

    private PostEntryEntity requirePostEntity(long postId) {
        return postEntryRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("动态不存在"));
    }

    private CommentEntryEntity requireCommentEntity(long commentId) {
        return commentEntryRepository.findById(commentId).orElseThrow(() -> new IllegalArgumentException("评论不存在"));
    }

    private void refreshLikeCount(PostEntryEntity post) {
        post.setLikeCount((int) postLikeRepository.countByPostId(post.getId()));
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
            entity.getRole(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
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

    private PostEntry toDomain(PostEntryEntity entity) {
        PostEntry post = new PostEntry(
            entity.getId(),
            entity.getAuthorId(),
            entity.getPetId(),
            entity.getContent(),
            deserializeImageUrls(entity.getImageUrlsJson()),
            entity.getVideoUrl(),
            entity.getTopic(),
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
            entity.getContent(),
            entity.isDeleted(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private String serializeImageUrls(List<String> imageUrls) {
        try {
            return objectMapper.writeValueAsString(imageUrls == null ? Collections.emptyList() : imageUrls);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("图片列表序列化失败", exception);
        }
    }

    private List<String> deserializeImageUrls(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawValue, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new IllegalStateException("图片列表反序列化失败", exception);
        }
    }
}
