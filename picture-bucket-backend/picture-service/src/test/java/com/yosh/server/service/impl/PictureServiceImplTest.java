package com.yosh.server.service.impl;

import com.yosh.common.model.dto.file.UploadPictureResult;
import com.yosh.common.model.dto.picture.PictureUploadRequest;
import com.yosh.common.model.entry.Picture;
import com.yosh.common.model.vo.LoginUserVO;
import com.yosh.common.model.vo.PictureVO;
import com.yosh.server.manger.FileManger;
import com.yosh.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PictureServiceImplTest {

    private final MultipartFile file = mock(MultipartFile.class);
    private final FileManger fileManger = mock(FileManger.class);
    private final UserService userService = mock(UserService.class);
    private final RecordingPictureService pictureService = new RecordingPictureService();
    private final LoginUserVO loginUser = LoginUserVO.builder()
            .id(1L)
            .build();

    @BeforeEach
    void setUp() {
        pictureService.clear();
        ReflectionTestUtils.setField(pictureService, "fileManger", fileManger);
        ReflectionTestUtils.setField(pictureService, "userService", userService);
        when(fileManger.uploadFile(eq(file), any(String.class))).thenReturn(UploadPictureResult.builder()
                .url("https://example.com/test.png")
                .picName("test")
                .picSize(1024L)
                .picWidth(100)
                .picHeight(50)
                .picScale(2D)
                .picFormat("png")
                .build());
        when(userService.isAdmin(loginUser)).thenReturn(false);
    }

    @Test
    void uploadPictureCreatesPictureWhenRequestDoesNotContainId() {
        PictureVO result = pictureService.uploadPicture(file, new PictureUploadRequest(), loginUser);

        assertThat(pictureService.savedPicture).isNotNull();
        assertThat(pictureService.updatedPicture).isNull();
        assertThat(pictureService.savedPicture.getUserId()).isEqualTo(loginUser.getId());
        assertThat(pictureService.savedPicture.getCreateTime()).isNotNull();
        assertThat(pictureService.savedPicture.getEditTime()).isNull();
        assertThat(result.getUrl()).isEqualTo("https://example.com/test.png");
        verify(fileManger).uploadFile(file, "public/1");
    }

    @Test
    void uploadPictureUpdatesExistingPictureWhenRequestContainsId() {
        PictureUploadRequest uploadRequest = new PictureUploadRequest();
        uploadRequest.setId(100L);
        pictureService.existingPicture = Picture.builder()
                .id(100L)
                .userId(loginUser.getId())
                .build();

        pictureService.uploadPicture(file, uploadRequest, loginUser);

        assertThat(pictureService.savedPicture).isNull();
        assertThat(pictureService.updatedPicture).isNotNull();
        assertThat(pictureService.updatedPicture.getId()).isEqualTo(100L);
        assertThat(pictureService.updatedPicture.getUserId()).isEqualTo(loginUser.getId());
        assertThat(pictureService.updatedPicture.getEditTime()).isNotNull();
    }

    private static class RecordingPictureService extends PictureServiceImpl {

        private Picture existingPicture;
        private Picture savedPicture;
        private Picture updatedPicture;

        @Override
        public Picture getById(Serializable id) {
            return existingPicture;
        }

        @Override
        public boolean save(Picture entity) {
            savedPicture = entity;
            return true;
        }

        @Override
        public boolean updateById(Picture entity) {
            updatedPicture = entity;
            return true;
        }

        private void clear() {
            existingPicture = null;
            savedPicture = null;
            updatedPicture = null;
        }
    }
}
