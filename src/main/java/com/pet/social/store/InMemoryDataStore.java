package com.pet.social.store;

import com.pet.social.domain.CommentEntry;
import com.pet.social.domain.PetProfile;
import com.pet.social.domain.PostEntry;
import com.pet.social.domain.ReviewStatus;
import com.pet.social.domain.UserAccount;
import com.pet.social.domain.UserRole;
import com.pet.social.domain.UserStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryDataStore {
    private final AtomicLong userIdGenerator = new AtomicLong(1000);
    private final AtomicLong petIdGenerator = new AtomicLong(2000);
    private final AtomicLong postIdGenerator = new AtomicLong(3000);
    private final AtomicLong commentIdGenerator = new AtomicLong(4000);

    private final Map<Long, UserAccount> users = new ConcurrentHashMap<>();
    private final Map<String, Long> phoneIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> wechatIndex = new ConcurrentHashMap<>();
    private final Map<Long, PetProfile> pets = new ConcurrentHashMap<>();
    private final Map<Long, PostEntry> posts = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> postLikes = new ConcurrentHashMap<>();
    private final Map<Long, CommentEntry> comments = new ConcurrentHashMap<>();

    private volatile Long adminUserId;

    public synchronized UserAccount getOrCreatePhoneUser(String phone) {
        Long existingId = phoneIndex.get(phone);
        if (existingId != null) {
            return users.get(existingId);
        }
        long userId = userIdGenerator.incrementAndGet();
        UserAccount user = new UserAccount(
            userId,
            "用户" + phone.substring(Math.max(0, phone.length() - 4)),
            phone,
            null,
            UserRole.USER,
            UserStatus.ACTIVE,
            Instant.now()
        );
        users.put(userId, user);
        phoneIndex.put(phone, userId);
        return user;
    }

    public synchronized UserAccount getOrCreateWechatUser(String wechatKey, String nickname) {
        Long existingId = wechatIndex.get(wechatKey);
        if (existingId != null) {
            return users.get(existingId);
        }
        long userId = userIdGenerator.incrementAndGet();
        UserAccount user = new UserAccount(
            userId,
            nickname == null || nickname.isBlank() ? "微信用户" + wechatKey.substring(Math.max(0, wechatKey.length() - 4)) : nickname,
            null,
            wechatKey,
            UserRole.USER,
            UserStatus.ACTIVE,
            Instant.now()
        );
        users.put(userId, user);
        wechatIndex.put(wechatKey, userId);
        return user;
    }

    public synchronized UserAccount getOrCreateAdminUser() {
        if (adminUserId != null) {
            return users.get(adminUserId);
        }
        long userId = userIdGenerator.incrementAndGet();
        UserAccount user = new UserAccount(
            userId,
            "系统管理员",
            "admin@local",
            "__admin__",
            UserRole.ADMIN,
            UserStatus.ACTIVE,
            Instant.now()
        );
        users.put(userId, user);
        phoneIndex.put("admin@local", userId);
        wechatIndex.put("__admin__", userId);
        adminUserId = userId;
        return user;
    }

    public UserAccount getUser(long userId) {
        return users.get(userId);
    }

    public synchronized void updateUserStatus(long userId, UserStatus status) {
        UserAccount user = requireUser(userId);
        user.setStatus(status);
    }

    public synchronized PetProfile createPet(long ownerId, String name, String type, Integer ageMonths) {
        long petId = petIdGenerator.incrementAndGet();
        boolean shouldBeDefault = listPetsByOwner(ownerId).stream().noneMatch(PetProfile::isDefaultPet);
        PetProfile pet = new PetProfile(
            petId,
            ownerId,
            name,
            type,
            ageMonths,
            shouldBeDefault,
            Instant.now(),
            Instant.now()
        );
        pets.put(petId, pet);
        if (shouldBeDefault) {
            setDefaultPet(ownerId, petId);
        }
        return pet;
    }

    public synchronized PetProfile updatePet(long ownerId, long petId, String name, String type, Integer ageMonths) {
        PetProfile pet = requirePet(petId);
        if (pet.getOwnerId() != ownerId) {
            throw new IllegalArgumentException("只能编辑自己的宠物");
        }
        pet.update(name, type, ageMonths);
        return pet;
    }

    public synchronized void setDefaultPet(long ownerId, long petId) {
        PetProfile pet = requirePet(petId);
        if (pet.getOwnerId() != ownerId) {
            throw new IllegalArgumentException("只能设置自己的宠物");
        }
        for (PetProfile candidate : pets.values()) {
            if (candidate.getOwnerId() == ownerId) {
                candidate.setDefaultPet(candidate.getId() == petId);
            }
        }
    }

    public List<PetProfile> listPetsByOwner(long ownerId) {
        return pets.values().stream()
            .filter(pet -> pet.getOwnerId() == ownerId)
            .sorted(Comparator.comparing(PetProfile::getCreatedAt).reversed())
            .toList();
    }

    public Optional<PetProfile> findPet(long petId) {
        return Optional.ofNullable(pets.get(petId));
    }

    public synchronized PostEntry createPost(long authorId, Long petId, String content, List<String> imageUrls,
                                             String videoUrl, String topic, ReviewStatus reviewStatus, String reviewReason) {
        long postId = postIdGenerator.incrementAndGet();
        PostEntry post = new PostEntry(
            postId,
            authorId,
            petId,
            content,
            imageUrls,
            videoUrl,
            topic,
            reviewStatus,
            reviewReason,
            Instant.now(),
            Instant.now()
        );
        posts.put(postId, post);
        postLikes.put(postId, new HashSet<>());
        return post;
    }

    public PostEntry getPost(long postId) {
        return posts.get(postId);
    }

    public List<PostEntry> listAllPosts() {
        return posts.values().stream()
            .sorted(Comparator.comparing(PostEntry::getCreatedAt).reversed())
            .toList();
    }

    public List<PostEntry> listApprovedPublicPosts() {
        return posts.values().stream()
            .filter(post -> post.getReviewStatus() == ReviewStatus.APPROVED)
            .sorted(Comparator.comparing(PostEntry::getCreatedAt).reversed())
            .toList();
    }

    public synchronized void updatePostReview(long postId, ReviewStatus status, String reason) {
        PostEntry post = requirePost(postId);
        post.setReviewStatus(status);
        post.setReviewReason(reason);
    }

    public synchronized void deletePost(long postId) {
        PostEntry post = requirePost(postId);
        post.setReviewStatus(ReviewStatus.DELETED);
        post.setReviewReason("已删除");
    }

    public synchronized boolean addLike(long postId, long userId) {
        PostEntry post = requirePost(postId);
        Set<Long> likes = postLikes.computeIfAbsent(postId, key -> new HashSet<>());
        boolean changed = likes.add(userId);
        if (changed) {
            post.setLikeCount(likes.size());
        }
        return changed;
    }

    public synchronized boolean removeLike(long postId, long userId) {
        PostEntry post = requirePost(postId);
        Set<Long> likes = postLikes.computeIfAbsent(postId, key -> new HashSet<>());
        boolean changed = likes.remove(userId);
        if (changed) {
            post.setLikeCount(likes.size());
        }
        return changed;
    }

    public boolean hasLiked(long postId, long userId) {
        return postLikes.getOrDefault(postId, Set.of()).contains(userId);
    }

    public synchronized CommentEntry createComment(long postId, long authorId, String content) {
        PostEntry post = requirePost(postId);
        long commentId = commentIdGenerator.incrementAndGet();
        CommentEntry comment = new CommentEntry(commentId, postId, authorId, content, false, Instant.now(), Instant.now());
        comments.put(commentId, comment);
        post.setCommentCount((int) comments.values().stream()
            .filter(item -> item.getPostId() == postId && !item.isDeleted())
            .count());
        return comment;
    }

    public synchronized void deleteComment(long commentId) {
        CommentEntry comment = requireComment(commentId);
        if (!comment.isDeleted()) {
            comment.markDeleted();
            PostEntry post = requirePost(comment.getPostId());
            post.setCommentCount((int) comments.values().stream()
                .filter(item -> item.getPostId() == post.getId() && !item.isDeleted())
                .count());
        }
    }

    public List<CommentEntry> listCommentsByPost(long postId) {
        return comments.values().stream()
            .filter(comment -> comment.getPostId() == postId && !comment.isDeleted())
            .sorted(Comparator.comparing(CommentEntry::getCreatedAt))
            .toList();
    }

    public CommentEntry getComment(long commentId) {
        return comments.get(commentId);
    }

    public Map<Long, UserAccount> snapshotUsers(List<Long> userIds) {
        Map<Long, UserAccount> snapshot = new HashMap<>();
        for (Long userId : userIds) {
            if (userId != null && users.containsKey(userId)) {
                snapshot.put(userId, users.get(userId));
            }
        }
        return snapshot;
    }

    private UserAccount requireUser(long userId) {
        UserAccount user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private PetProfile requirePet(long petId) {
        PetProfile pet = pets.get(petId);
        if (pet == null) {
            throw new IllegalArgumentException("宠物不存在");
        }
        return pet;
    }

    private PostEntry requirePost(long postId) {
        PostEntry post = posts.get(postId);
        if (post == null) {
            throw new IllegalArgumentException("动态不存在");
        }
        return post;
    }

    private CommentEntry requireComment(long commentId) {
        CommentEntry comment = comments.get(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        return comment;
    }
}
