package pvt.mktech.petcare.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


/**
 * 头像上传请求参数
 *
 * @author PetCare Code Generator
 * @since 2025-12-17
 */
@Data
public class AvatarUploadDTO {
    private MultipartFile file;

    private String cropInfo; // 可选：前端裁剪信息 JSON
}