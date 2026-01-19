package pvt.mktech.petcare.club.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 媒体URL
 */
@Data
public class MediaUrl implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;

    private Integer type;

    private String thumbnail;
}
