package pvt.mktech.petcare.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.entity.Pets;

import java.util.List;

/**
 * 宠物表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
public interface PetsService extends IService<Pets> {

    Pets save(String token, Pets pets);


    List<Pets> findByUserId(String token);
}
