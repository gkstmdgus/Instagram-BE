package instagram.instagrambe.post.service;

import instagram.instagrambe.comment.dto.CommentResponseDto;
import instagram.instagrambe.comment.repository.CommentRepository;
import instagram.instagrambe.post.dto.PostRequestDto;
import instagram.instagrambe.post.dto.PostResponseDto;
import instagram.instagrambe.post.entity.Post;
import instagram.instagrambe.post.image.common.S3Uploader;
import instagram.instagrambe.post.repository.PostLikeRepository;
import instagram.instagrambe.post.repository.PostRepository;
import instagram.instagrambe.user.entity.User;
import instagram.instagrambe.util.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static instagram.instagrambe.util.ErrorCode.FORBIDDEN_DATA;
import static instagram.instagrambe.util.ErrorCode.NOT_FOUND_DATA;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final S3Uploader s3Uploader;
    private final PostLikeRepository likeRepository;
    private final CommentRepository commentRepository;

    // 게시글 작성
    @Transactional
    public ResponseEntity<PostResponseDto> createPost(MultipartFile image,
                                                      PostRequestDto postRequestDto,
                                                      User user) throws IOException {
//        System.out.println("postRepository.contents = " + postRepository.getContents());
//        System.out.println("-------user = " + user.getUsername());
        String storedFileName = s3Uploader.upload(image);//, "images"); //s3에 업로드하기

        postRequestDto.setImageUrl(storedFileName);

        Post post = postRepository.saveAndFlush(new Post(postRequestDto, user));
        return ResponseEntity.ok().body(PostResponseDto.of(post));
    }

    //게시글 전체 조회
    @Transactional
    public ResponseEntity<List<PostResponseDto>> getPosts(User user) {
        List<Post> postList = postRepository.findAllByOrderByModifiedAtDesc();
        List<PostResponseDto> postResponseDtoList =new ArrayList<>();

        for (Post post : postList) {
            boolean heart = isHeart(user, post);
            List<CommentResponseDto> commentResponseDtoList = getComments(post);
            postResponseDtoList.add(new PostResponseDto(post, commentResponseDtoList, heart));
        }
        return ResponseEntity.ok().body(postResponseDtoList);
    }

    // 게시글 상세 조회 (선택한 게시글 조회)
    @Transactional
    public ResponseEntity<PostResponseDto> getPost(Long post_id, User user) {
        Post post = postRepository.findById(post_id).orElseThrow(
                () -> new CustomException(NOT_FOUND_DATA));
        List<CommentResponseDto> commentResponseDtoList = getComments(post);
        boolean heart = isHeart(user, post);
        PostResponseDto postResponseDto = new PostResponseDto(post, commentResponseDtoList, heart);
        return ResponseEntity.ok().body(postResponseDto);
    }

    // 게시글 수정 (선택한 게시글 수정)
    @Transactional
    public ResponseEntity<PostResponseDto> updateBlog(Long post_id, PostRequestDto postRequestDto, User user) {
        Post post = postRepository.findById(post_id).orElseThrow(
                () -> new CustomException(NOT_FOUND_DATA));
//        if(user.getRole() == USER) { //관리자 없다고 가정.
        if (user.getUsername().equals(post.getUser().getUsername()))
            post.update(postRequestDto);
        else throw new CustomException(FORBIDDEN_DATA);
        return ResponseEntity.ok().body(PostResponseDto.of(post));
    }

    // 게시글 삭제 (선택한 게시글 삭제)
    @Transactional
    public ResponseEntity deleteBlog(Long post_id, User user) {
        Post post = postRepository.findById(post_id).orElseThrow(
                () -> new CustomException(NOT_FOUND_DATA));
//        if(user.getRole()==USER) { //관리자 없다고 가정.
        if (user.getUsername().equals(post.getUser().getUsername()))
            postRepository.deleteById(post_id);
        else throw new CustomException(FORBIDDEN_DATA);
        return ResponseEntity.ok().body("게시글 삭제 성공");
    }
}


