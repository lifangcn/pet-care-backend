package pvt.mktech.petcare.core.service;

import com.mybatisflex.core.service.IService;
import pvt.mktech.petcare.core.entity.Pet;

import java.util.List;

/**
 * 宠物表 服务层。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
public interface PetService extends IService<Pet> {

    /**
     * 保存宠物
     *
     * @param pet 宠物
     * @return 保存后的宠物
     */
    Pet savePet(Pet pet);

    /**
     * 根据用户id查询用户所有宠物
     *
     * @param userId 用户id
     * @return 宠物列表
     */
    List<Pet> findByUserId(Long userId);

    /**
     * 根据用户id查询用户所有宠物
     *
     * @param userId  用户id
     * @param petName 宠物名称
     * @return 宠物列表
     */
    Pet findByUserIdAndPetName(Long userId, String petName);
}
