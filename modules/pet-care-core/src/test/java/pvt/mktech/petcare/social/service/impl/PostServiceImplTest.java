package pvt.mktech.petcare.social.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import pvt.mktech.petcare.common.redis.RedisUtil;
import pvt.mktech.petcare.common.web.UserContext;
import pvt.mktech.petcare.points.entity.codelist.ActionTypeOfPointsRecord;
import pvt.mktech.petcare.points.event.PointsEarnEvent;
import pvt.mktech.petcare.social.dto.request.PostSaveRequest;
import pvt.mktech.petcare.social.entity.Post;
import pvt.mktech.petcare.social.entity.codelist.AuditStatusOfContent;
import pvt.mktech.petcare.social.mapper.PostMapper;
import pvt.mktech.petcare.social.service.InteractionService;
import pvt.mktech.petcare.social.service.PostLabelService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceImplTest {

    private final InteractionService interactionService = mock(InteractionService.class);
    private final PostLabelService postLabelService = mock(PostLabelService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final RedisUtil redisUtil = mock(RedisUtil.class);
    private final PostMapper postMapper = mock(PostMapper.class);

    private final PostServiceImpl postService = new PostServiceImpl(interactionService, postLabelService, publisher, redisUtil);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "mapper", postMapper);
        UserContext.setUserId(123L);
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUserId();
    }

    @Test
    void savePostShouldCreateApprovedPostAndPublishEvent() {
        when(postMapper.insert(any(Post.class), anyBoolean())).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(100L);
            return 1;
        });

        PostSaveRequest request = new PostSaveRequest();
        request.setTitle("hello");
        request.setContent("world");
        request.setPostType("DAILY");

        Post saved = postService.savePost(request);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getUserId()).isEqualTo(123L);
        assertThat(saved.getAuditStatus()).isEqualTo(AuditStatusOfContent.APPROVED);

        ArgumentCaptor<PointsEarnEvent> eventCaptor = ArgumentCaptor.forClass(PointsEarnEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        PointsEarnEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(123L);
        assertThat(event.getActionType()).isEqualTo(ActionTypeOfPointsRecord.PUBLISH);
        assertThat(event.getBizId()).isEqualTo(100L);
    }

    @Test
    void savePostShouldAssociateLabelsWhenProvided() {
        when(postMapper.insert(any(Post.class), anyBoolean())).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(200L);
            return 1;
        });

        PostSaveRequest request = new PostSaveRequest();
        request.setTitle("with labels");
        request.setLabelIds(List.of(1L, 2L, 3L));

        postService.savePost(request);

        verify(postLabelService).savePostLabels(200L, List.of(1L, 2L, 3L));
    }

    @Test
    void savePostShouldNotPublishEventWhenLabelAssociationFails() {
        when(postMapper.insert(any(Post.class), anyBoolean())).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setId(300L);
            return 1;
        });
        doThrow(new RuntimeException("label failure")).when(postLabelService).savePostLabels(any(), any());

        PostSaveRequest request = new PostSaveRequest();
        request.setLabelIds(List.of(1L));

        assertThatThrownBy(() -> postService.savePost(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("label failure");

        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void savePostShouldBeTransactional() throws NoSuchMethodException {
        Transactional transactional = PostServiceImpl.class.getMethod("savePost", PostSaveRequest.class)
                .getAnnotation(Transactional.class);
        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).containsExactly(Exception.class);
    }
}
