package com.my.blog.controller;

import com.my.blog.dto.CreatePostDto;
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

import java.util.ArrayList;
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
            @RequestParam("search") String search,
            @RequestParam("pageNumber") Integer pageNumber,
            @RequestParam("pageSize") Integer pageSize
    ) {

        final var postsModels = new ArrayList<PostModel>();

        postsModels.add(new PostModel(1, "Name", "text", new ArrayList<>(), 1, 1));

        return new PostsDto(postsModels, false, false, 1);
    }

    @GetMapping("/{id}")
    PostModel getPost(@PathVariable("id") long id) {
        return postService.getPost(id);
    }

    @PostMapping()
    void createPost(@RequestBody CreatePostDto createPostDto) {
        postService.savePost(createPostDto);
    }

    @PutMapping("/{id}")
    PostModel updatePost(@PathVariable int id, @RequestBody PostsDto postsDto) {
        return new PostModel(1, "Name", "text", new ArrayList<>(), 1, 1);
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
    public String updateImage(@PathVariable("id") Long postId, @RequestParam("file") MultipartFile file) {
        final var filePath = filesService.upload(file);

        if (filePath == null) {
            throw new RuntimeException("Unable to upload file");
        }

        postService.updatePostImage(postId, filePath);

        return filePath;
    }

    @GetMapping("/{id}/image")
    ResponseEntity<Resource> getImage(@PathVariable("id")  Long id) {
        final var imagePath = postService.getPostImagePath(id);

        Resource file = filesService.download(imagePath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @GetMapping("/{id}/comments")
    List<CommentModel> getComments(@PathVariable("id") Long id) {
        return postService.getComments(id);
    }

}
