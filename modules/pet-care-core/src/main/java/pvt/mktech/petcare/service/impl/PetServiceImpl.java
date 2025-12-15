package pvt.mktech.petcare.service.impl;

import cn.hutool.core.map.MapUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.entity.Pet;
import pvt.mktech.petcare.mapper.PetsMapper;
import pvt.mktech.petcare.service.PetService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static pvt.mktech.petcare.entity.table.PetTableDef.PETS;

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
    public static final String LOGIN_TOKEN_KEY = "login:token:";
    private final StringRedisTemplate stringRedisTemplate;
    private final PetsMapper petsMapper;
    @Override
    public Pet save(String token, Pet pet) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(LOGIN_TOKEN_KEY + token);
        if (MapUtil.isEmpty(entries)) {

            return null;
        }
        Long userId = MapUtil.getLong(entries, "id");
        pet.setUserId(userId);
        saveOrUpdate(pet);
        return pet;
    }

    @Override
    public List<Pet> findByUserId(String token) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(LOGIN_TOKEN_KEY + token);
        if (MapUtil.isEmpty(entries)) {
            return Collections.emptyList();
        }
        Long userId = MapUtil.getLong(entries, "id");
        return list(PETS.USER_ID.eq(userId));
    }
}
