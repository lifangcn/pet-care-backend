package pvt.mktech.petcare.core.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.common.context.UserContext;
import pvt.mktech.petcare.core.entity.Pet;
import pvt.mktech.petcare.core.mapper.PetsMapper;
import pvt.mktech.petcare.core.service.PetService;

import java.util.List;

import static pvt.mktech.petcare.core.entity.table.PetTableDef.PETS;

/**
 * 宠物表 服务层实现。
 *
 * @author PetCare Code Generator
 * @since 2025-12-02
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PetServiceImpl extends ServiceImpl<PetsMapper, Pet> implements PetService {

    @Override
    public Pet savePet(Pet pet) {
        Long userId = UserContext.getUserInfo().getUserId();
        pet.setUserId(userId);
        saveOrUpdate(pet);
        return pet;
    }

    @Override
    public List<Pet> findByUserId(Long userId) {
        return list(PETS.USER_ID.eq(userId));
    }
}
