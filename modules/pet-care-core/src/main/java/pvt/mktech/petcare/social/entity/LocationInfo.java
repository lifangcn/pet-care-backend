package pvt.mktech.petcare.social.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地点信息
 */
@Data
public class LocationInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String address;

    private String city;

    private String district;
}
