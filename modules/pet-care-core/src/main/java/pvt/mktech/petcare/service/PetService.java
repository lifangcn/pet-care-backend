package pvt.mktech.petcare.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.entity.Pet;

import java.util.List;

/**
 * 宠物表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
public interface PetService extends IService<Pet> {

    Pet save(String token, Pet pet);


    List<Pet> findByUserId(String token);
}
