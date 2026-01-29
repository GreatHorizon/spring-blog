package com.my.blog.controller;

import com.my.blog.dto.PostUpdateDto;
import com.my.blog.dto.PostsDto;
import com.my.blog.model.CommentModel;
import com.my.blog.model.PostModel;
import com.my.blog.service.FilesService;
import com.my.blog.service.PostService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    final PostService postService;
    final FilesService filesService;

    PostController(PostService postService, FilesService filesService) {
        this.postService = postService;
        this.filesService = filesService;
    }

    @GetMapping
    PostsDto getPosts(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "pageNumber") Integer pageNumber,
            @RequestParam(name = "pageSize") Integer pageSize
    ) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    PostModel getPost(@PathVariable("id") long id) {
        return postService.getPost(id);
    }

    @PostMapping()
    PostModel createPost(@RequestBody PostUpdateDto postUpdateDto) {
        return postService.createPost(postUpdateDto);
    }

    @PutMapping("/{id}")
    PostModel updatePost(@RequestBody PostUpdateDto postsDto) {
        return postService.updatePost(postsDto);
    }

    @DeleteMapping("/{id}")
    void deletePost(@PathVariable("id") long id) {
        postService.deletePost(id);
    }

    @PostMapping("/{id}/likes")
    int incrementLikesCount(@PathVariable("id") Long id) {
        return postService.incrementLikesCount(id);
    }

    @PutMapping("/{id}/image")
    public String updateImage(@PathVariable("id") Long postId, @RequestParam("image") MultipartFile file) {
        final var filePath = filesService.upload(file);

        if (filePath == null) {
            throw new RuntimeException("Unable to upload file");
        }

        postService.updatePostImage(postId, filePath);

        return filePath;
    }

    @GetMapping("/{id}/image")
    ResponseEntity<Resource> getImage(@PathVariable("id") Long id) {
        final var imagePath = postService.getPostImagePath(id);

        if (imagePath.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        Resource file = filesService.download(imagePath.get());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @GetMapping("/{id}/comments")
    List<CommentModel> getComments(@PathVariable("id") Long id) {
        return postService.getComments(id);
    }

    @GetMapping("/{post_id}/comments/{comment_id}")
    CommentModel getComment(@PathVariable("post_id") Long ignoredPostId, @PathVariable("comment_id") Long commentId) {
        return postService.getComment(commentId);
    }

    @PostMapping("/{post_id}/comments")
    CommentModel createComment(@PathVariable("post_id") Long ignoredPostId, @RequestBody CommentModel commentModel) {
        return postService.createComment(commentModel);
    }

    @PutMapping("/{post_id}/comments/{comment_id}")
    CommentModel updateComment(@PathVariable("post_id") Long ignoredPostId, @PathVariable("comment_id") Long ignoredCommentId, @RequestBody CommentModel commentModel) {
        return postService.updateComment(commentModel);
    }

    @DeleteMapping("/{post_id}/comments/{comment_id}")
    void deleteComment(@PathVariable("post_id") Long ignoredPostId, @PathVariable("comment_id") Long commentId) {
        postService.deleteComment(commentId);
    }


}
